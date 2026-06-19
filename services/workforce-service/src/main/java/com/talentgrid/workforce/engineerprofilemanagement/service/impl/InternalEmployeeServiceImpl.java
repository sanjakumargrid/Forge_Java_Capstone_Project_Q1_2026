package com.talentgrid.workforce.engineerprofilemanagement.service.impl;

import com.talentgrid.workforce.engineerprofilemanagement.dto.EmployeeProfileUpdatedPayload;
import com.talentgrid.workforce.engineerprofilemanagement.dto.InternalEmployeeResponse;
import com.talentgrid.workforce.engineerprofilemanagement.dto.UpdateEngineerProfileRequest;
import com.talentgrid.workforce.engineerprofilemanagement.entity.InternalEmployee;
import com.talentgrid.workforce.engineerprofilemanagement.enums.HrisSyncStatus;
import com.talentgrid.workforce.engineerprofilemanagement.exception.ResourceNotFoundException;
import com.talentgrid.workforce.engineerprofilemanagement.dto.UserDto;
import com.talentgrid.workforce.kafka.producer.WorkforceKafkaProducer;
import com.talentgrid.workforce.engineerprofilemanagement.repository.InternalEmployeeRepository;
import com.talentgrid.workforce.engineerprofilemanagement.service.InternalEmployeeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class InternalEmployeeServiceImpl implements InternalEmployeeService {

    private final InternalEmployeeRepository repository;
    private final WorkforceKafkaProducer workforceKafkaProducer;

    public InternalEmployeeServiceImpl(InternalEmployeeRepository repository,
                                       WorkforceKafkaProducer workforceKafkaProducer) {
        this.repository = repository;
        this.workforceKafkaProducer = workforceKafkaProducer;
    }

    @Override
    public InternalEmployeeResponse getEmployeeDetailsById(Long employeeId) {
        InternalEmployee employee = repository.findByEmployeeIdAndIsDeletedFalse(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Internal employee not found for id: " + employeeId));

        return mapToResponse(employee);
    }

    @Override
    public InternalEmployeeResponse getEmployeeByDatabaseId(Long id) {
        InternalEmployee employee = repository.findById(id)
                .filter(e -> !e.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Internal employee not found for database id: " + id));

        return mapToResponse(employee);
    }

    @Override
    public List<InternalEmployeeResponse> getAllEngineers() {
        return repository.findByIsDeletedFalseOrderByIdAsc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public InternalEmployeeResponse updateOwnProfile(Long employeeId,
                                                     UpdateEngineerProfileRequest request,
                                                     String requestId) {

        InternalEmployee employee = repository.findByEmployeeIdAndIsDeletedFalse(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Internal employee not found for id: " + employeeId));

        //Reject empty PATCH
        if (request.getSkills() == null
                && request.getResumeDriveLink() == null
                && request.getAvailabilityDate() == null) {
            throw new IllegalArgumentException("At least one field must be updated");
        }

        boolean skillsChanged = false;
        boolean resumeDriveLinkChanged = false;
        boolean availabilityChanged = false;

        // Handle skills update safely
        if (request.getSkills() != null) {
            String[] newSkills = request.getSkills().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(skill -> !skill.isBlank())
                    .distinct()
                    .toArray(String[]::new);
            skillsChanged = !Arrays.equals(employee.getSkills(), newSkills);

            if (skillsChanged) {
                employee.setSkills(newSkills);
                employee.setSkillsVector(null);
                employee.setLastEmbeddedAt(null);
            }
        }

        if (request.getResumeDriveLink() != null) {
            String resumeDriveLink = request.getResumeDriveLink().trim();
            resumeDriveLinkChanged = !Objects.equals(employee.getResumeDriveLink(), resumeDriveLink);

            if (resumeDriveLinkChanged) {
                employee.setResumeDriveLink(resumeDriveLink);
            }
        }

        // Handle availability update safely
        if (request.getAvailabilityDate() != null) {
            availabilityChanged = !Objects.equals(
                    employee.getAvailabilityDate(),
                    request.getAvailabilityDate()
            );

            if (availabilityChanged) {
                employee.setAvailabilityDate(request.getAvailabilityDate());
            }
        }

        // No actual changes
        if (!skillsChanged && !resumeDriveLinkChanged && !availabilityChanged) {
            return mapToResponse(employee);
        }

        employee.setUpdatedAt(LocalDateTime.now());

        InternalEmployee saved = repository.save(employee);

        List<String> updatedFields = new ArrayList<>();

        if (skillsChanged) {
            updatedFields.add("skills");
        }
        if (resumeDriveLinkChanged) {
            updatedFields.add("resumeDriveLink");
        }
        if (availabilityChanged) {
            updatedFields.add("availabilityDate");
        }

        workforceKafkaProducer.publishEmployeeProfileUpdated(
                EmployeeProfileUpdatedPayload.builder()
                        .employeeId(saved.getEmployeeId())
                        .updatedFields(updatedFields)
                        .skills(skillsChanged ? List.of(saved.getSkills()) : null)
                        .resumeDriveLink(resumeDriveLinkChanged ? saved.getResumeDriveLink() : null)
                        .availabilityDate(availabilityChanged ? saved.getAvailabilityDate() : null)
                        .build(),
                requestId
        );

        return mapToResponse(saved);
    }

    private InternalEmployeeResponse mapToResponse(InternalEmployee employee) {
        InternalEmployeeResponse response = new InternalEmployeeResponse();
        response.setEmployeeId(employee.getEmployeeId());
        response.setName(employee.getName());
        response.setEmail(employee.getEmail());
        response.setLevel(employee.getLevel() != null ? employee.getLevel().name() : null);
        response.setSkills(employee.getSkills());
        response.setSkillsVector(employee.getSkillsVector());
        response.setCurrentProject(employee.getCurrentProject());
        response.setResumeDriveLink(employee.getResumeDriveLink());
        response.setAvailabilityDate(employee.getAvailabilityDate());
        response.setLocation(employee.getLocation());
        response.setContractType(employee.getContractType() != null ? employee.getContractType().name() : null);
        response.setUtilisationPct(employee.getUtilisationPct());
        response.setManagerId(employee.getManagerId());
        response.setLastEmbeddedAt(employee.getLastEmbeddedAt());
        response.setHrisSyncStatus(employee.getHrisSyncStatus() != null ? employee.getHrisSyncStatus().name() : null);
        response.setCreatedAt(employee.getCreatedAt());
        response.setUpdatedAt(employee.getUpdatedAt());
        response.setIsDeleted(Boolean.TRUE.equals(employee.getIsDeleted()));
        response.setDeletedAt(employee.getDeletedAt());
        return response;
    }


    @Override
    @Transactional
    public InternalEmployeeResponse syncEmployeeFromKafka(UserDto userDto) {
        if (userDto == null || userDto.getEmployeeId() == null) {
            throw new IllegalArgumentException("Kafka user payload must contain employeeId");
        }

        Long employeeId = userDto.getEmployeeId();
        LocalDateTime now = LocalDateTime.now();

        InternalEmployee employee = repository.findByEmployeeIdAndIsDeletedFalse(employeeId)
                .orElseGet(InternalEmployee::new);

        employee.setEmployeeId(employeeId);
        employee.setName(userDto.getName());
        employee.setEmail(userDto.getEmail());
        employee.setLocation(userDto.getLocation());
        employee.setAvailabilityDate(userDto.getAvailableFrom() != null ? userDto.getAvailableFrom().toLocalDate() : null);
        employee.setHrisSyncStatus(HrisSyncStatus.SYNCED);
        employee.setIsDeleted(Boolean.FALSE);
        employee.setDeletedAt(null);

        if (employee.getCreatedAt() == null) {
            employee.setCreatedAt(now);
        }
        employee.setUpdatedAt(now);

        InternalEmployee savedEmployee = repository.save(employee);
        return mapToResponse(savedEmployee);
    }
}
