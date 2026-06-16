package offerService.offer.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApprovalStep {

    private Integer orderNumber;

    private String approverEmail;

    private String approverName;

    private Boolean approved = false;

    private String approvedBy;

    private String comments;
}