package com.talentgrid.candidate.externalCandidate.mapper;

import com.talentgrid.candidate.externalCandidate.dto.*;
import com.talentgrid.candidate.externalCandidate.entity.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ExternalCandidateMapper {

  private ExternalCandidateMapper() {
  }

  public static ExternalCandidate dtoToEntity(ExternalCandidateDto dto) {

    if (dto == null) {
      return null;
    }

    ExternalCandidate candidate = new ExternalCandidate();

    candidate.setFirstName(dto.getFirstName());
    candidate.setLastName(dto.getLastName());
    candidate.setEmail(dto.getEmail());
    candidate.setPhoneNumber(dto.getPhoneNumber());
    candidate.setDateOfBirth(dto.getDateOfBirth());
    candidate.setGender(dto.getGender());
    candidate.setTotalExperienceYears(dto.getTotalExperienceYears());
    candidate.setTotalGapYears(dto.getTotalGapYears());
    candidate.setCurrentCtc(dto.getCurrentCtc());
    candidate.setExpectedCtc(dto.getExpectedCtc());
    candidate.setNoticePeriodDays(dto.getNoticePeriodDays());
    candidate.setWillingToRelocate(dto.getWillingToRelocate());
    candidate.setFreeNotes(dto.getFreeNotes());


    candidate.setAddress(addressDtoToEntity(dto.getAddress(), candidate));
    candidate.setSkills(skillDtoListToEntitySet(dto.getSkills(), candidate));
    candidate.setSocialLinks(socialLinksDtoListToEntitySet(dto.getSocialLinks(), candidate));
    candidate.setExperienceDetails(experienceDtoListToEntitySet(dto.getExperienceDetails(), candidate));
    candidate.setEducationDetails(educationDtoListToEntitySet(dto.getEducationDetails(), candidate));
    candidate.setCertificationDetails(certificationDtoListToEntitySet(dto.getCertificationDetails(), candidate));
    candidate.setResumeDetails(resumeDtoListToEntityList(dto.getResumeDetails(), candidate));

    return candidate;
  }

  public static ExternalCandidateDto entityToDto(ExternalCandidate candidate) {

    if (candidate == null) {
      return null;
    }

    ExternalCandidateDto dto = new ExternalCandidateDto();

    dto.setFirstName(candidate.getFirstName());
    dto.setLastName(candidate.getLastName());
    dto.setEmail(candidate.getEmail());
    dto.setPhoneNumber(candidate.getPhoneNumber());
    dto.setDateOfBirth(candidate.getDateOfBirth());
    dto.setGender(candidate.getGender());
    dto.setTotalExperienceYears(candidate.getTotalExperienceYears());
    dto.setTotalGapYears(candidate.getTotalGapYears());
    dto.setCurrentCtc(candidate.getCurrentCtc());
    dto.setExpectedCtc(candidate.getExpectedCtc());
    dto.setNoticePeriodDays(candidate.getNoticePeriodDays());
    dto.setWillingToRelocate(candidate.getWillingToRelocate());
    dto.setFreeNotes(candidate.getFreeNotes());


    dto.setAddress(addressEntityToDto(candidate.getAddress()));
    dto.setSkills(skillEntitySetToDtoList(candidate.getSkills()));
    dto.setSocialLinks(socialLinksEntitySetToDtoList(candidate.getSocialLinks()));
    dto.setExperienceDetails(experienceEntitySetToDtoList(candidate.getExperienceDetails()));
    dto.setEducationDetails(educationEntitySetToDtoList(candidate.getEducationDetails()));
    dto.setCertificationDetails(certificationEntitySetToDtoList(candidate.getCertificationDetails()));
    dto.setResumeDetails(resumeEntityListToDtoList(candidate.getResumeDetails()));

    return dto;
  }

  public static void copyDtoToExistingEntity(
          ExternalCandidateDto dto,
          ExternalCandidate candidate
  ) {

    if (dto == null || candidate == null) {
      return;
    }

    candidate.setFirstName(dto.getFirstName());
    candidate.setLastName(dto.getLastName());
    candidate.setEmail(dto.getEmail());
    candidate.setPhoneNumber(dto.getPhoneNumber());
    candidate.setDateOfBirth(dto.getDateOfBirth());
    candidate.setGender(dto.getGender());
    candidate.setTotalExperienceYears(dto.getTotalExperienceYears());
    candidate.setTotalGapYears(dto.getTotalGapYears());
    candidate.setCurrentCtc(dto.getCurrentCtc());
    candidate.setExpectedCtc(dto.getExpectedCtc());
    candidate.setNoticePeriodDays(dto.getNoticePeriodDays());
    candidate.setWillingToRelocate(dto.getWillingToRelocate());
    candidate.setFreeNotes(dto.getFreeNotes());


    updateAddress(dto, candidate);
    updateSkills(dto, candidate);
    updateSocialLinks(dto, candidate);
    updateExperienceDetails(dto, candidate);
    updateEducationDetails(dto, candidate);
    updateCertificationDetails(dto, candidate);
    updateResumeDetails(dto, candidate);
  }

  private static void updateAddress(
          ExternalCandidateDto dto,
          ExternalCandidate candidate
  ) {

    if (dto.getAddress() == null) {
      candidate.setAddress(null);
      return;
    }

    Address address = candidate.getAddress();

    if (address == null) {
      address = new Address();
      address.setCandidate(candidate);
      candidate.setAddress(address);
    }

    address.setStreet1(dto.getAddress().getStreet1());
    address.setStreet2(dto.getAddress().getStreet2());
    address.setCity(dto.getAddress().getCity());
    address.setState(dto.getAddress().getState());
    address.setCountry(dto.getAddress().getCountry());
    address.setZipCode(dto.getAddress().getZipCode());
  }

  private static void updateSkills(
          ExternalCandidateDto dto,
          ExternalCandidate candidate
  ) {

    if (dto.getSkills() == null || dto.getSkills().isEmpty()) {
      return;
    }

    dto.getSkills().forEach(newSkillDto -> {

      boolean alreadyExists = candidate.getSkills()
              .stream()
              .anyMatch(existingSkill ->
                      equalsIgnoreCase(
                              existingSkill.getSkillName(),
                              newSkillDto.getSkillName()
                      )
              );

      if (!alreadyExists) {
        SkillDetail skill = new SkillDetail();
        skill.setCandidate(candidate);
        skill.setSkillName(newSkillDto.getSkillName().trim());

        candidate.getSkills().add(skill);
      }
    });
  }

  private static void updateSocialLinks(
          ExternalCandidateDto dto,
          ExternalCandidate candidate
  ) {

    if (dto.getSocialLinks() == null || dto.getSocialLinks().isEmpty()) {
      return;
    }

    dto.getSocialLinks().forEach(newSocialDto -> {

      SocialLinks existingSocialLink = candidate.getSocialLinks()
              .stream()
              .filter(existingLink ->
                      existingLink.getSocial().equals(newSocialDto.getSocial())
              )
              .findFirst()
              .orElse(null);

      if (existingSocialLink != null) {
        existingSocialLink.setLinks(newSocialDto.getLinks());
      } else {
        SocialLinks socialLinks = new SocialLinks();
        socialLinks.setCandidate(candidate);
        socialLinks.setSocial(newSocialDto.getSocial());
        socialLinks.setLinks(newSocialDto.getLinks());

        candidate.getSocialLinks().add(socialLinks);
      }
    });
  }

  private static void updateExperienceDetails(
          ExternalCandidateDto dto,
          ExternalCandidate candidate
  ) {

    if (dto.getExperienceDetails() == null || dto.getExperienceDetails().isEmpty()) {
      return;
    }

    dto.getExperienceDetails().forEach(newExperienceDto -> {

      boolean alreadyExists = candidate.getExperienceDetails()
              .stream()
              .anyMatch(existingExperience ->
                      equalsIgnoreCase(existingExperience.getCompanyName(), newExperienceDto.getCompanyName())
                              && equalsIgnoreCase(existingExperience.getJobTitle(), newExperienceDto.getJobTitle())
                              && equalsIgnoreCase(existingExperience.getDesignation(), newExperienceDto.getDesignation())
                              && equalsObject(existingExperience.getStartDate(), newExperienceDto.getStartDate())
                              && equalsObject(existingExperience.getEndDate(), newExperienceDto.getEndDate())
              );

      if (!alreadyExists) {
        ExperienceDetail experience = new ExperienceDetail();

        experience.setCandidate(candidate);
        experience.setCompanyName(newExperienceDto.getCompanyName());
        experience.setJobTitle(newExperienceDto.getJobTitle());
        experience.setStartDate(newExperienceDto.getStartDate());
        experience.setEndDate(newExperienceDto.getEndDate());
        experience.setDesignation(newExperienceDto.getDesignation());

        candidate.getExperienceDetails().add(experience);
      }
    });
  }

  private static void updateEducationDetails(
          ExternalCandidateDto dto,
          ExternalCandidate candidate
  ) {

    if (dto.getEducationDetails() == null || dto.getEducationDetails().isEmpty()) {
      return;
    }


    dto.getEducationDetails().forEach(newEducationDto -> {

      boolean alreadyExists = candidate.getEducationDetails()
              .stream()
              .anyMatch(existingEducation ->
                      equalsIgnoreCase(existingEducation.getDegree(), newEducationDto.getDegree())
                              && equalsIgnoreCase(existingEducation.getSpecialization(), newEducationDto.getSpecialization())
                              && equalsIgnoreCase(existingEducation.getInstitutionName(), newEducationDto.getInstitutionName())
                              && equalsObject(existingEducation.getStartYear(), newEducationDto.getStartYear())
                              && equalsObject(existingEducation.getEndYear(), newEducationDto.getEndYear())
              );

      if (!alreadyExists) {
        EducationDetail education = new EducationDetail();

        education.setCandidate(candidate);
        education.setDegree(newEducationDto.getDegree());
        education.setSpecialization(newEducationDto.getSpecialization());
        education.setInstitutionName(newEducationDto.getInstitutionName());
        education.setStartYear(newEducationDto.getStartYear());
        education.setEndYear(newEducationDto.getEndYear());
        education.setPercentage(newEducationDto.getPercentage());

        candidate.getEducationDetails().add(education);
      }
    });
  }

  private static void updateCertificationDetails(
          ExternalCandidateDto dto,
          ExternalCandidate candidate
  ) {

    if (dto.getCertificationDetails() == null || dto.getCertificationDetails().isEmpty()) {
      return;
    }

    dto.getCertificationDetails().forEach(newCertificationDto -> {

      boolean alreadyExists = candidate.getCertificationDetails()
              .stream()
              .anyMatch(existingCertification ->
                      equalsIgnoreCase(existingCertification.getCertificateName(), newCertificationDto.getCertificateName())
                              && equalsIgnoreCase(existingCertification.getIssuingOrganization(), newCertificationDto.getIssuingOrganization())
                              && equalsObject(existingCertification.getIssuedDate(), newCertificationDto.getIssuedDate())
              );

      if (!alreadyExists) {
        CertificationDetail certification = new CertificationDetail();

        certification.setCandidate(candidate);
        certification.setCertificateName(newCertificationDto.getCertificateName());
        certification.setIssuingOrganization(newCertificationDto.getIssuingOrganization());
        certification.setIssuedDate(newCertificationDto.getIssuedDate());
        certification.setCertificateFilePath(newCertificationDto.getCertificateFilePath());

        candidate.getCertificationDetails().add(certification);
      }
    });
  }

  private static void updateResumeDetails(
          ExternalCandidateDto dto,
          ExternalCandidate candidate
  ) {

    if (dto.getResumeDetails() == null || dto.getResumeDetails().isEmpty()) {
      return;
    }

    dto.getResumeDetails().forEach(newResumeDto -> {

      boolean alreadyExists = candidate.getResumeDetails()
              .stream()
              .anyMatch(existingResume ->
                      equalsIgnoreCase(existingResume.getResumeFilePath(), newResumeDto.getResumeFilePath())
                              && equalsIgnoreCase(existingResume.getResumeOriginalFilename(), newResumeDto.getResumeOriginalFilename())
              );

      if (!alreadyExists) {
        ResumeDetail resume = new ResumeDetail();

        resume.setCandidate(candidate);
        resume.setResumeFilePath(newResumeDto.getResumeFilePath());
        resume.setResumeOriginalFilename(newResumeDto.getResumeOriginalFilename());

        candidate.getResumeDetails().add(resume);
      }
    });
  }

  private static Address addressDtoToEntity(
          AddressDto dto,
          ExternalCandidate candidate
  ) {

    if (dto == null) {
      return null;
    }

    Address address = new Address();

    address.setCandidate(candidate);
    address.setStreet1(dto.getStreet1());
    address.setStreet2(dto.getStreet2());
    address.setCity(dto.getCity());
    address.setState(dto.getState());
    address.setCountry(dto.getCountry());
    address.setZipCode(dto.getZipCode());

    return address;
  }

  private static AddressDto addressEntityToDto(Address address) {

    if (address == null) {
      return null;
    }

    AddressDto dto = new AddressDto();

    dto.setStreet1(address.getStreet1());
    dto.setStreet2(address.getStreet2());
    dto.setCity(address.getCity());
    dto.setState(address.getState());
    dto.setCountry(address.getCountry());
    dto.setZipCode(address.getZipCode());

    return dto;
  }

  private static Set<SkillDetail> skillDtoListToEntitySet(
          List<SkillDetailDto> dtoList,
          ExternalCandidate candidate
  ) {

    if (dtoList == null) {
      return new HashSet<>();
    }

    return dtoList.stream()
            .map(dto -> {
              SkillDetail skill = new SkillDetail();
              skill.setCandidate(candidate);
              skill.setSkillName(dto.getSkillName().trim());
              return skill;
            })
            .collect(Collectors.toSet());
  }

  private static List<SkillDetailDto> skillEntitySetToDtoList(
          Set<SkillDetail> skills
  ) {

    if (skills == null) {
      return new ArrayList<>();
    }

    return skills.stream()
            .map(skill -> {
              SkillDetailDto dto = new SkillDetailDto();
              dto.setSkillName(skill.getSkillName());
              return dto;
            })
            .collect(Collectors.toList());
  }

  private static Set<SocialLinks> socialLinksDtoListToEntitySet(
          List<SocialLinksDto> dtoList,
          ExternalCandidate candidate
  ) {

    if (dtoList == null) {
      return new HashSet<>();
    }

    return dtoList.stream()
            .map(dto -> {
              SocialLinks socialLinks = new SocialLinks();
              socialLinks.setCandidate(candidate);
              socialLinks.setSocial(dto.getSocial());
              socialLinks.setLinks(dto.getLinks());
              return socialLinks;
            })
            .collect(Collectors.toSet());
  }

  private static List<SocialLinksDto> socialLinksEntitySetToDtoList(
          Set<SocialLinks> socialLinks
  ) {

    if (socialLinks == null) {
      return new ArrayList<>();
    }

    return socialLinks.stream()
            .map(entity -> {
              SocialLinksDto dto = new SocialLinksDto();
              dto.setSocial(entity.getSocial());
              dto.setLinks(entity.getLinks());
              return dto;
            })
            .collect(Collectors.toList());
  }

  private static Set<ExperienceDetail> experienceDtoListToEntitySet(
          List<ExperienceDetailDto> dtoList,
          ExternalCandidate candidate
  ) {

    if (dtoList == null) {
      return new HashSet<>();
    }

    return dtoList.stream()
            .map(dto -> {
              ExperienceDetail experience = new ExperienceDetail();
              experience.setCandidate(candidate);
              experience.setCompanyName(dto.getCompanyName());
              experience.setJobTitle(dto.getJobTitle());
              experience.setStartDate(dto.getStartDate());
              experience.setEndDate(dto.getEndDate());
              experience.setDesignation(dto.getDesignation());
              return experience;
            })
            .collect(Collectors.toSet());
  }

  private static List<ExperienceDetailDto> experienceEntitySetToDtoList(
          Set<ExperienceDetail> experienceDetails
  ) {

    if (experienceDetails == null) {
      return new ArrayList<>();
    }

    return experienceDetails.stream()
            .map(entity -> {
              ExperienceDetailDto dto = new ExperienceDetailDto();
              dto.setCompanyName(entity.getCompanyName());
              dto.setJobTitle(entity.getJobTitle());
              dto.setStartDate(entity.getStartDate());
              dto.setEndDate(entity.getEndDate());
              dto.setDesignation(entity.getDesignation());
              return dto;
            })
            .collect(Collectors.toList());
  }

  private static Set<EducationDetail> educationDtoListToEntitySet(
          List<EducationDetailDto> dtoList,
          ExternalCandidate candidate
  ) {

    if (dtoList == null) {
      return new HashSet<>();
    }


    return dtoList.stream()
            .map(dto -> {
              EducationDetail education = new EducationDetail();
              education.setCandidate(candidate);
              education.setDegree(dto.getDegree());
              education.setSpecialization(dto.getSpecialization());
              education.setInstitutionName(dto.getInstitutionName());
              education.setStartYear(dto.getStartYear());
              education.setEndYear(dto.getEndYear());
              education.setPercentage(dto.getPercentage());
              return education;
            })
            .collect(Collectors.toSet());
  }

  private static List<EducationDetailDto> educationEntitySetToDtoList(
          Set<EducationDetail> educationDetails
  ) {

    if (educationDetails == null) {
      return new ArrayList<>();
    }

    return educationDetails.stream()
            .map(entity -> {
              EducationDetailDto dto = new EducationDetailDto();
              dto.setDegree(entity.getDegree());
              dto.setSpecialization(entity.getSpecialization());
              dto.setInstitutionName(entity.getInstitutionName());
              dto.setStartYear(entity.getStartYear());
              dto.setEndYear(entity.getEndYear());
              dto.setPercentage(entity.getPercentage());
              return dto;
            })
            .collect(Collectors.toList());
  }

  private static Set<CertificationDetail> certificationDtoListToEntitySet(
          List<CertificationDetailDto> dtoList,
          ExternalCandidate candidate
  ) {

    if (dtoList == null) {
      return new HashSet<>();
    }

    return dtoList.stream()
            .map(dto -> {
              CertificationDetail certification = new CertificationDetail();
              certification.setCandidate(candidate);
              certification.setCertificateName(dto.getCertificateName());
              certification.setIssuingOrganization(dto.getIssuingOrganization());
              certification.setIssuedDate(dto.getIssuedDate());
              certification.setCertificateFilePath(dto.getCertificateFilePath());
              return certification;
            })
            .collect(Collectors.toSet());
  }

  private static List<CertificationDetailDto> certificationEntitySetToDtoList(
          Set<CertificationDetail> certificationDetails
  ) {

    if (certificationDetails == null) {
      return new ArrayList<>();
    }

    return certificationDetails.stream()
            .map(entity -> {
              CertificationDetailDto dto = new CertificationDetailDto();
              dto.setCertificateName(entity.getCertificateName());
              dto.setIssuingOrganization(entity.getIssuingOrganization());
              dto.setIssuedDate(entity.getIssuedDate());
              dto.setCertificateFilePath(entity.getCertificateFilePath());
              return dto;
            })
            .collect(Collectors.toList());
  }

  private static List<ResumeDetail> resumeDtoListToEntityList(
          List<ResumeDetailDto> dtoList,
          ExternalCandidate candidate
  ) {

    if (dtoList == null) {
      return new ArrayList<>();
    }

    return dtoList.stream()
            .map(dto -> {
              ResumeDetail resume = new ResumeDetail();
              resume.setCandidate(candidate);
              resume.setResumeFilePath(dto.getResumeFilePath());
              resume.setResumeOriginalFilename(dto.getResumeOriginalFilename());
              return resume;
            })
            .collect(Collectors.toList());
  }

  private static List<ResumeDetailDto> resumeEntityListToDtoList(
          List<ResumeDetail> resumeDetails
  ) {

    if (resumeDetails == null) {
      return new ArrayList<>();
    }

    return resumeDetails.stream()
            .map(entity -> {
              ResumeDetailDto dto = new ResumeDetailDto();
              dto.setResumeFilePath(entity.getResumeFilePath());
              dto.setResumeOriginalFilename(entity.getResumeOriginalFilename());
              return dto;
            })
            .collect(Collectors.toList());
  }

  private static boolean equalsIgnoreCase(String value1, String value2) {

    if (value1 == null && value2 == null) {
      return true;
    }

    if (value1 == null || value2 == null) {
      return false;
    }

    return value1.trim().equalsIgnoreCase(value2.trim());
  }

  private static boolean equalsObject(Object value1, Object value2) {
    return value1 == null ? value2 == null : value1.equals(value2);
  }

}