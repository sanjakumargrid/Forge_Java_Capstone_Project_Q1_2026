package com.talentgrid.interview.interview.service;

import com.talentgrid.interview.client.EmployeeClient;
import com.talentgrid.interview.client.dto.EmployeeDto;
import com.talentgrid.interview.exception.BusinessException;
import com.talentgrid.interview.interview.dto.InterviewerRequestDto;
import com.talentgrid.interview.interview.dto.InterviewerResponseDto;
import com.talentgrid.interview.interview.entity.Interviewer;
import com.talentgrid.interview.interview.repository.InterviewerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewerService {

    private final InterviewerRepository interviewerRepository;
    private final EmployeeClient employeeClient;

    @Transactional
    public InterviewerResponseDto addInterviewer(InterviewerRequestDto request) {
        if (interviewerRepository.existsById(request.getEmployeeId())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Employee " + request.getEmployeeId() + " is already an interviewer");
        }

        // Verify the employee exists in the organization
        EmployeeDto employee = employeeClient.getEmployee(request.getEmployeeId());

        Interviewer interviewer = Interviewer.builder()
                .employeeId(request.getEmployeeId())
                .domainName(request.getDomainName())
                .location(request.getLocation())
                .grade(request.getGrade())
                .build();

        Interviewer saved = interviewerRepository.save(interviewer);
        return toDto(saved, employee);
    }

    @Transactional(readOnly = true)
    public List<InterviewerResponseDto> searchInterviewers(String location, String domainName) {
        List<Interviewer> interviewers;

        if (location != null && domainName != null) {
            interviewers = interviewerRepository.findByLocationAndDomainName(location, domainName);
        } else if (location != null) {
            interviewers = interviewerRepository.findByLocation(location);
        } else if (domainName != null) {
            interviewers = interviewerRepository.findByDomainName(domainName);
        } else {
            interviewers = interviewerRepository.findAll();
        }

        return interviewers.stream()
                .map(this::toDtoWithoutEmployeeData)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public InterviewerResponseDto getInterviewer(Long employeeId) {
        Interviewer interviewer = interviewerRepository.findById(employeeId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Interviewer not found with ID: " + employeeId));
                
        EmployeeDto employee = employeeClient.getEmployee(employeeId);
        return toDto(interviewer, employee);
    }

    @Transactional
    public void removeInterviewer(Long employeeId) {
        if (!interviewerRepository.existsById(employeeId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Interviewer not found with ID: " + employeeId);
        }
        interviewerRepository.deleteById(employeeId);
    }

    private InterviewerResponseDto toDto(Interviewer interviewer, EmployeeDto employee) {
        String firstName = null;
        String lastName = null;
        
        if (employee != null && employee.getName() != null) {
            String[] parts = employee.getName().split(" ", 2);
            firstName = parts[0];
            if (parts.length > 1) {
                lastName = parts[1];
            }
        }
        
        return InterviewerResponseDto.builder()
                .employeeId(interviewer.getEmployeeId())
                .domainName(interviewer.getDomainName())
                .location(interviewer.getLocation())
                .grade(interviewer.getGrade())
                .email(employee != null ? employee.getEmail() : null)
                .firstName(firstName)
                .lastName(lastName)
                .build();
    }
    
    private InterviewerResponseDto toDtoWithoutEmployeeData(Interviewer interviewer) {
        return InterviewerResponseDto.builder()
                .employeeId(interviewer.getEmployeeId())
                .domainName(interviewer.getDomainName())
                .location(interviewer.getLocation())
                .grade(interviewer.getGrade())
                .build();
    }
}
