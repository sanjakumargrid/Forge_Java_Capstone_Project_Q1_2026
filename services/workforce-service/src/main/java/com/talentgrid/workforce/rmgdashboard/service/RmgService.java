package com.talentgrid.workforce.rmgdashboard.service;

import com.talentgrid.workforce.rmgdashboard.client.DemandClient;
import com.talentgrid.workforce.rmgdashboard.dto.DemandDto;
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

    public Page<DemandDto> getPendingApprovalDemands(Pageable pageable) {
        log.info("Fetching pending approval demands from demand-service, page={}, size={}", 
                pageable.getPageNumber(), pageable.getPageSize());
        
        List<DemandDto> allDemands = demandClient.getDemandsByStatus("PENDING_APPROVAL");
        log.info("Received {} demands with status PENDING_APPROVAL from demand-service", allDemands.size());
        
        // Filter demands with PENDING_APPROVAL status
        List<DemandDto> pendingDemands = allDemands.stream()
                .filter(demand -> "PENDING_APPROVAL".equalsIgnoreCase(demand.getStatus()))
                .collect(Collectors.toList());
        
        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), pendingDemands.size());
        
        List<DemandDto> pageContent = pendingDemands.subList(start, end);
        
        return new PageImpl<>(pageContent, pageable, pendingDemands.size());
    }
}
