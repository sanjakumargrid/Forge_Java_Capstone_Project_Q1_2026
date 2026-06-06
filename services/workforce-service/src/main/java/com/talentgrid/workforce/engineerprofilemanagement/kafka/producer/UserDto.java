package com.talentgrid.workforce.engineerprofilemanagement.kafka.producer;



import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class UserDto {

    private Long employeeId;

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 150)
    private String designation;

    @Size(max = 150)
    private String department;

    @Size(max = 150)
    private String businessUnit;

    @Size(max = 150)
    private String location;

    @Size(max = 20)
    private String phoneNumber;

    private String pictureUrl;

    @Size(max = 255)
    private String ssoId;

    @Size(max = 100)
    private String ssoProvider;

    private Boolean isActive;

    private OffsetDateTime lastLoginAt;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    private OffsetDateTime availableFrom;
}