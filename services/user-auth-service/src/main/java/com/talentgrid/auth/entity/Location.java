package com.talentgrid.auth.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "locations",
    uniqueConstraints = @UniqueConstraint(columnNames = {"country", "location_name"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String country;

    @Column(name = "location_name", nullable = false, length = 100)
    private String locationName;
}
