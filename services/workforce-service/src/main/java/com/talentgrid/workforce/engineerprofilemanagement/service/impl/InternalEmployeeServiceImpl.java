package com.talentgrid.workforce.engineerprofilemanagement.service.impl;

import com.talentgrid.workforce.engineerprofilemanagement.dto.InternalEmployeeResponse;
import com.talentgrid.workforce.engineerprofilemanagement.entity.InternalEmployee;
import com.talentgrid.workforce.engineerprofilemanagement.enums.HrisSyncStatus;
import com.talentgrid.workforce.engineerprofilemanagement.exception.ResourceNotFoundException;
import com.talentgrid.workforce.engineerprofilemanagement.kafka.producer.UserDto;
import com.talentgrid.workforce.engineerprofilemanagement.repository.InternalEmployeeRepository;
import com.talentgrid.workforce.engineerprofilemanagement.service.InternalEmployeeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
public class InternalEmployeeServiceImpl implements InternalEmployeeService {

    private final InternalEmployeeRepository repository;

    public InternalEmployeeServiceImpl(InternalEmployeeRepository repository) {
        this.repository = repository;
    }

    @Override
    public InternalEmployeeResponse getEmployeeDetailsById(String employeeId) {
        InternalEmployee employee = repository.findByEmployeeIdAndIsDeletedFalse(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Internal employee not found for id: " + employeeId));

        return mapToResponse(employee);
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

        String employeeId = String.valueOf(userDto.getEmployeeId());
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
