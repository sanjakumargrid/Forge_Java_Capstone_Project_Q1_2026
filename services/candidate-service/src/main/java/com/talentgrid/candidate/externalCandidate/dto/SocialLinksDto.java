package com.talentgrid.candidate.externalCandidate.dto;

import com.talentgrid.candidate.externalCandidate.enums.Social;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.URL;

@Getter
@Setter
public class SocialLinksDto {



  @NotNull(message = "Social platform is required")
  private Social social;

  @NotBlank(message = "Social link is required")
  @URL(message = "Invalid social link URL")
  @Size(max = 500, message = "Social link must not exceed 500 characters")
  private String links;
}
