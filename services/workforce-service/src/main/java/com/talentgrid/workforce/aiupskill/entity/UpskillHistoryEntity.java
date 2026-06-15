package com.talentgrid.workforce.aiupskill.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "upskill_history", indexes = {
        @Index(name = "idx_employee_demand", columnList = "employee_id, demand_id")
})
public class UpskillHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "employee_id", nullable = false)
    private String employeeId;

    @Column(name = "demand_id", nullable = false)
    private String demandId;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Column(name = "raw_payload_json", columnDefinition = "TEXT", nullable = false)
    private String rawPayloadJson;

    // Boilerplate Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getDemandId() { return demandId; }
    public void setDemandId(String demandId) { this.demandId = demandId; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

    public String getRawPayloadJson() { return rawPayloadJson; }
    public void setRawPayloadJson(String rawPayloadJson) { this.rawPayloadJson = rawPayloadJson; }
}