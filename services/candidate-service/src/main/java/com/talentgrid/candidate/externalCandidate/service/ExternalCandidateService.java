package com.talentgrid.candidate.externalCandidate.service;

import com.talentgrid.audit.client.AuditLogClient;
import com.talentgrid.audit.dto.AuditAction;
import com.talentgrid.audit.dto.AuditLogPayload;
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
import com.talentgrid.candidate.kafka.producer.CandidateEventProducer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
public class ExternalCandidateService {

    private final ExternalCandidateRepository externalCandidateRepository;
    private final HashUtil hashUtil;
    private final ApplicationClient applicationClient;
    private final AuditLogClient auditLogClient;
    private final CandidateEventProducer candidateEventProducer;
    private final ExternalCandidatePersistenceService candidatePersistenceService;

    public ExternalCandidateService(
            ExternalCandidateRepository externalCandidateRepository,
            HashUtil hashUtil,
            ApplicationClient applicationClient,
            AuditLogClient auditLogClient,
            CandidateEventProducer candidateEventProducer,
            ExternalCandidatePersistenceService candidatePersistenceService
    ) {
        this.externalCandidateRepository = externalCandidateRepository;
        this.hashUtil = hashUtil;
        this.applicationClient = applicationClient;
        this.auditLogClient = auditLogClient;
        this.candidateEventProducer = candidateEventProducer;
        this.candidatePersistenceService = candidatePersistenceService;
    }

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

        boolean duplicateCandidate = existingCandidate.isPresent();

        ExternalCandidate candidate;

        if (duplicateCandidate) {
            candidate = existingCandidate.get();

            ExternalCandidateMapper.copyDtoToExistingEntity(dto, candidate);

            candidate.setEmail(email);
            candidate.setPhoneNumber(phone);
            candidate.setEmailHash(emailHash);
            candidate.setPhoneHash(phoneHash);

            candidate = candidatePersistenceService.saveCandidate(candidate);

            auditLogClient.logAction(
                    AuditLogPayload.builder()
                            .entityType("CANDIDATE")
                            .entityId(candidate.getCandidateId())
                            .action(AuditAction.UPDATE)
                            .afterState(Map.of(
                                    "firstName", candidate.getFirstName(),
                                    "lastName", candidate.getLastName(),
                                    "email", candidate.getEmail(),
                                    "isDuplicateMerge", true
                            ))
                            .serviceName("candidate-service")
                            .endpoint("/api/v1/candidates")
                            .build()
            );

            candidateEventProducer.publishUpdated(candidate);

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

            candidate = candidatePersistenceService.saveCandidate(candidate);

            auditLogClient.logAction(
                    AuditLogPayload.builder()
                            .entityType("CANDIDATE")
                            .entityId(candidate.getCandidateId())
                            .action(AuditAction.CREATE)
                            .afterState(Map.of(
                                    "firstName", candidate.getFirstName(),
                                    "lastName", candidate.getLastName(),
                                    "email", candidate.getEmail()
                            ))
                            .serviceName("candidate-service")
                            .endpoint("/api/v1/candidates")
                            .build()
            );

            candidateEventProducer.publishCreated(candidate);
        }

        ApplicationRequestDto applicationRequest =
                buildApplicationRequest(dto, candidate.getCandidateId());

        ApplicationResponseDto applicationResponse =
                applicationClient.createApplication(applicationRequest);

        if (applicationResponse == null || applicationResponse.getApplicationId() == null) {
            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "Automatic application submission failed because application-service did not return application id"
            );
        }

        boolean applicationAlreadyExists =
                Boolean.TRUE.equals(applicationResponse.getApplicationAlreadyExists());

        String message = buildCandidateCreateMessage(
                duplicateCandidate,
                applicationAlreadyExists
        );

        int status = duplicateCandidate || applicationAlreadyExists
                ? HttpStatus.OK.value()
                : HttpStatus.CREATED.value();

        return CandidateResponse.builder()
                .status(status)
                .message(message)
                .duplicate(duplicateCandidate)
                .candidateId(candidate.getCandidateId())
                .applicationId(applicationResponse.getApplicationId())
                .jobPostingId(applicationResponse.getJobPostingId())
                .demandId(applicationResponse.getDemandId())
                .applicationAlreadyExists(applicationAlreadyExists)
                .applicationSubmitted(!applicationAlreadyExists)
                .applicationMessage(applicationResponse.getMessage())
                .build();
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

        if (dto.getJobPostingId() == null) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Job posting id is required for automatic application submission"
            );
        }

        if (dto.getDemandId() == null) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Demand id is required for automatic application submission"
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
        applicationRequest.setJobPostingId(dto.getJobPostingId());
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

    private String buildCandidateCreateMessage(
            boolean duplicateCandidate,
            boolean applicationAlreadyExists
    ) {
        if (duplicateCandidate && applicationAlreadyExists) {
            return "Candidate already exists and already applied for the same job posting and demand";
        }

        if (duplicateCandidate) {
            return "Candidate already exists, so profile was updated and application was created for the new job posting and demand";
        }

        if (applicationAlreadyExists) {
            return "Candidate created, but application already exists for this job posting and demand";
        }

        return "Candidate created successfully and application submitted automatically";
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

        auditLogClient.logAction(
                AuditLogPayload.builder()
                        .entityType("CANDIDATE")
                        .entityId(savedCandidate.getCandidateId())
                        .action(AuditAction.UPDATE)
                        .afterState(Map.of(
                                "firstName", savedCandidate.getFirstName(),
                                "lastName", savedCandidate.getLastName(),
                                "email", savedCandidate.getEmail()
                        ))
                        .serviceName("candidate-service")
                        .endpoint("/api/v1/external-candidates/" + candidateId)
                        .build()
        );

        candidateEventProducer.publishUpdated(savedCandidate);

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
                        .findByCandidateIdAndIsDeletedFalse(candidateId)
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

        ExternalCandidate deletedCandidate =
                externalCandidateRepository.save(candidate);

        auditLogClient.logAction(
                AuditLogPayload.builder()
                        .entityType("CANDIDATE")
                        .entityId(candidate.getCandidateId())
                        .action(AuditAction.DELETE)
                        .afterState(Map.of(
                                "isDeleted", true
                        ))
                        .serviceName("candidate-service")
                        .endpoint("/api/v1/external-candidates/" + candidateId)
                        .build()
        );

        candidateEventProducer.publishDeleted(deletedCandidate);
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
