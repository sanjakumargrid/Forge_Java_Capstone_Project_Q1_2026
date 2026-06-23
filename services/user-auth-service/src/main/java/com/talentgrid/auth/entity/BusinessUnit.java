package com.talentgrid.auth.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "business_units")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_unit_name", nullable = false, unique = true, length = 100)
    private String businessUnitName;
}
