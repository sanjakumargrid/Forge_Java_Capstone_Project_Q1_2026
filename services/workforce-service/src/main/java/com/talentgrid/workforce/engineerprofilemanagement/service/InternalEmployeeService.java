package com.talentgrid.workforce.engineerprofilemanagement.service;

import com.talentgrid.workforce.engineerprofilemanagement.dto.InternalEmployeeResponse;
import org.springframework.stereotype.Service;

@Service
public interface InternalEmployeeService {

    InternalEmployeeResponse getEmployeeDetailsById(String employeeId);
}
