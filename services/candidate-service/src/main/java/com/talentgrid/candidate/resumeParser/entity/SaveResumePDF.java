package com.talentgrid.candidate.resumeParser.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaveResumePDF {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;
    @Lob
    @Column(columnDefinition = "LONGBLOB")
    byte [] data;
    Integer userId;
}
