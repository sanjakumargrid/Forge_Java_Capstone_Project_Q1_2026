package com.talentgrid.auth.repository;

import com.talentgrid.auth.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    @Query("SELECT p FROM Project p LEFT JOIN FETCH p.account WHERE p.projectManagerId = :projectManagerId")
    List<Project> findByProjectManagerId(@Param("projectManagerId") Long projectManagerId);

    List<Project> findByAccountId(Long accountId);

    /**
     * @param accountId owning account id
     * @return projects belonging to the account
     */
    @Query("SELECT p FROM Project p LEFT JOIN FETCH p.account WHERE p.account.id = :accountId")
    List<Project> findByAccountId(@Param("accountId") Long accountId);

    /**
     * @param id project id
     * @return project with its account eagerly loaded
     */
    @Query("SELECT p FROM Project p LEFT JOIN FETCH p.account WHERE p.id = :id")
    Optional<Project> findByIdWithAccount(@Param("id") Long id);

    /**
     * @param accountIds account ids owned by the current account manager
     * @return projects for the given accounts
     */
    @Query("SELECT p FROM Project p LEFT JOIN FETCH p.account WHERE p.account.id IN :accountIds")
    List<Project> findByAccountIdIn(@Param("accountIds") List<Long> accountIds);

    /**
     * @param name      project name
     * @param accountId owning account id
     * @return whether a project with the same name exists under the account
     */
    boolean existsByNameAndAccountId(String name, Long accountId);

    /**
     * @param name      project name
     * @param accountId owning account id
     * @param id        project id to exclude
     * @return whether another project under the account uses the same name
     */
    boolean existsByNameAndAccountIdAndIdNot(String name, Long accountId, Long id);
}