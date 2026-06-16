package com.talentgrid.candidate.externalCandidate.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDate;

@Getter
@Setter
public class CertificationDetailDto {

    @NotBlank(message = "Certificate name is required")
    @Size(max = 150, message = "Certificate name must not exceed 150 characters")
    private String certificateName;

    @NotBlank(message = "Issuing organization is required")
    @Size(max = 150, message = "Issuing organization must not exceed 150 characters")
    private String issuingOrganization;

    @NotNull(message = "Issued date is required")
    @PastOrPresent(message = "Issued date cannot be in the future")
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate issuedDate;

    @Size(max = 500)
    @Column(name = "certificate_file_path")
    @URL(message = "Invalid file path")
    private String certificateFilePath;
}