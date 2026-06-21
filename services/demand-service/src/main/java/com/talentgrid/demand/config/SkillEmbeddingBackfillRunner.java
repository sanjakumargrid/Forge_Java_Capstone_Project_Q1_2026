package com.talentgrid.demand.config;

import com.talentgrid.demand.service.SkillEmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("backfill-embeddings")
@RequiredArgsConstructor
public class SkillEmbeddingBackfillRunner implements ApplicationRunner {

    private final SkillEmbeddingService skillEmbeddingService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Starting one-time backfill of missing skill embeddings...");
        try {
            skillEmbeddingService.backfillAllMissingEmbeddings();
        } catch (Exception e) {
            log.error("Error during skill embedding backfill", e);
        }
    }
}
