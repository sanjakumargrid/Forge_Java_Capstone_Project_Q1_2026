package com.talentgrid.workforce.engineerprofilemanagement.service;

import com.talentgrid.workforce.engineerprofilemanagement.dto.InternalEmployeeResponse;
import com.talentgrid.workforce.engineerprofilemanagement.kafka.producer.UserDto;
import org.springframework.stereotype.Service;

@Service
public interface InternalEmployeeService {

    InternalEmployeeResponse getEmployeeDetailsById(String employeeId);

    InternalEmployeeResponse syncEmployeeFromKafka(UserDto userDto);
}
