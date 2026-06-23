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
 */
@Component("demandSecurity")
public class DemandSecurityService {

    private static final Logger log = LoggerFactory.getLogger(DemandSecurityService.class);
    private final DemandRepository demandRepository;

    public DemandSecurityService(DemandRepository demandRepository) {
        this.demandRepository = demandRepository;
    }

    public boolean isOwner(Long demandId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            return false;
        }

        Optional<Demand> demandOpt = demandRepository.findByDemandIdAndIsDeletedFalse(demandId);
        if (demandOpt.isEmpty()) {
            return false;
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
        if (SecurityUtils.isPlatformAdmin()) {
            return true;
        }
        return isOwner(demandId);
    }

    public boolean canView(Long demandId) {
        if (SecurityUtils.isPlatformAdmin()) {
            return true;
        }

        Optional<Demand> demandOpt = demandRepository.findById(demandId);
        if (demandOpt.isEmpty()) {
            return true;
        }

        Demand demand = demandOpt.get();
        if (Boolean.TRUE.equals(demand.getIsDeleted())) {
            Long currentUserId = SecurityUtils.getCurrentUserId();
            return currentUserId != null && currentUserId.equals(demand.getCreatedBy());
        }

        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId != null && currentUserId.equals(demand.getCreatedBy())) {
            return true;
        }

        DemandStatus status = demand.getStatus();

        if (SecurityUtils.isPortfolioManager()) {
            return true;
        }

        if (SecurityUtils.isRecruiter()) {
            return status == DemandStatus.OPEN_EXTERNAL
                    || status == DemandStatus.FILLED
                    || status == DemandStatus.CLOSED;
        }

        if (SecurityUtils.isEmployee()) {
            return status == DemandStatus.OPEN_EXTERNAL;
        }

        if (SecurityUtils.isHiringManager()) {
            Long userAccountId = SecurityUtils.getCurrentUserAccountId();
            if (userAccountId != null && userAccountId.equals(demand.getAccountId())) {
                return true;
            }
            return status == DemandStatus.OPEN_EXTERNAL;
        }

        if (SecurityUtils.isResourceManager()) {
            return status == DemandStatus.INTERNAL_SEARCH
                    || status == DemandStatus.ON_HOLD
                    || status == DemandStatus.OPEN_EXTERNAL
                    || status == DemandStatus.CLOSED;
        }

        if (SecurityUtils.isTaManager()) {
            return status == DemandStatus.OPEN_EXTERNAL
                    || status == DemandStatus.CLOSED;
        }

        return status == DemandStatus.OPEN_EXTERNAL;
    }

    /**
     * True when the caller may call PATCH /status for this demand (fine-grained checks also run in service).
     */
    public boolean canTransition(Long demandId) {
        if (SecurityUtils.isPlatformAdmin()) {
            return true;
        }
        Optional<Demand> demandOpt = demandRepository.findByDemandIdAndIsDeletedFalse(demandId);
        if (demandOpt.isEmpty()) {
            return false;
        }
        Demand demand = demandOpt.get();
        DemandStatus status = demand.getStatus();

        if (isOwner(demandId) && status != DemandStatus.CLOSED) {
            return true;
        }

        if (SecurityUtils.isPortfolioManager()) {
            return true;
        }

        if (SecurityUtils.isResourceManager()) {
            return status == DemandStatus.INTERNAL_SEARCH
                    || status == DemandStatus.ON_HOLD
                    || status == DemandStatus.OPEN_EXTERNAL;
        }

        if (SecurityUtils.isTaManager()) {
            return status == DemandStatus.OPEN_EXTERNAL;
        }

        if (SecurityUtils.isRecruiter()) {
            return status == DemandStatus.OPEN_EXTERNAL || status == DemandStatus.ON_HOLD;
        }

        return false;
    }

    public boolean canCloseOrCancel() {
        return SecurityUtils.isPlatformAdmin();
    }
}
