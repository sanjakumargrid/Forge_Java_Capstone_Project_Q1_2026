package com.talentgrid.workforce.engineerprofilemanagement.service;

import com.talentgrid.workforce.engineerprofilemanagement.dto.InternalEmployeeResponse;
import com.talentgrid.workforce.engineerprofilemanagement.dto.SkillCatalogEntryDto;
import com.talentgrid.workforce.engineerprofilemanagement.dto.UpdateEngineerProfileRequest;
import com.talentgrid.workforce.engineerprofilemanagement.dto.WorkforceAnalyticsResponse;
import com.talentgrid.workforce.engineerprofilemanagement.dto.UserDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface InternalEmployeeService {

    InternalEmployeeResponse getEmployeeDetailsById(Long employeeId);

    InternalEmployeeResponse getEmployeeByEmail(String email);

    InternalEmployeeResponse getEmployeeByDatabaseId(Long id);

    List<InternalEmployeeResponse> getAllEngineers();

    org.springframework.data.domain.Page<InternalEmployeeResponse> getAllEngineers(int page, int size);

    WorkforceAnalyticsResponse getWorkforceAnalytics();

    InternalEmployeeResponse updateOwnProfile(String emailId, UpdateEngineerProfileRequest request, String requestId);

    List<SkillCatalogEntryDto> getSkillCatalog();

    InternalEmployeeResponse syncEmployeeFromKafka(UserDto userDto);

    void deleteEmployeeByEmployeeId(Long employeeId);
}
