package com.talentgrid.workforce.rmgdashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PagedResponse<T> {
    private List<T> content;
    private int page;
    private int pageSize;
    private long totalElements;
    private int totalPages;
}
