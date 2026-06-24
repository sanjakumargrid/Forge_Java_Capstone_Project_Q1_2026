package offerService.offer.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApplicationDto {

    private Long applicationId;

    private Long candidateId;

    @com.fasterxml.jackson.annotation.JsonAlias("demandId")
    private Long jobPostingId;

    private String currentStage;
}
