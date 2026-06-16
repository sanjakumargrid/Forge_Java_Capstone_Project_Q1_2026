package offerService.offer.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApplicationDto {

    private Long applicationId;

    private Long candidateId;

    private Long demandId;

    private String currentStage;
}
