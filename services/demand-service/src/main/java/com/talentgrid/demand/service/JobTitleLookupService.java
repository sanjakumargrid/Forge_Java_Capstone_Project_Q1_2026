package com.talentgrid.demand.service;

import com.talentgrid.demand.domain.entity.JobTitle;
import com.talentgrid.demand.dto.response.JobTitleResponse;
import com.talentgrid.demand.repository.JobTitleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobTitleLookupService {

    private final JobTitleRepository jobTitleRepository;

    public List<JobTitleResponse> getAllJobTitles() {
        return jobTitleRepository.findAllByOrderByTitleNameAsc().stream()
                .map(jobTitle -> JobTitleResponse.builder()
                        .jobTitleId(jobTitle.getJobTitleId())
                        .titleName(jobTitle.getTitleName())
                        .build())
                .collect(Collectors.toList());
    }

    public JobTitle resolveJobTitleId(Long jobTitleId) {
        if (jobTitleId == null) {
            return null;
        }
        return jobTitleRepository.findById(jobTitleId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid job title ID: " + jobTitleId));
    }
}
