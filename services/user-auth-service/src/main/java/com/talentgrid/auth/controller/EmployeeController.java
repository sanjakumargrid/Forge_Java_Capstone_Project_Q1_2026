package com.talentgrid.auth.controller;

import com.talentgrid.auth.dto.response.EmployeeDto;
import com.talentgrid.auth.entity.User;
import com.talentgrid.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final UserRepository userRepository;

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeDto> getEmployeeById(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        EmployeeDto dto = EmployeeDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getUsername()) // Using username as name
                .isInterviewerEligible(user.getIsInterviewerEligible())
                .build();

        return ResponseEntity.ok(dto);
    }

    @GetMapping
    public ResponseEntity<List<EmployeeDto>> getEmployeesByLocation(
            @RequestParam String location,
            @RequestParam Boolean isInterviewerEligible) {

        List<User> users = userRepository.findByLocationAndIsInterviewerEligible(location, isInterviewerEligible);

        List<EmployeeDto> employeeDtos = users.stream()
                .map(user -> EmployeeDto.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .name(user.getUsername())
                        .isInterviewerEligible(user.getIsInterviewerEligible())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(employeeDtos);
    }
}
