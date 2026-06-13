package com.talentgrid.candidate.externalCandidate.service;

import com.talentgrid.candidate.exception.BusinessException;
import com.talentgrid.candidate.externalCandidate.client.ApplicationClient;
import com.talentgrid.candidate.externalCandidate.dto.ApplicationRequestDto;
import com.talentgrid.candidate.externalCandidate.dto.ApplicationResponseDto;
import com.talentgrid.candidate.externalCandidate.dto.ExternalCandidateDto;
import com.talentgrid.candidate.externalCandidate.dto.response.CandidateResponse;
import com.talentgrid.candidate.externalCandidate.entity.ExternalCandidate;
import com.talentgrid.candidate.externalCandidate.mapper.ExternalCandidateMapper;
import com.talentgrid.candidate.externalCandidate.repository.ExternalCandidateRepository;
import com.talentgrid.candidate.externalCandidate.utility.HashUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        validateAutomaticApplicationRequiredFields(dto);

        String email = normalizeEmail(dto.getEmail());
        String phone = normalizePhone(dto.getPhoneNumber());

        String emailHash = hashUtil.sha256(email);
        String phoneHash = hashUtil.sha256(phone);

        Optional<ExternalCandidate> existingCandidate =
                externalCandidateRepository.findActiveDuplicate(
                        emailHash,
                        phoneHash
                );

        boolean duplicate = existingCandidate.isPresent();

        ExternalCandidate candidate;

        if (duplicate) {
            candidate = existingCandidate.get();
        } else {
            candidate = ExternalCandidateMapper.dtoToEntity(dto);

            candidate.setCandidateId(null);
            candidate.setEmail(email);
            candidate.setPhoneNumber(phone);
            candidate.setEmailHash(emailHash);
            candidate.setPhoneHash(phoneHash);

            candidate.setIsDeleted(false);
            candidate.setDeletedAt(null);
            candidate.setDeletedBy(null);
            candidate.setDeleteReason(null);

            candidate = externalCandidateRepository.save(candidate);

            /*
             * Important:
             * flush makes sure candidateId is generated before calling application-service.
             * If application-service fails after this, @Transactional will rollback this save.
             */
            externalCandidateRepository.flush();
        }

        ApplicationRequestDto applicationRequest =
                buildApplicationRequest(
                        dto,
                        candidate.getCandidateId()
                );

        ApplicationResponseDto applicationResponse =
                applicationClient.createApplication(applicationRequest);

        if (applicationResponse == null) {
            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "Automatic application submission failed because application-service returned empty response"
            );
        }

        if (duplicate) {
            return CandidateResponse.builder()
                    .status(HttpStatus.OK.value())
                    .message("Candidate already exists and application submitted for new demand")
                    .duplicate(true)
                    .candidateId(candidate.getCandidateId())
                    .applicationSubmitted(true)
                    .applicationMessage("Application created successfully")
                    .build();
        }

        return CandidateResponse.builder()
                .status(HttpStatus.CREATED.value())
                .message("Candidate created successfully and application submitted automatically")
                .duplicate(false)
                .candidateId(candidate.getCandidateId())
                .applicationSubmitted(true)
                .applicationMessage("Application created successfully")
                .build();
    }

    @Transactional
    public CandidateResponse updateCandidate(
            Long candidateId,
            ExternalCandidateDto dto
    ) {

        ExternalCandidate existingCandidate =
                externalCandidateRepository
                        .findWithDetailsByCandidateIdAndIsDeletedFalse(candidateId)
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

        ExternalCandidateMapper.copyDtoToExistingEntity(
                dto,
                existingCandidate
        );

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
                externalCandidateRepository
                        .findWithDetailsByCandidateIdAndIsDeletedFalse(candidateId)
                        .orElseThrow(() -> new BusinessException(
                                HttpStatus.NOT_FOUND,
                                "Candidate not found"
                        ));

        return ExternalCandidateMapper.entityToDto(candidate);
    }

    @Transactional
    public void deleteById(Long candidateId) {

        ExternalCandidate candidate =
                externalCandidateRepository
                        .findByCandidateIdAndIsDeletedFalse(candidateId)
                        .orElseThrow(() -> new BusinessException(
                                HttpStatus.NOT_FOUND,
                                "Candidate not found"
                        ));

        candidate.setIsDeleted(true);
        candidate.setDeletedAt(LocalDateTime.now());

        externalCandidateRepository.save(candidate);
    }

    private void validateAutomaticApplicationRequiredFields(
            ExternalCandidateDto dto
    ) {

        if (dto == null) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Candidate details are required"
            );
        }

        if (dto.getDemandId() == null) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Demand ID is required for automatic application submission"
            );
        }

        if (dto.getSource() == null) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Source is required for automatic application submission"
            );
        }

        validateResumeForAutomaticSubmission(dto);
    }

    private ApplicationRequestDto buildApplicationRequest(
            ExternalCandidateDto dto,
            Long candidateId
    ) {

        ApplicationRequestDto applicationRequest =
                new ApplicationRequestDto();

        applicationRequest.setCandidateId(candidateId);
        applicationRequest.setDemandId(dto.getDemandId());
        applicationRequest.setSource(dto.getSource().name());

        applicationRequest.setResumeFilePath(getResumeFilePath(dto));
        applicationRequest.setResumeOriginalFilename(
                getResumeOriginalFilename(dto)
        );

        applicationRequest.setFreeNotes(dto.getFreeNotes());
        applicationRequest.setReferralCode(null);

        return applicationRequest;
    }

    private void validateResumeForAutomaticSubmission(
            ExternalCandidateDto dto
    ) {

        if (dto.getResumeDetails() == null ||
                dto.getResumeDetails().isEmpty()) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Resume is required for automatic application submission"
            );
        }

        if (dto.getResumeDetails().get(0) == null) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Resume details are required for automatic application submission"
            );
        }

        if (dto.getResumeDetails().get(0).getResumeFilePath() == null ||
                dto.getResumeDetails().get(0).getResumeFilePath().isBlank()) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Resume file path is required for automatic application submission"
            );
        }

        if (dto.getResumeDetails().get(0).getResumeOriginalFilename() == null ||
                dto.getResumeDetails().get(0).getResumeOriginalFilename().isBlank()) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Resume original filename is required for automatic application submission"
            );
        }
    }

    private String getResumeFilePath(ExternalCandidateDto dto) {

        return dto.getResumeDetails()
                .get(0)
                .getResumeFilePath()
                .trim();
    }

    private String getResumeOriginalFilename(ExternalCandidateDto dto) {

        return dto.getResumeDetails()
                .get(0)
                .getResumeOriginalFilename()
                .trim();
    }

    private String normalizeEmail(String email) {

        if (email == null || email.isBlank()) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Email is required"
            );
        }

        return email.trim().toLowerCase();
    }

    private String normalizePhone(String phone) {

        if (phone == null || phone.isBlank()) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Phone number is required"
            );
        }

        return phone.trim();
    }
}