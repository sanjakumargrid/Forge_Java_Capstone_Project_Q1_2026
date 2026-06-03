package com.talentgrid.demand.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class DemandExceptionHandler {

    @ExceptionHandler(DemandNotFoundException.class)
    public ResponseEntity<String> handleNotFound(DemandNotFoundException ex) {
        return ResponseEntity.status(404).body(ex.getMessage());
    }

    @ExceptionHandler({
        InvalidDemandStateException.class,
        IllegalDemandTransitionException.class,
        DemandAlreadyExistsException.class
    })
    public ResponseEntity<String> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.status(400).body(ex.getMessage());
    }
}
