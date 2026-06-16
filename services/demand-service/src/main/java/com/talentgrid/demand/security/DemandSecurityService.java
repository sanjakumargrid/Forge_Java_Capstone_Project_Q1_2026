package com.talentgrid.demand.security;

import com.talentgrid.demand.domain.entity.Demand;
import com.talentgrid.demand.domain.enums.DemandStatus;
import com.talentgrid.demand.repository.DemandRepository;
import com.talentgrid.demand.util.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Custom Spring Security component for evaluating data-level (scoped) permissions.
 * Evaluates rules defined in the demand-permissions.md matrix.
 * 
 * Used in @PreAuthorize annotations, e.g.:
 * @PreAuthorize("hasAuthority('DEMAND_UPDATE') and @demandSecurity.isOwnerOrAdmin(#id)")
 */
@Component("demandSecurity")
public class DemandSecurityService {

    private static final Logger log = LoggerFactory.getLogger(DemandSecurityService.class);
    private final DemandRepository demandRepository;

    public DemandSecurityService(DemandRepository demandRepository) {
        this.demandRepository = demandRepository;
    }

    /**
     * Checks if the currently authenticated user is the creator of the specified demand.
     * Used for scoped permissions (⚡) where HMs can only edit/delete their own demands.
     *
     * @param demandId the ID of the demand
     * @return true if the current user created the demand, false otherwise
     */
    public boolean isOwner(Long demandId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            return false;
        }

        Optional<Demand> demandOpt = demandRepository.findById(demandId);
        if (demandOpt.isEmpty()) {
            return false; // Let the controller throw 404
        }

        Demand demand = demandOpt.get();
        boolean isOwner = currentUserId.equals(demand.getCreatedBy());
        
        if (!isOwner) {
            log.warn("User {} denied scoped access to demand {} (owner: {})", 
                    currentUserId, demandId, demand.getCreatedBy());
        }
        
        return isOwner;
    }

    public boolean isOwnerOrHasGlobalAccess(Long demandId) {
        if (SecurityUtils.hasAnyRole("ADMIN", "RMG")) {
            return true;
        }
        return isOwner(demandId);
    }

    /**
     * Checks if the user can view the demand based on their role and the demand's status.
     */
    public boolean canView(Long demandId) {
        if (isOwnerOrHasGlobalAccess(demandId)) {
            return true;
        }

        Optional<Demand> demandOpt = demandRepository.findById(demandId);
        if (demandOpt.isEmpty()) {
            return false;
        }

        Demand demand = demandOpt.get();
        DemandStatus status = demand.getStatus();

        if (SecurityUtils.hasAnyRole("RECRUITER")) {
            return status == DemandStatus.OPEN_EXTERNAL ||
                   status == DemandStatus.FILLED_PARTIALLY ||
                   status == DemandStatus.FILLED_EXTERNAL ||
                   status == DemandStatus.CLOSED;
        }

        if (SecurityUtils.hasAnyRole("EMPLOYEE")) {
            return status == DemandStatus.OPEN_EXTERNAL;
        }

        if (SecurityUtils.hasAnyRole("HM")) {
            Long userAccountId = SecurityUtils.getCurrentUserAccountId();
            if (userAccountId != null && userAccountId.equals(demand.getAccountId())) {
                return true;
            }
            return status == DemandStatus.OPEN_EXTERNAL;
        }

        // Default fallback
        return status == DemandStatus.OPEN_EXTERNAL;
    }

    /**
     * Checks if the user is authorized to perform state transitions on the demand.
     */
    public boolean canTransition(Long demandId) {
        if (SecurityUtils.hasAnyRole("ADMIN", "RMG")) {
            return true;
        }

        if (SecurityUtils.hasAnyRole("RECRUITER")) {
            Optional<Demand> demandOpt = demandRepository.findById(demandId);
            if (demandOpt.isEmpty()) {
                return false;
            }
            DemandStatus status = demandOpt.get().getStatus();
            // Recruiters can transition demands that are actively recruiting externally
            return status == DemandStatus.OPEN_EXTERNAL ||
                   status == DemandStatus.FILLED_PARTIALLY;
        }

        return false;
    }

    /**
     * Checks if the user is authorized to close or cancel a demand.
     * Only RMG and ADMIN roles have this permission.
     */
    public boolean canCloseOrCancel() {
        return SecurityUtils.hasAnyRole("ADMIN", "RMG");
    }
}
