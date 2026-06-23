package com.talentgrid.jobposting.service;

import com.talentgrid.jobposting.dto.embedded.ChannelDto;
import com.talentgrid.jobposting.dto.embedded.ChannelMetricsDto;
import com.talentgrid.jobposting.dto.request.ChannelEventRequest;
import com.talentgrid.jobposting.dto.response.ChannelPresenceResponse;
import com.talentgrid.jobposting.dto.response.MarketPresenceResponse;
import com.talentgrid.jobposting.entity.JobPosting;
import com.talentgrid.jobposting.exception.ResourceNotFoundException;
import com.talentgrid.jobposting.repository.JobPostingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Market-presence analytics (REQ-AN-03): records per-channel funnel events on job postings
 * and serves the aggregated dashboard consumed by BL Team 3.
 *
 * This service is intentionally self-contained, depending only on the job posting repository.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final JobPostingRepository jobPostingRepository;

    /** Human-readable labels for known channel keys; falls back to the key itself. */
    private static final Map<String, String> CHANNEL_LABELS = Map.of(
            "linkedin", "LinkedIn",
            "indeed", "Indeed",
            "portal", "Careers Portal"
    );

    /**
     * Records one funnel event (view/click/apply-start/apply-completion) for a job posting
     * on a specific channel. Increments the matching counter, creating the channel bucket
     * if it does not exist yet.
     */
    @Transactional
    public void recordEvent(ChannelEventRequest req) {
        JobPosting posting = jobPostingRepository.findById(req.getJobPostingId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Job posting not found: " + req.getJobPostingId()));

        List<ChannelMetricsDto> metrics = posting.getChannelMetrics();
        if (metrics == null) {
            metrics = new ArrayList<>();
            posting.setChannelMetrics(metrics);
        }

        ChannelMetricsDto bucket = metrics.stream()
                .filter(m -> req.getChannel().equalsIgnoreCase(m.getChannel()))
                .findFirst()
                .orElse(null);

        if (bucket == null) {
            bucket = ChannelMetricsDto.builder().channel(req.getChannel()).build();
            metrics.add(bucket);
        }

        switch (req.getEventType()) {
            case VIEW -> bucket.setViews(bucket.getViews() + 1);
            case CLICK -> bucket.setClicks(bucket.getClicks() + 1);
            case APPLY_START -> bucket.setApplyStarts(bucket.getApplyStarts() + 1);
            case APPLY_COMPLETION -> bucket.setApplyCompletions(bucket.getApplyCompletions() + 1);
        }

        jobPostingRepository.save(posting);
        log.info("Recorded {} on channel={} for jobPostingId={}",
                req.getEventType(), req.getChannel(), req.getJobPostingId());
    }

    /** Market-presence dashboard aggregated across every job posting. */
    @Transactional(readOnly = true)
    public MarketPresenceResponse getMarketPresence() {
        return aggregate(jobPostingRepository.findAll());
    }

    /** Market-presence dashboard for a single job posting. */
    @Transactional(readOnly = true)
    public MarketPresenceResponse getMarketPresenceForPosting(Long jobPostingId) {
        JobPosting posting = jobPostingRepository.findById(jobPostingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Job posting not found: " + jobPostingId));
        return aggregate(List.of(posting));
    }

    /** Sums per-channel funnel counters across the supplied postings and derives totals + rates. */
    private MarketPresenceResponse aggregate(List<JobPosting> postings) {
        Map<String, long[]> byChannel = new LinkedHashMap<>(); // [views, clicks, applyStarts, applyCompletions]
        Map<String, String> labels = new LinkedHashMap<>();

        for (JobPosting posting : postings) {
            // Seed labels from the posting's declared channels, so channels with zero
            // activity still appear in the dashboard.
            if (posting.getChannels() != null) {
                for (ChannelDto c : posting.getChannels()) {
                    labels.putIfAbsent(c.getKey(), c.getLabel());
                    byChannel.putIfAbsent(c.getKey(), new long[4]);
                }
            }
            if (posting.getChannelMetrics() == null) continue;
            for (ChannelMetricsDto m : posting.getChannelMetrics()) {
                long[] acc = byChannel.computeIfAbsent(m.getChannel(), k -> new long[4]);
                acc[0] += m.getViews();
                acc[1] += m.getClicks();
                acc[2] += m.getApplyStarts();
                acc[3] += m.getApplyCompletions();
            }
        }

        List<ChannelPresenceResponse> channels = new ArrayList<>();
        long tViews = 0, tClicks = 0, tStarts = 0, tCompletions = 0;

        for (Map.Entry<String, long[]> e : byChannel.entrySet()) {
            long[] v = e.getValue();
            channels.add(buildChannel(
                    e.getKey(),
                    labels.getOrDefault(e.getKey(), CHANNEL_LABELS.getOrDefault(e.getKey(), e.getKey())),
                    v[0], v[1], v[2], v[3]));
            tViews += v[0];
            tClicks += v[1];
            tStarts += v[2];
            tCompletions += v[3];
        }

        ChannelPresenceResponse totals = buildChannel(
                "ALL", "All Channels", tViews, tClicks, tStarts, tCompletions);

        return MarketPresenceResponse.builder()
                .postingsCount(postings.size())
                .channels(channels)
                .totals(totals)
                .build();
    }

    private ChannelPresenceResponse buildChannel(String key, String label,
                                                 long views, long clicks,
                                                 long applyStarts, long applyCompletions) {
        return ChannelPresenceResponse.builder()
                .channel(key)
                .label(label)
                .views(views)
                .clicks(clicks)
                .applyStarts(applyStarts)
                .applyCompletions(applyCompletions)
                .clickThroughRate(rate(clicks, views))
                .applyCompletionRate(rate(applyCompletions, applyStarts))
                .build();
    }

    /** Safe ratio rounded to 4 decimal places; 0 when the denominator is 0. */
    private double rate(long numerator, long denominator) {
        if (denominator <= 0) return 0d;
        return Math.round((double) numerator / denominator * 10000d) / 10000d;
    }
}
