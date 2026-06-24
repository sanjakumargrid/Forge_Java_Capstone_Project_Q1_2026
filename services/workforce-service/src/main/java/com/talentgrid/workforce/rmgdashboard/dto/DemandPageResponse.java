package com.talentgrid.workforce.rmgdashboard.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DemandPageResponse {
    private List<DemandDto> content;

    public List<DemandDto> getContent() {
        return content;
    }

    public void setContent(List<DemandDto> content) {
        this.content = content;
    }
}
