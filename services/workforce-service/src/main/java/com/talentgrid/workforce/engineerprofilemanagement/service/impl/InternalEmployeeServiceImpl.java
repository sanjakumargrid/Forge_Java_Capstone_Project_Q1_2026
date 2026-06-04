package com.talentgrid.workforce.engineerprofilemanagement.service.impl;

import com.talentgrid.workforce.engineerprofilemanagement.dto.InternalEmployeeResponse;
import com.talentgrid.workforce.engineerprofilemanagement.entity.InternalEmployee;
import com.talentgrid.workforce.engineerprofilemanagement.exception.ResourceNotFoundException;
import com.talentgrid.workforce.engineerprofilemanagement.repository.InternalEmployeeRepository;
import com.talentgrid.workforce.engineerprofilemanagement.service.InternalEmployeeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
