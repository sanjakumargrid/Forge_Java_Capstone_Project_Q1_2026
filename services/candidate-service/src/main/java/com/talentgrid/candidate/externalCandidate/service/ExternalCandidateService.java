package com.talentgrid.candidate.externalCandidate.service;


import com.talentgrid.candidate.exception.BusinessException;
import com.talentgrid.candidate.externalCandidate.dto.CandidateResponse;
import com.talentgrid.candidate.externalCandidate.dto.ExternalCandidateDto;
import com.talentgrid.candidate.externalCandidate.entity.ExternalCandidate;
import com.talentgrid.candidate.externalCandidate.mapper.ExternalCandidateMapper;
import com.talentgrid.candidate.externalCandidate.repository.ExternalCandidateRepository;
import com.talentgrid.candidate.externalCandidate.utility.HashUtil;
import com.talentgrid.candidate.kafka.CandidateKafkaProducer;
import com.talentgrid.kafka.events.candidate.CandidatePayload;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class ExternalCandidateService {

    private final ExternalCandidateRepository externalCandidateRepository;
    private final HashUtil hashUtil;
    private final CandidateKafkaProducer candidateKafkaProducer;

    public ExternalCandidateService(
            ExternalCandidateRepository externalCandidateRepository,
            HashUtil hashUtil,
            CandidateKafkaProducer candidateKafkaProducer
    ) {
        this.externalCandidateRepository = externalCandidateRepository;
        this.hashUtil = hashUtil;
        this.candidateKafkaProducer = candidateKafkaProducer;
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

//        candidate.setPiiAnonymized(false);
//        candidate.setPiiAnonymizedAt(null);


        ExternalCandidate savedCandidate =
                externalCandidateRepository.save(candidate);

        CandidatePayload payload =
                buildCandidatePayload(savedCandidate);

        candidateKafkaProducer.publishCandidateCreated(
                payload,
                null
        );

        return CandidateResponse.builder()
                .status(HttpStatus.CREATED.value())
                .message("Candidate created successfully")
                .duplicate(false)
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

        ExternalCandidateMapper.copyDtoToExistingEntity(dto,existingCandidate);

        existingCandidate.setEmail(email);
        existingCandidate.setPhoneNumber(phone);
        existingCandidate.setEmailHash(emailHash);
        existingCandidate.setPhoneHash(phoneHash);

        ExternalCandidate savedCandidate =
                externalCandidateRepository.save(existingCandidate);

        CandidatePayload payload =
                buildCandidatePayload(savedCandidate);

        candidateKafkaProducer.publishCandidateUpdated(
                payload,
                null
        );

        return CandidateResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Candidate updated successfully")
                .duplicate(false)
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

        ExternalCandidate deletedCandidate =
                externalCandidateRepository.save(candidate);

        CandidatePayload payload =
                buildCandidatePayload(deletedCandidate);

        candidateKafkaProducer.publishCandidateDeleted(
                payload,
                null
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String normalizePhone(String phone) {
        return phone.trim();
    }

    private CandidatePayload buildCandidatePayload(
            ExternalCandidate candidate
    ) {
        return CandidatePayload.builder()
                .candidateId(candidate.getCandidateId())
                .firstName(candidate.getFirstName())
                .lastName(candidate.getLastName())
                .email(candidate.getEmail())
                .build();
    }

}