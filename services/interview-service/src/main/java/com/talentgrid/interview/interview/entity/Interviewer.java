package com.talentgrid.interview.interview.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "interviewer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Interviewer {

    @Id
    @Column(name = "employee_id")
    private Long employeeId;

    @Column(name = "domain_name", nullable = false)
    private String domainName;

    @Column(name = "location")
    private String location;

    @Column(name = "grade")
    private String grade;

}

