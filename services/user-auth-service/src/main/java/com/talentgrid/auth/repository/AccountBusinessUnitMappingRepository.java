package com.talentgrid.auth.repository;

import com.talentgrid.auth.entity.AccountBusinessUnitMapping;
import com.talentgrid.auth.entity.BusinessUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountBusinessUnitMappingRepository extends JpaRepository<AccountBusinessUnitMapping, Long> {

    @Query("SELECT m.businessUnit FROM AccountBusinessUnitMapping m WHERE m.account.id = :accountId")
    List<BusinessUnit> findBusinessUnitsByAccountId(@Param("accountId") Long accountId);
}
