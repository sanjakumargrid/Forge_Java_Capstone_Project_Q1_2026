package com.talentgrid.candidate.externalCandidate.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "address")
@Getter
@Setter
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long addressId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id")
    private ExternalCandidate candidate;

    @NotNull(message = "Street1 cannot be null")
    private String street1;

    @NotNull(message = "Street2 cannot be null")
    private String street2;

    @NotNull(message = "City cannot be null")
    private String city;

    @NotNull(message = "State cannot be null")
    private String state;

    @NotNull(message = "Country cannot be null")
    private String country;

    @NotNull(message = "Zip code cannot be null")
    private Long zipCode;
}
