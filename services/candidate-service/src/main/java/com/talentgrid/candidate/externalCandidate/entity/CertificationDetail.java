package com.talentgrid.candidate.externalCandidate.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
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
@Entity
@Table(name = "certification_detail")
public class CertificationDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "certificate_id")
    private Long certificateId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private ExternalCandidate candidate;

    @NotBlank(message = "Certificate name is required")
    @Size(max = 150)
    @Column(name = "certificate_name", nullable = false)
    private String certificateName;

    @NotBlank(message = "Issuing organization is required")
    @Size(max = 150)
    @Column(name = "issuing_organization", nullable = false)
    private String issuingOrganization;

    @NotNull(message = "Issued date is required")
    @PastOrPresent(message = "Issued date cannot be in the future")
    @JsonFormat(pattern = "dd-MM-yyyy")
    @Column(name = "issued_date", nullable = false)
    private LocalDate issuedDate;

    @Size(max = 500)
    @Column(name = "certificate_file_path")
    @URL(message = "Invalid file path")
    private String certificateFilePath;
}