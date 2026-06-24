package com.talentgrid.interview.interview.integration;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GoogleCalendarResponse {

    private String eventId;

    private String meetLink;
}