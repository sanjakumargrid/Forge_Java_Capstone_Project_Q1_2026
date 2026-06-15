package com.talentgrid.candidate.externalCandidate.service;

import com.talentgrid.candidate.externalCandidate.entity.ExternalCandidate;
import com.talentgrid.candidate.externalCandidate.repository.ExternalCandidateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExternalCandidatePersistenceService {

    private final ExternalCandidateRepository externalCandidateRepository;

    public ExternalCandidatePersistenceService(
            ExternalCandidateRepository externalCandidateRepository
    ) {
        this.externalCandidateRepository = externalCandidateRepository;
    }

    @Transactional
    public ExternalCandidate saveCandidate(ExternalCandidate candidate) {
        return externalCandidateRepository.saveAndFlush(candidate);
    }
}