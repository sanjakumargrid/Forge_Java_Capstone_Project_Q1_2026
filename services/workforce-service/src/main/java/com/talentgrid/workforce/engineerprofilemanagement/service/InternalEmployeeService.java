package com.talentgrid.workforce.engineerprofilemanagement.service;

import com.talentgrid.workforce.engineerprofilemanagement.dto.InternalEmployeeResponse;
import com.talentgrid.workforce.engineerprofilemanagement.dto.UpdateEngineerProfileRequest;
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

    InternalEmployeeResponse updateOwnProfile(String emailId, UpdateEngineerProfileRequest request, String requestId);

    InternalEmployeeResponse syncEmployeeFromKafka(UserDto userDto);

    void deleteEmployeeByEmployeeId(Long employeeId);
}
