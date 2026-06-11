package com.talentgrid.candidate.externalCandidate.service;

import com.talentgrid.candidate.exception.BusinessException;
import com.talentgrid.candidate.externalCandidate.client.ApplicationClient;
import com.talentgrid.candidate.externalCandidate.dto.ApplicationRequestDto;
import com.talentgrid.candidate.externalCandidate.dto.CandidateResponse;
import com.talentgrid.candidate.externalCandidate.dto.ExternalCandidateDto;
import com.talentgrid.candidate.externalCandidate.entity.ExternalCandidate;
import com.talentgrid.candidate.externalCandidate.mapper.ExternalCandidateMapper;
import com.talentgrid.candidate.externalCandidate.repository.ExternalCandidateRepository;
import com.talentgrid.candidate.externalCandidate.utility.HashUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class ExternalCandidateService {

    private final ExternalCandidateRepository externalCandidateRepository;
    private final HashUtil hashUtil;
    private final ApplicationClient applicationClient;

    public ExternalCandidateService(
            ExternalCandidateRepository externalCandidateRepository,
            HashUtil hashUtil,
            ApplicationClient applicationClient
    ) {
        this.externalCandidateRepository = externalCandidateRepository;
        this.hashUtil = hashUtil;
        this.applicationClient = applicationClient;
    }

    @Transactional
    public CandidateResponse createCandidate(ExternalCandidateDto dto) {

        String email = normalizeEmail(dto.getEmail());
        String phone = normalizePhone(dto.getPhoneNumber());

        String emailHash = hashUtil.sha256(email);
        String phoneHash = hashUtil.sha256(phone);

        Optional<ExternalCandidate> existingCandidate =
                externalCandidateRepository.findActiveDuplicate(emailHash, phoneHash);

        if (existingCandidate.isPresent()) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "Candidate already exists with same email or phone number"
            );
        }

        ExternalCandidate candidate = ExternalCandidateMapper.dtoToEntity(dto);

        candidate.setCandidateId(null);
        candidate.setEmail(email);
        candidate.setPhoneNumber(phone);
        candidate.setEmailHash(emailHash);
        candidate.setPhoneHash(phoneHash);

        candidate.setIsDeleted(false);
        candidate.setDeletedAt(null);
        candidate.setDeletedBy(null);
        candidate.setDeleteReason(null);

        ExternalCandidate savedCandidate =
                externalCandidateRepository.save(candidate);

        if (dto.getDemandId() != null) {
            ApplicationRequestDto applicationRequest =
                    buildApplicationRequest(dto, savedCandidate.getCandidateId());

            submitApplicationAfterCandidateCommit(applicationRequest);
        }

        return CandidateResponse.builder()
                .status(HttpStatus.CREATED.value())
                .message(
                        dto.getDemandId() == null
                                ? "Candidate created successfully"
                                : "Candidate created successfully and application submission triggered automatically"
                )
                .duplicate(false)
                .candidateId(savedCandidate.getCandidateId())
                .build();
    }

    @Transactional
    public CandidateResponse updateCandidate(Long candidateId, ExternalCandidateDto dto) {

        ExternalCandidate existingCandidate =
                externalCandidateRepository.findWithDetailsByCandidateIdAndIsDeletedFalse(candidateId)
                        .orElseThrow(() -> new BusinessException(
                                HttpStatus.NOT_FOUND,
                                "Candidate not found"
                        ));

        String email = normalizeEmail(dto.getEmail());
        String phone = normalizePhone(dto.getPhoneNumber());

        String emailHash = hashUtil.sha256(email);
        String phoneHash = hashUtil.sha256(phone);

        Optional<ExternalCandidate> duplicateCandidate =
                externalCandidateRepository.findDuplicateForUpdate(
                        candidateId,
                        emailHash,
                        phoneHash
                );

        if (duplicateCandidate.isPresent()) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "Candidate already exists with same email or phone number"
            );
        }

        ExternalCandidateMapper.copyDtoToExistingEntity(dto, existingCandidate);

        existingCandidate.setEmail(email);
        existingCandidate.setPhoneNumber(phone);
        existingCandidate.setEmailHash(emailHash);
        existingCandidate.setPhoneHash(phoneHash);

        ExternalCandidate savedCandidate =
                externalCandidateRepository.save(existingCandidate);

        return CandidateResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Candidate updated successfully")
                .duplicate(false)
                .candidateId(savedCandidate.getCandidateId())
                .build();
    }

    @Transactional(readOnly = true)
    public ExternalCandidateDto getByCandidateId(Long candidateId) {

        ExternalCandidate candidate =
                externalCandidateRepository.findWithDetailsByCandidateIdAndIsDeletedFalse(candidateId)
                        .orElseThrow(() -> new BusinessException(
                                HttpStatus.NOT_FOUND,
                                "Candidate not found"
                        ));

        return ExternalCandidateMapper.entityToDto(candidate);
    }

    @Transactional
    public void deleteById(Long candidateId) {

        ExternalCandidate candidate =
                externalCandidateRepository.findByCandidateIdAndIsDeletedFalse(candidateId)
                        .orElseThrow(() -> new BusinessException(
                                HttpStatus.NOT_FOUND,
                                "Candidate not found"
                        ));

        candidate.setIsDeleted(true);
        candidate.setDeletedAt(LocalDateTime.now());

        externalCandidateRepository.save(candidate);
    }

    private ApplicationRequestDto buildApplicationRequest(
            ExternalCandidateDto dto,
            Long candidateId
    ) {

        ApplicationRequestDto applicationRequest = new ApplicationRequestDto();

        applicationRequest.setCandidateId(candidateId);
        applicationRequest.setDemandId(dto.getDemandId());

        applicationRequest.setResumeFilePath(getResumeFilePath(dto));
        applicationRequest.setResumeOriginalFilename(getResumeOriginalFilename(dto));

        applicationRequest.setAiRationale(
                "Candidate profile automatically submitted for demand screening."
        );

        applicationRequest.setFreeNotes(dto.getFreeNotes());
        applicationRequest.setCurrentStage("APPLIED");
        applicationRequest.setAiScore(0);
        applicationRequest.setReferralCode(null);
        applicationRequest.setBlockedFromReapply(false);

        return applicationRequest;
    }

    private void submitApplicationAfterCandidateCommit(
            ApplicationRequestDto applicationRequest
    ) {

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {
                        applicationClient.createApplication(applicationRequest);
                    }
                }
        );
    }

    private String getResumeFilePath(ExternalCandidateDto dto) {

        if (dto.getResumeDetails() == null || dto.getResumeDetails().isEmpty()) {
            return "No resume uploaded";
        }

        return dto.getResumeDetails().get(0).getResumeFilePath();
    }

    private String getResumeOriginalFilename(ExternalCandidateDto dto) {

        if (dto.getResumeDetails() == null || dto.getResumeDetails().isEmpty()) {
            return "No resume uploaded";
        }

        return dto.getResumeDetails().get(0).getResumeOriginalFilename();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String normalizePhone(String phone) {
        return phone.trim();
    }
}