package com.talentgrid.auth.service.impl;

import com.talentgrid.auth.dto.response.UserSummaryResponse;
import com.talentgrid.auth.entity.User;
import com.talentgrid.auth.repository.UserRepository;
import com.talentgrid.auth.service.interfaces.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserLookupServiceImpl implements UserLookupService {

  private final UserRepository userRepository;

  @Override
  public UserSummaryResponse getRmgByLocation(String location) {

    User rmg = userRepository
            .findFirstByLocationAndRoles_NameAndEnabledTrue(
                    location,
                    "RESOURCE_MANAGER")
            .orElseThrow(() ->
                    new RuntimeException(
                            "No active resource manager found for location: " + location));

    return UserSummaryResponse.builder()
            .id(rmg.getId())
            .username(rmg.getUsername())
            .email(rmg.getEmail())
            .location(rmg.getLocation())
            .build();
  }
}