package com.talentgrid.candidate.resumeParser.repository;

import com.talentgrid.candidate.resumeParser.entity.SaveResumePDF;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SaveResumePDFRepository extends JpaRepository<SaveResumePDF,Integer> {
}
