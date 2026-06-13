package com.talentgrid.demand.security;

import com.talentgrid.demand.domain.entity.Demand;
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

    /**
     * Checks if the user is either the owner of the demand OR has an Admin/RM role 
     * that grants universal access.
     */
    public boolean isOwnerOrHasGlobalAccess(Long demandId) {
        // Here we could extract roles from SecurityContextHolder to see if they are ADMIN or RM
        // For now, if they are the owner, they definitely have access.
        // TODO: Expand to check "hasRole('ADMIN') || hasRole('RM')" once token validation is live.
        return isOwner(demandId);
    }
}
