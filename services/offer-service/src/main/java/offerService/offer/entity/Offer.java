package offerService.offer.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import offerService.offer.dto.ApprovalStep;
import offerService.offer.enums.Status;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "offer")
public class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "offer_id")
    private Long id;

    @NotNull(message = "Application ID is required")
    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Size(max = 255, message = "Candidate name must not exceed 255 characters")
    @Column(name = "candidate_name", length = 255)
    private String candidateName;

    @Column(name = "current_approval_step")
    private Integer currentApprovalStep = 0;

    @NotBlank(message = "Role is required")
    @Size(max = 100, message = "Role must not exceed 100 characters")
    @Column(name = "role", nullable = false)
    private String role;

    @NotNull(message = "Base salary is required")
    @DecimalMin(
            value = "0.0",
            inclusive = false,
            message = "Base salary must be greater than 0"
    )
    @Column(name = "base_salary", nullable = false)
    private BigDecimal baseSalary;

    @DecimalMin(value = "0.0", message = "Bonus cannot be negative")
    @Column(name = "bonus")
    private BigDecimal bonus;

    @DecimalMin(value = "0.0", message = "Equity cannot be negative")
    @Column(name = "equity")
    private BigDecimal equity;

    @NotNull(message = "Joining date is required")
    @Future(message = "Joining date must be in the future")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Column(name = "joining_date", nullable = false)
    private LocalDate joiningDate;

    @NotNull(message = "Offer status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "offer_status", nullable = false)
    private Status offerStatus;

    @NotBlank(message = "Employment type is required")
    @Size(max = 50, message = "Employment type must not exceed 50 characters")
    @Column(name = "employment_type", nullable = false)
    private String employmentType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "approval_chain", columnDefinition = "jsonb")
    private List<ApprovalStep> approvalChain;

    @Size(max = 100, message = "Approved by must not exceed 100 characters")
    @Column(name = "approved_by")
    private String approvedBy;

    @Size(max = 100, message = "Rejected by must not exceed 100 characters")
    @Column(name = "rejected_by")
    private String rejectedBy;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @Future(message = "Offer expiry date must be in the future")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Size(max = 150, message = "DocuSign ID must not exceed 150 characters")
    @Column(name = "docu_sign_id", length = 150)
    private String docuSignId;

    @PrePersist
    private void onCreate() {
        if (this.offerStatus == null) {
            this.offerStatus = Status.DRAFT;
        }

        if (this.currentApprovalStep == null) {
            this.currentApprovalStep = 0;
        }
    }
}
