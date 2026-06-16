package com.talentgrid.candidate.externalCandidate.entity;


import com.talentgrid.candidate.externalCandidate.enums.Social;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.URL;

@Getter
@Setter
@Entity
@Table(
        name = "social_links",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_candidate_social",
                        columnNames = {"candidate_id", "social"}
                )
        }
)
public class SocialLinks {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "social_links_id")
    private Long socialLinksId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private ExternalCandidate candidate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "social", nullable = false)
    private Social social;

    @NotBlank
    @URL
    @Column(name = "links", nullable = false, length = 500)
    private String links;
}