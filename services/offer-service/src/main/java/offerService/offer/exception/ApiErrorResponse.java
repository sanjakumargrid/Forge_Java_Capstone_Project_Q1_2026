package offerService.offer.exception;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApiErrorResponse {

    private LocalDateTime timestamp;

    private String message;

    private int status;

    private String error;

    public ApiErrorResponse(
            int status,
            String error,
            String message
    ) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.error = error;
        this.message = message;
    }
}