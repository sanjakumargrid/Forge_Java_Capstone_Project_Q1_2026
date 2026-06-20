package com.talentgrid.demand.domain.entity;

import jakarta.persistence.*;

/**
 * JPA entity representing a job title in the job_titles lookup table.
 */
@Entity
@Table(name = "job_titles")
public class JobTitle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "job_title_id")
    private Long jobTitleId;

    @Column(name = "title_name", nullable = false, unique = true, length = 255)
    private String titleName;

    public Long getJobTitleId() {
        return jobTitleId;
    }

    public void setJobTitleId(Long jobTitleId) {
        this.jobTitleId = jobTitleId;
    }

    public String getTitleName() {
        return titleName;
    }

    public void setTitleName(String titleName) {
        this.titleName = titleName;
    }
}
