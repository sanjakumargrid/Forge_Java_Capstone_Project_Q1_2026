package com.talentgrid.workforce.rmgdashboard.dto;

import java.util.List;

public class DemandPageResponse {
    private List<DemandDto> content;

    public List<DemandDto> getContent() {
        return content;
    }

    public void setContent(List<DemandDto> content) {
        this.content = content;
    }
}
