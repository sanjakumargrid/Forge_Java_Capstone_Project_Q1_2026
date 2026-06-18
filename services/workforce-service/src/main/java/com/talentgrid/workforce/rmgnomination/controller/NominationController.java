package com.talentgrid.workforce.rmgnomination.controller;

import com.talentgrid.workforce.rmgnomination.dto.NominationRequest;
import com.talentgrid.workforce.rmgnomination.dto.NominationResponse;
import com.talentgrid.workforce.rmgnomination.service.NominationService;
import com.talentgrid.workforce.rmgdashboard.dto.DemandDto;
import com.talentgrid.workforce.skillgapheatmap.provider.model.DemandServiceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rmg/nominations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "RMG Nomination", description = "Nomination APIs for RMG nomination flow")
public class NominationController {

    private final NominationService nominationService;

    @PostMapping
    @PreAuthorize("hasAuthority('WORKFORCE_NOMINATION_CREATE')")
    @Operation(summary = "Nominate engineer for demand",
            description = "Creates a manual nomination after duplicate and utilisation checks")
    public ResponseEntity<NominationResponse> nominateEngineer(@Valid @RequestBody NominationRequest request) {
        log.info("Received nomination request for employeeId={} and demandId={}",
                request.getEmployeeId(), request.getDemandId());
        NominationResponse response = nominationService.nominate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/demand/{demandId}")
    @PreAuthorize("hasAuthority('WORKFORCE_NOMINATION_VIEW')")
    @Operation(summary = "Get nominations by demand",
            description = "Returns all non-deleted nominations created for a given demand ID")
    public ResponseEntity<List<NominationResponse>> getNominationsByDemand(@PathVariable Long demandId) {
        return ResponseEntity.ok(nominationService.getNominationsByDemand(demandId));
    }

    @GetMapping("/engineer/{employeeId}")
    @PreAuthorize("hasAuthority('WORKFORCE_NOMINATION_VIEW')")
    @Operation(summary = "Get nominations by engineer",
            description = "Returns all non-deleted nominations for a specific engineer")
    public ResponseEntity<List<NominationResponse>> getNominationsByEngineer(@PathVariable Long employeeId) {
        return ResponseEntity.ok(nominationService.getNominationsByEngineer(employeeId));
    }

    @GetMapping("/getDemandById/{demandId}")
    @PreAuthorize("hasAuthority('WORKFORCE_NOMINATION_VIEW')")
    @Operation(summary = "Get demand details by demand ID",
            description = "Fetches demand details from demand-service by demand ID for RMG nomination flow")
    public ResponseEntity<DemandServiceResponse> getDemandById(@PathVariable Long demandId) {
        return ResponseEntity.ok(nominationService.getDemandDetailsById(demandId));
    }

    @PatchMapping("/demands/{demandId}/internal-search")
    @PreAuthorize("hasAuthority('DEMAND_STATUS_TRANSITION')")
    @Operation(summary = "Move demand to INTERNAL_SEARCH for nomination",
            description = "Transitions demand from APPROVED to INTERNAL_SEARCH using demand-service transition API")
    public ResponseEntity<DemandDto> moveDemandToInternalSearch(@PathVariable Long demandId) {
        return ResponseEntity.ok(nominationService.moveDemandToInternalSearch(demandId));
    }
}
