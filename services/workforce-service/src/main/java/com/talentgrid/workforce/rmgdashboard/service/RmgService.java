package com.talentgrid.workforce.rmgdashboard.service;

import com.talentgrid.workforce.rmgdashboard.client.DemandClient;
import com.talentgrid.workforce.rmgdashboard.dto.DemandDto;
import com.talentgrid.workforce.rmgdashboard.dto.StatusUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RmgService {

    private final DemandClient demandClient;

    public Page<DemandDto> getDemandsByStatus(String status, Pageable pageable) {
        log.info("Fetching demands from demand-service with status={}, page={}, size={}",
                status, pageable.getPageNumber(), pageable.getPageSize());

        List<DemandDto> allDemands = demandClient.getDemandsByStatus(status);
        if (allDemands == null) {
            allDemands = List.of();
        }
        List<DemandDto> filteredDemands = allDemands.stream()
                .filter(demand -> status.equalsIgnoreCase(demand.getStatus()))
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filteredDemands.size());

        if (start >= filteredDemands.size()) {
            return new PageImpl<>(List.of(), pageable, filteredDemands.size());
        }

        List<DemandDto> pageContent = filteredDemands.subList(start, end);
        return new PageImpl<>(pageContent, pageable, filteredDemands.size());
    }

    public DemandDto updateDemandStatus(Long demandId, String status) {
        StatusUpdateRequest statusUpdateRequest = StatusUpdateRequest.builder()
                .status(status)
                .build();

        return demandClient.updateDemandStatus(demandId, statusUpdateRequest);
    }
}
