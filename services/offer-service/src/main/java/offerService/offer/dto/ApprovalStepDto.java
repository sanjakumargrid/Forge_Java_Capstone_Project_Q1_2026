package offerService.offer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ApprovalStepDto {

    private Integer stepOrder;

    private String approverEmail;

    private String approverName;

    private Boolean approved = false;

    private String approvedBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approvedAt;

    private String comments;
}