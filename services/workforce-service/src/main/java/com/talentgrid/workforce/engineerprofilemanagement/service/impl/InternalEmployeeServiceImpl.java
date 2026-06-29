package com.talentgrid.workforce.engineerprofilemanagement.service.impl;

import com.talentgrid.workforce.engineerprofilemanagement.dto.EmployeeProfileUpdatedPayload;
import com.talentgrid.workforce.engineerprofilemanagement.dto.InternalEmployeeResponse;
import com.talentgrid.workforce.engineerprofilemanagement.dto.SkillCatalogEntryDto;
import com.talentgrid.workforce.engineerprofilemanagement.dto.UpdateEngineerProfileRequest;
import com.talentgrid.workforce.engineerprofilemanagement.entity.InternalEmployee;
import com.talentgrid.workforce.engineerprofilemanagement.entity.SkillCatalogEntry;
import com.talentgrid.workforce.engineerprofilemanagement.enums.HrisSyncStatus;
import com.talentgrid.workforce.engineerprofilemanagement.exception.ResourceNotFoundException;
import com.talentgrid.workforce.engineerprofilemanagement.dto.UserDto;
import com.talentgrid.workforce.kafka.producer.WorkforceKafkaProducer;
import com.talentgrid.workforce.engineerprofilemanagement.repository.InternalEmployeeRepository;
import com.talentgrid.workforce.engineerprofilemanagement.repository.SkillCatalogRepository;
import com.talentgrid.workforce.engineerprofilemanagement.service.InternalEmployeeService;
import com.talentgrid.workforce.engineerprofilemanagement.service.SkillCatalogValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional(readOnly = true)
public class InternalEmployeeServiceImpl implements InternalEmployeeService {

    private static final Pattern MARKDOWN_LINK_PATTERN = Pattern.compile("^\\[(?:[^\\]]*)\\]\\((https?://[^)]+)\\)$");

    private final InternalEmployeeRepository repository;
    private final SkillCatalogRepository skillCatalogRepository;
    private final SkillCatalogValidator skillCatalogValidator;
    private final WorkforceKafkaProducer workforceKafkaProducer;

    public InternalEmployeeServiceImpl(InternalEmployeeRepository repository,
                                       SkillCatalogRepository skillCatalogRepository,
                                       SkillCatalogValidator skillCatalogValidator,
                                       WorkforceKafkaProducer workforceKafkaProducer) {
        this.repository = repository;
        this.skillCatalogRepository = skillCatalogRepository;
        this.skillCatalogValidator = skillCatalogValidator;
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
    public InternalEmployeeResponse getEmployeeByEmail(String email) {
        InternalEmployee employee = repository.findByEmailIgnoreCaseAndIsDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Internal employee not found for email: " + email));
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
    public org.springframework.data.domain.Page<InternalEmployeeResponse> getAllEngineers(int page, int size) {
        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by("id").ascending());
        return repository.findByIsDeletedFalse(pageable)
                .map(this::mapToResponse);
    }

    @Override
    public List<SkillCatalogEntryDto> getSkillCatalog() {
        return skillCatalogRepository.findAllByOrderBySkillNameAsc().stream()
                .map(this::mapToSkillCatalogDto)
                .toList();
    }

    @Override
    @Transactional
    public InternalEmployeeResponse updateOwnProfile(String emailId,
                                                     UpdateEngineerProfileRequest request,
                                                     String requestId) {

        InternalEmployee employee = repository.findByEmailIgnoreCaseAndIsDeletedFalse(emailId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Internal employee not found for email: " + emailId));

        // Reject empty PATCH
        if (request.getSkills() == null
                && request.getAvailabilityDate() == null
                && request.getResumeDriveLink() == null) {
            throw new IllegalArgumentException("At least one field must be updated");
        }

        boolean skillsChanged = false;
        boolean availabilityChanged = false;
        boolean resumeChanged = false;

        // Handle skills update safely
        if (request.getSkills() != null) {
            List<String> validatedSkills = skillCatalogValidator.resolveCanonicalSkills(request.getSkills());
            String[] newSkills = validatedSkills.toArray(new String[0]);
            skillsChanged = !Arrays.equals(employee.getSkills(), newSkills);

            if (skillsChanged) {
                employee.setSkills(newSkills);
                employee.setSkillsVector(null);
                employee.setLastEmbeddedAt(null);
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

        if (request.getResumeDriveLink() != null) {
            String newResumeDriveLink = sanitizeResumeDriveLink(request.getResumeDriveLink());
            if (StringUtils.hasText(newResumeDriveLink)) {
                resumeChanged = !Objects.equals(employee.getResumeDriveLink(), newResumeDriveLink);
                if (resumeChanged) {
                    employee.setResumeDriveLink(newResumeDriveLink);
                }
            }
        }

        // No actual changes
        if (!skillsChanged && !availabilityChanged && !resumeChanged) {
            return mapToResponse(employee);
        }

        employee.setUpdatedAt(LocalDateTime.now());

        InternalEmployee saved = repository.save(employee);

        List<String> updatedFields = new ArrayList<>();

        if (skillsChanged) {
            updatedFields.add("skills");
        }
        if (availabilityChanged) {
            updatedFields.add("availabilityDate");
        }
        if (resumeChanged) {
            updatedFields.add("resumeDriveLink");
        }

        workforceKafkaProducer.publishEmployeeProfileUpdated(
                EmployeeProfileUpdatedPayload.builder()
                        .employeeId(saved.getEmployeeId())
                        .updatedFields(updatedFields)
                        .skills(skillsChanged
                                ? (saved.getSkills() != null
                                        ? Arrays.asList(saved.getSkills())
                                        : Collections.emptyList())
                                : null)
                        .availabilityDate(availabilityChanged ? saved.getAvailabilityDate() : null)
                        .resumeDriveLink(resumeChanged ? saved.getResumeDriveLink() : null)
                        .build(),
                requestId
        );

        return mapToResponse(saved);
    }

    private String sanitizeResumeDriveLink(String resumeDriveLink) {
        String trimmed = resumeDriveLink == null ? "" : resumeDriveLink.trim();
        if (!StringUtils.hasText(trimmed)) {
            return "";
        }

        Matcher markdownMatcher = MARKDOWN_LINK_PATTERN.matcher(trimmed);
        if (markdownMatcher.matches()) {
            return markdownMatcher.group(1).trim();
        }

        return trimmed;
    }

    private SkillCatalogEntryDto mapToSkillCatalogDto(SkillCatalogEntry entry) {
        return SkillCatalogEntryDto.builder()
                .skillId(entry.getSkillId())
                .skillName(entry.getSkillName())
                .build();
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
        response.setAvailabilityDate(employee.getAvailabilityDate());
        response.setResumeDriveLink(employee.getResumeDriveLink());
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
