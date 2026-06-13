package com.talentgrid.candidate.externalCandidate.dto;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddressDto {

  @NotNull(message = "Street1 cannot be null")
  private String street1;

  @NotNull(message = "Street2 cannot be null")
  private String street2;

  @NotNull(message = "City cannot be null")
  private String city;

  @NotNull(message = "State cannot be null")
  private String state;

  @NotNull(message = "Country cannot be null")
  private String country;

  @NotNull(message = "Zip code cannot be null")
  @Min(value = 100000, message = "Zip code must be 6 digits")
  @Max(value = 999999, message = "Zip code must be 6 digits")
  private Long zipCode;
}