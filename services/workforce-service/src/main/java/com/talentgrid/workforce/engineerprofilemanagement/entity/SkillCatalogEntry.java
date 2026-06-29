package com.talentgrid.workforce.engineerprofilemanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

/**
 * Read-only view of the shared {@code skills} catalog table owned by demand-service.
 */
@Getter
@Setter
@Entity
@Immutable
@Table(name = "skills")
public class SkillCatalogEntry {

    @Id
    @Column(name = "skill_id")
    private Long skillId;

    @Column(name = "skill_name", nullable = false)
    private String skillName;
}
