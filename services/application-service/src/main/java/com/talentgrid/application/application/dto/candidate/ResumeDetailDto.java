package com.talentgrid.application.application.dto.candidate;


import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResumeDetailDto {

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
