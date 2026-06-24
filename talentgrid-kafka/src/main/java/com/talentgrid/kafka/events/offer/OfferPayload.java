package com.talentgrid.kafka.events.offer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferPayload {

    private Long offerId;

    private Long applicationId;

    private Long demandId;

    private String role;

    private BigDecimal baseSalary;

    private BigDecimal bonus;

    private BigDecimal equity;

    private LocalDate joiningDate;

    private String status;

    private String rejectionReason;
}