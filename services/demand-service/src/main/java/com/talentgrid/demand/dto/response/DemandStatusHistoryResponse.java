package com.talentgrid.demand.dto.response;

public class DemandStatusHistoryResponse {
    private Long id;
    private String fromStatus;
    private String toStatus;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFromStatus() { return fromStatus; }
    public void setFromStatus(String fromStatus) { this.fromStatus = fromStatus; }
    public String getToStatus() { return toStatus; }
    public void setToStatus(String toStatus) { this.toStatus = toStatus; }
}
