package com.talentgrid.auth.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    /**
     * The User ID of the Project Manager (PM) responsible for this project.
     * PM is a parent role of HM — demands created by a PM are auto-approved.
     */
    @Column(name = "project_manager_id")
    private Long projectManagerId;
}