package com.talentgrid.candidate.resumeParser.service;

import com.talentgrid.candidate.resumeParser.entity.SaveResumePDF;
import com.talentgrid.candidate.resumeParser.repository.SaveResumePDFRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SaveResumePDFService {


    @Autowired
    SaveResumePDFRepository saveResumePFDRepository;

    public SaveResumePDF savePDF(SaveResumePDF saveResumePDF) {
        return saveResumePFDRepository.save(saveResumePDF);
    }
}

