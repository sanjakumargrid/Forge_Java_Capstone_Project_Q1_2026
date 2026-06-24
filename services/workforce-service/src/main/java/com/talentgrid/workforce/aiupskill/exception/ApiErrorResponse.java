package com.talentgrid.workforce.aiupskill.exception;

import java.time.LocalDateTime;

public record ApiErrorResponse(
        LocalDateTime timestamp,
        int status,
        String errorCode,
        String message,
        String path
) {}
