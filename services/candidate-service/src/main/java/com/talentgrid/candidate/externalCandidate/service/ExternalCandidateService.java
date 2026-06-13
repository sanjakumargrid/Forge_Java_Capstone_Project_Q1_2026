package com.talentgrid.candidate.externalCandidate.service;

import com.talentgrid.candidate.exception.BusinessException;
import com.talentgrid.candidate.externalCandidate.client.ApplicationClient;
import com.talentgrid.candidate.externalCandidate.dto.ApplicationRequestDto;
import com.talentgrid.candidate.externalCandidate.dto.ApplicationResponseDto;
import com.talentgrid.candidate.externalCandidate.dto.response.CandidateResponse;
import com.talentgrid.candidate.externalCandidate.dto.ExternalCandidateDto;
import com.talentgrid.candidate.externalCandidate.entity.ExternalCandidate;
import com.talentgrid.candidate.externalCandidate.mapper.ExternalCandidateMapper;
import com.talentgrid.candidate.externalCandidate.repository.ExternalCandidateRepository;
import com.talentgrid.candidate.externalCandidate.utility.HashUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class ExternalCandidateService {

    private final ExternalCandidateRepository externalCandidateRepository;
    private final HashUtil hashUtil;
    private final ApplicationClient applicationClient;
    private final TransactionTemplate transactionTemplate;

    public ExternalCandidateService(
            ExternalCandidateRepository externalCandidateRepository,
            HashUtil hashUtil,
            ApplicationClient applicationClient,
            TransactionTemplate transactionTemplate
    ) {
        this.externalCandidateRepository = externalCandidateRepository;
        this.hashUtil = hashUtil;
        this.applicationClient = applicationClient;
        this.transactionTemplate = transactionTemplate;
    }

    public CandidateResponse createCandidate(ExternalCandidateDto dto) {

        if (dto.getDemandId() == null) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Demand ID is required for automatic application submission"
            );
        }

        CandidateRegistrationResult registrationResult =
                getOrCreateCandidateInTransaction(dto);

        ApplicationRequestDto applicationRequest =
                buildApplicationRequest(
                        dto,
                        registrationResult.getCandidate().getCandidateId()
                );

        try {
            ApplicationResponseDto applicationResponse =
                    applicationClient.createApplication(applicationRequest);

            if (applicationResponse == null) {
                throw new BusinessException(
                        HttpStatus.BAD_GATEWAY,
                        "Automatic application submission failed because application-service returned empty response"
                );
            }

            if (registrationResult.isDuplicate()) {
                return CandidateResponse.builder()
                        .status(HttpStatus.OK.value())
                        .message("Candidate already exists and application submitted for new demand")
                        .duplicate(true)
                        .candidateId(registrationResult.getCandidate().getCandidateId())
                        .applicationSubmitted(true)
                        .applicationMessage("Application created successfully")
                        .build();
            }

            return CandidateResponse.builder()
                    .status(HttpStatus.CREATED.value())
                    .message("Candidate created successfully and application submitted automatically")
                    .duplicate(false)
                    .candidateId(registrationResult.getCandidate().getCandidateId())
                    .applicationSubmitted(true)
                    .applicationMessage("Application created successfully")
                    .build();

        } catch (BusinessException ex) {

            if (ex.getStatus() == HttpStatus.CONFLICT) {
                return CandidateResponse.builder()
                        .status(HttpStatus.CONFLICT.value())
                        .message("Application already exists for this candidate and demand")
                        .duplicate(true)
                        .candidateId(registrationResult.getCandidate().getCandidateId())
                        .applicationSubmitted(false)
                        .applicationMessage(ex.getMessage())
                        .build();
            }

            throw ex;
        }
    }

    private CandidateRegistrationResult getOrCreateCandidateInTransaction(ExternalCandidateDto dto) {

        return transactionTemplate.execute(status -> {

            String email = normalizeEmail(dto.getEmail());
            String phone = normalizePhone(dto.getPhoneNumber());

            String emailHash = hashUtil.sha256(email);
            String phoneHash = hashUtil.sha256(phone);

            Optional<ExternalCandidate> existingCandidate =
                    externalCandidateRepository.findActiveDuplicate(emailHash, phoneHash);

            if (existingCandidate.isPresent()) {
                return new CandidateRegistrationResult(existingCandidate.get(), true);
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

            return new CandidateRegistrationResult(savedCandidate, false);
        });
    }

    private static class CandidateRegistrationResult {

        private final ExternalCandidate candidate;
        private final boolean duplicate;

        private CandidateRegistrationResult(
                ExternalCandidate candidate,
                boolean duplicate
        ) {
            this.candidate = candidate;
            this.duplicate = duplicate;
        }

        private ExternalCandidate getCandidate() {
            return candidate;
        }

        private boolean isDuplicate() {
            return duplicate;
        }
    }

    @Transactional
    public CandidateResponse updateCandidate(
            Long candidateId,
            ExternalCandidateDto dto
    ) {

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
                .applicationSubmitted(false)
                .applicationMessage(null)
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

        if (dto.getSource() != null) {
            applicationRequest.setSource(dto.getSource().name());
        }

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