package com.talentgrid.candidate.externalCandidate.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "resume_detail")
public class ResumeDetail {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "resume_id")
  private Long resumeId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "candidate_id", nullable = false)
  private ExternalCandidate candidate;

  @NotBlank(message = "Resume file path is required")
  @Size(max = 500)
  @Column(
          name = "resume_file_path",
          nullable = false
  )
  private String resumeFilePath;

  @NotBlank(message = "Original filename is required")
  @Size(max = 255)
  @Column(
          name = "resume_original_filename",
          nullable = false
  )
  private String resumeOriginalFilename;

}
