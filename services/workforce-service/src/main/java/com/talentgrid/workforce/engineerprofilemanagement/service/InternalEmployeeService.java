package com.talentgrid.workforce.engineerprofilemanagement.service;

import com.talentgrid.workforce.engineerprofilemanagement.dto.InternalEmployeeResponse;
import com.talentgrid.workforce.engineerprofilemanagement.dto.UpdateEngineerProfileRequest;
import com.talentgrid.workforce.engineerprofilemanagement.kafka.producer.UserDto;
import org.springframework.stereotype.Service;

@Service
public interface InternalEmployeeService {

    InternalEmployeeResponse getEmployeeDetailsById(String employeeId);

    InternalEmployeeResponse updateOwnProfile(String employeeId, UpdateEngineerProfileRequest request, String requestId);

    InternalEmployeeResponse syncEmployeeFromKafka(UserDto userDto);
}
