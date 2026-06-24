package com.talentgrid.workforce.rmgnomination.service;

import com.talentgrid.workforce.rmgnomination.entity.InternalMatch;
import com.talentgrid.workforce.rmgnomination.exception.NominationBusinessRuleException;
import com.talentgrid.workforce.rmgnomination.repository.InternalMatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NominationValidationService {

    private final InternalMatchRepository internalMatchRepository;

    @Transactional(readOnly = true)
    public List<InternalMatch> getActiveEngineerNominations(Long employeeId) {
        return internalMatchRepository.findByEmployee_IdAndIsDeletedFalse(employeeId);
    }

}