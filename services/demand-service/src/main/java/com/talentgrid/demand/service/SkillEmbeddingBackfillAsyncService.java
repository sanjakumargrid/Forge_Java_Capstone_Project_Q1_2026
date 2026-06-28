package com.talentgrid.demand.service;

import com.talentgrid.demand.domain.entity.Skill;
import com.talentgrid.demand.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkillEmbeddingBackfillAsyncService {

    private final SkillEmbeddingService skillEmbeddingService;
    private final SkillRepository skillRepository;

    @Async
    public void backfillMissingInBackground() {
        List<Skill> allSkills = skillRepository.findAll();
        List<Skill> missing = allSkills.stream()
                .filter(s -> s.getEmbedding() == null || s.getEmbedding().length == 0)
                .collect(Collectors.toList());
        if (missing.isEmpty()) {
            log.info("No skills missing embeddings — backfill skipped.");
            return;
        }
        log.info("On-demand background backfill started: {} skills missing embeddings", missing.size());
        for (Skill skill : missing) {
            skillEmbeddingService.embedIfMissing(skill);
        }
        log.info("On-demand background backfill complete.");
    }
}
