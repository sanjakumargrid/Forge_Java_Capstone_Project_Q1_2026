package com.talentgrid.demand.constants;

import java.util.List;

public final class DemandConstants {

  private DemandConstants() {}

    public static final String ENTITY_DEMAND = "DEMAND";

    public static final String SERVICE_NAME = "demand-service";

    public static final String CREATE_DEMAND_ENDPOINT = "/api/demands";

    public static final String DEFAULT_LEVEL = "Senior";

    public static final String DEFAULT_STATUS = "OPEN";

    public static final List<String> DEFAULT_SKILLS =
            List.of("Java", "Spring Boot", "Kafka");

    public static final Long DEFAULT_ENTITY_ID = 101L;

    public static final Long DEFAULT_ACTOR_ID = 1001L;

    public static final String DEFAULT_IP_ADDRESS = "127.0.0.1";

    public static final String DEFAULT_USER_AGENT = "Postman";

    public static final String DEMAND_CREATED_EVENT = "DEMAND_CREATED";

    public static final String DEMAND_APPROVED_EVENT = "DEMAND_APPROVED";
  }
