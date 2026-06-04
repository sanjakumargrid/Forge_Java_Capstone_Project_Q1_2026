package com.talentgrid.workforce.engineerprofilemanagement.entity;

import com.talentgrid.workforce.engineerprofilemanagement.enums.ContractType;
import com.talentgrid.workforce.engineerprofilemanagement.enums.HrisSyncStatus;
import com.talentgrid.workforce.engineerprofilemanagement.enums.Level;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "internal_employees")
public class InternalEmployee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false, unique = true, length = 50)
    private String employeeId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Level level;

    @Column(name = "skills", columnDefinition = "text[]")
    private String[] skills;

    @Column(name = "skills_vector", columnDefinition = "vector")
    private String skillsVector;

    @Column(name = "current_project", length = 150)
    private String currentProject;

    @Column(name = "availability_date")
    private LocalDate availabilityDate;

    @Column(length = 100)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "contract_type", length = 20)
    private ContractType contractType;

    @Column(name = "utilisation_pct")
    private Integer utilisationPct;

    @Column(name = "manager_id")
    private Long managerId;

    @Column(name = "last_embedded_at")
    private LocalDateTime lastEmbeddedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "hris_sync_status", length = 20)
    private HrisSyncStatus hrisSyncStatus;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted")
    private Boolean isDeleted;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;


}
