package com.talentgrid.auth.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "account_business_unit_mapping",
    uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "business_unit_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountBusinessUnitMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_unit_id", nullable = false)
    private BusinessUnit businessUnit;
}
