package com.talentgrid.auth.repository;

import com.talentgrid.auth.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    @Query("SELECT p FROM Project p LEFT JOIN FETCH p.account WHERE p.projectManagerId = :projectManagerId")
    List<Project> findByProjectManagerId(@Param("projectManagerId") Long projectManagerId);
}