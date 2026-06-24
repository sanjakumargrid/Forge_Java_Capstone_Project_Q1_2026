package offerService.offer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;


import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import offerService.offer.enums.Status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class OfferDto {

  private Long id;

  @NotNull(message = "Application id is required")
  private Long applicationId;

  @Size(max = 255, message = "Candidate name must not exceed 255 characters")
  private String candidateName;

  @NotBlank(message = "Role is required")
  @Size(max = 100, message = "Role must not exceed 100 characters")
  private String role;

  @NotNull(message = "Base salary is required")
  @DecimalMin(
      value = "0.0",
      inclusive = false,
      message = "Base salary must be greater than 0"
  )
  private BigDecimal baseSalary;

  @DecimalMin(
      value = "0.0",
      message = "Bonus cannot be negative"
  )
  private BigDecimal bonus;

  @DecimalMin(
      value = "0.0",
      message = "Equity cannot be negative"
  )
  private BigDecimal equity;

  @NotNull(message = "Joining date is required")
  @FutureOrPresent(message = "Joining date must be today or in the future")
  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate joiningDate;

  @NotNull(message = "Offer status is required")
  private Status offerStatus;

  @NotBlank(message = "Employment type is required")
  @Size(
      max = 50,
      message = "Employment type must not exceed 50 characters"
  )
  private String employmentType;

  @Size(
      max = 100,
      message = "Approved by must not exceed 100 characters"
  )
  private String approvedBy;

  @Size(
      max = 100,
      message = "Rejected by must not exceed 100 characters"
  )
  private String rejectedBy;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime approvedAt;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime rejectedAt;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime sentAt;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime signedAt;

  @Future(message = "Offer expiry date must be in the future")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime expiresAt;

  @Size(
      max = 150,
      message = "DocuSign ID must not exceed 150 characters"
  )
  private String docuSignId;
}