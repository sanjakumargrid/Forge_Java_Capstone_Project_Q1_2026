package com.talentgrid.demand.domain.entity;

import com.talentgrid.demand.domain.enums.DemandStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "demand_status_history")
public class DemandStatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "demand_id")
    private Demand demand;

    @Enumerated(EnumType.STRING)
    private DemandStatus fromStatus;

    @Enumerated(EnumType.STRING)
    private DemandStatus toStatus;

    private String changedBy;
    private String comments;
    private LocalDateTime changedAt;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Demand getDemand() { return demand; }
    public void setDemand(Demand demand) { this.demand = demand; }
    public DemandStatus getFromStatus() { return fromStatus; }
    public void setFromStatus(DemandStatus fromStatus) { this.fromStatus = fromStatus; }
    public DemandStatus getToStatus() { return toStatus; }
    public void setToStatus(DemandStatus toStatus) { this.toStatus = toStatus; }
    public String getChangedBy() { return changedBy; }
    public void setChangedBy(String changedBy) { this.changedBy = changedBy; }
    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
}
