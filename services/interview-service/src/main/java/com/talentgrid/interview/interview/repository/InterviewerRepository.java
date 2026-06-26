package com.talentgrid.interview.interview.repository;

import com.talentgrid.interview.interview.entity.Interviewer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterviewerRepository extends JpaRepository<Interviewer, Long> {
    
    List<Interviewer> findByLocationAndDomainName(String location, String domainName);
    
    List<Interviewer> findByDomainName(String domainName);

    List<Interviewer> findByLocation(String location);

    List<Interviewer> findByEmployeeId(long EmployeeId);
}
