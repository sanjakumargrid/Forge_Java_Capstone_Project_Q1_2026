package com.talentgrid.demand.repository;

import com.talentgrid.demand.domain.entity.JobTitle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobTitleRepository extends JpaRepository<JobTitle, Long> {
    List<JobTitle> findAllByOrderByTitleNameAsc();
}
