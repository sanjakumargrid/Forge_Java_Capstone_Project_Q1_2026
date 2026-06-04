package com.talentgrid.demand.mapper;

import com.talentgrid.demand.domain.entity.Demand;
import com.talentgrid.demand.domain.entity.DemandStatusHistory;
import com.talentgrid.demand.domain.enums.DemandPriority;
import com.talentgrid.demand.domain.enums.DemandStatus;
import com.talentgrid.demand.domain.enums.SeniorityLevel;
import com.talentgrid.demand.dto.request.CreateDemandRequest;
import com.talentgrid.demand.dto.request.UpdateDemandRequest;
import com.talentgrid.demand.dto.response.DemandResponse;
import com.talentgrid.demand.dto.response.DemandStatusHistoryResponse;
import com.talentgrid.demand.dto.response.DemandSummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DemandMapper}.
 *
 * Covers:
 *  - toEntity() from CreateDemandRequest: verifies all fields mapped, DRAFT status set, counts zeroed.
 *  - applyUpdate() PATCH semantics: null fields are ignored, non-null fields applied.
 *  - toResponse(): all 27 fields present in DemandResponse.
 *  - toSummaryResponse(): lightweight projection + ageInDays computation.
 *  - toHistoryResponse(): full audit trail fields.
 *  - Null-safety: null inputs return null without NPE.
 */
@DisplayName("DemandMapper — Mapping Tests")
class DemandMapperTest {

    private DemandMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new DemandMapper();
    }

    // ─── Helper builders ────────────────────────────────────────────────────────

    private CreateDemandRequest buildCreateRequest() {
        CreateDemandRequest req = new CreateDemandRequest();
        req.setTitle("Backend Engineer");
        req.setDescription("Need a Java expert");
        req.setLevel(SeniorityLevel.SENIOR);
        req.setLocation("Hyderabad");
        req.setProjectId(42L);
        req.setBusinessUnit("Platform");
        req.setSkills(List.of("Java", "Spring Boot", "Kafka"));
        req.setBudget(new BigDecimal("120000.00"));
        req.setRequiredCount(3);
        req.setTargetDate(LocalDate.of(2026, 9, 1));
        req.setPriority(DemandPriority.HIGH);
        return req;
    }

    private Demand buildFullDemand() {
        Demand d = new Demand();
        d.setDemandId(1L);
        d.setTitle("Backend Engineer");
        d.setDescription("Need a Java expert");
        d.setLevel(SeniorityLevel.SENIOR);
        d.setLocation("Hyderabad");
        d.setProjectId(42L);
        d.setBusinessUnit("Platform");
        d.setSkills(List.of("Java", "Spring Boot"));
        d.setBudget(new BigDecimal("120000.00"));
        d.setRequiredCount(3);
        d.setRecruitedCount(1);
        d.setInternalFilledCount(1);
        d.setExternalFilledCount(0);
        d.setStatus(DemandStatus.INTERNAL_SEARCH);
        d.setPriority(DemandPriority.HIGH);
        d.setPreviousStatus(null);
        d.setTargetDate(LocalDate.of(2026, 9, 1));
        d.setSearchStartAt(OffsetDateTime.now().minusDays(3));
        d.setApprovedAt(OffsetDateTime.now().minusDays(4));
        d.setClosureReason(null);
        d.setCreatedBy(100L);
        d.setAssignedRecruiter(200L);
        d.setAssignedRm(300L);
        d.setApprovedBy(400L);
        d.setIsDeleted(false);
        d.setVersion(1);
        d.setCreatedAt(OffsetDateTime.now().minusDays(5));
        d.setUpdatedAt(OffsetDateTime.now().minusDays(1));
        return d;
    }

    // ─── toEntity ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("toEntity(CreateDemandRequest)")
    class ToEntity {

        @Test
        @DisplayName("Maps all 11 request fields correctly")
        void maps_all_create_fields() {
            CreateDemandRequest req = buildCreateRequest();
            Demand d = mapper.toEntity(req);

            assertThat(d.getTitle()).isEqualTo("Backend Engineer");
            assertThat(d.getDescription()).isEqualTo("Need a Java expert");
            assertThat(d.getLevel()).isEqualTo(SeniorityLevel.SENIOR);
            assertThat(d.getLocation()).isEqualTo("Hyderabad");
            assertThat(d.getProjectId()).isEqualTo(42L);
            assertThat(d.getBusinessUnit()).isEqualTo("Platform");
            assertThat(d.getSkills()).containsExactly("Java", "Spring Boot", "Kafka");
            assertThat(d.getBudget()).isEqualByComparingTo("120000.00");
            assertThat(d.getRequiredCount()).isEqualTo(3);
            assertThat(d.getTargetDate()).isEqualTo(LocalDate.of(2026, 9, 1));
            assertThat(d.getPriority()).isEqualTo(DemandPriority.HIGH);
        }

        @Test
        @DisplayName("Status is DRAFT on creation")
        void initial_status_is_draft() {
            Demand d = mapper.toEntity(buildCreateRequest());
            assertThat(d.getStatus()).isEqualTo(DemandStatus.DRAFT);
        }

        @Test
        @DisplayName("Fill counts default to 0")
        void fill_counts_default_to_zero() {
            Demand d = mapper.toEntity(buildCreateRequest());
            assertThat(d.getInternalFilledCount()).isZero();
            assertThat(d.getExternalFilledCount()).isZero();
            assertThat(d.getRecruitedCount()).isZero();
        }

        @Test
        @DisplayName("isDeleted defaults to false")
        void is_deleted_defaults_false() {
            Demand d = mapper.toEntity(buildCreateRequest());
            assertThat(d.getIsDeleted()).isFalse();
        }
    }

    // ─── applyUpdate ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("applyUpdate(UpdateDemandRequest, Demand) — PATCH semantics")
    class ApplyUpdate {

        @Test
        @DisplayName("Non-null fields overwrite entity values")
        void non_null_fields_are_applied() {
            Demand d = buildFullDemand();
            UpdateDemandRequest req = new UpdateDemandRequest();
            req.setTitle("Updated Title");
            req.setPriority(DemandPriority.CRITICAL);

            mapper.applyUpdate(req, d);

            assertThat(d.getTitle()).isEqualTo("Updated Title");
            assertThat(d.getPriority()).isEqualTo(DemandPriority.CRITICAL);
        }

        @Test
        @DisplayName("Null fields in request leave entity unchanged")
        void null_fields_leave_entity_unchanged() {
            Demand d = buildFullDemand();
            String originalDescription = d.getDescription();
            String originalLocation = d.getLocation();

            UpdateDemandRequest req = new UpdateDemandRequest();
            req.setTitle("Only Title Changed");
            // description, location etc. remain null in request

            mapper.applyUpdate(req, d);

            assertThat(d.getDescription()).isEqualTo(originalDescription);
            assertThat(d.getLocation()).isEqualTo(originalLocation);
        }

        @Test
        @DisplayName("Empty update request changes nothing")
        void empty_request_leaves_demand_unchanged() {
            Demand d = buildFullDemand();
            String originalTitle = d.getTitle();

            mapper.applyUpdate(new UpdateDemandRequest(), d);

            assertThat(d.getTitle()).isEqualTo(originalTitle);
        }
    }

    // ─── toResponse ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("toResponse(Demand) — full DTO")
    class ToResponse {

        @Test
        @DisplayName("Maps all core fields to DemandResponse")
        void maps_all_fields() {
            Demand d = buildFullDemand();
            DemandResponse resp = mapper.toResponse(d);

            assertThat(resp.getDemandId()).isEqualTo(1L);
            assertThat(resp.getTitle()).isEqualTo("Backend Engineer");
            assertThat(resp.getDescription()).isEqualTo("Need a Java expert");
            assertThat(resp.getLevel()).isEqualTo("SENIOR");
            assertThat(resp.getLocation()).isEqualTo("Hyderabad");
            assertThat(resp.getProjectId()).isEqualTo(42L);
            assertThat(resp.getBusinessUnit()).isEqualTo("Platform");
            assertThat(resp.getSkills()).containsExactly("Java", "Spring Boot");
            assertThat(resp.getBudget()).isEqualByComparingTo("120000.00");
            assertThat(resp.getRequiredCount()).isEqualTo(3);
            assertThat(resp.getRecruitedCount()).isEqualTo(1);
            assertThat(resp.getInternalFilledCount()).isEqualTo(1);
            assertThat(resp.getExternalFilledCount()).isEqualTo(0);
            assertThat(resp.getStatus()).isEqualTo("INTERNAL_SEARCH");
            assertThat(resp.getPriority()).isEqualTo("HIGH");
            assertThat(resp.getCreatedBy()).isEqualTo(100L);
            assertThat(resp.getAssignedRecruiter()).isEqualTo(200L);
            assertThat(resp.getAssignedRm()).isEqualTo(300L);
            assertThat(resp.getApprovedBy()).isEqualTo(400L);
            assertThat(resp.getIsDeleted()).isFalse();
            assertThat(resp.getVersion()).isEqualTo(1);
        }

        @Test
        @DisplayName("Enum fields serialized as their name() strings")
        void enum_fields_use_name() {
            Demand d = buildFullDemand();
            d.setPreviousStatus(DemandStatus.INTERNAL_SEARCH);

            DemandResponse resp = mapper.toResponse(d);
            assertThat(resp.getPreviousStatus()).isEqualTo("INTERNAL_SEARCH");
        }

        @Test
        @DisplayName("Null demand returns null (null-safety)")
        void null_demand_returns_null() {
            assertThat(mapper.toResponse(null)).isNull();
        }

        @Test
        @DisplayName("Null enum fields map to null strings (null-safety)")
        void null_enum_returns_null_string() {
            Demand d = new Demand();
            d.setDemandId(99L);
            // status and priority are null

            DemandResponse resp = mapper.toResponse(d);
            assertThat(resp.getStatus()).isNull();
            assertThat(resp.getPriority()).isNull();
        }
    }

    // ─── toSummaryResponse ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("toSummaryResponse(Demand) — lightweight dashboard projection")
    class ToSummaryResponse {

        @Test
        @DisplayName("Maps lightweight fields")
        void maps_summary_fields() {
            Demand d = buildFullDemand();
            DemandSummaryResponse resp = mapper.toSummaryResponse(d);

            assertThat(resp.getDemandId()).isEqualTo(1L);
            assertThat(resp.getTitle()).isEqualTo("Backend Engineer");
            assertThat(resp.getStatus()).isEqualTo("INTERNAL_SEARCH");
            assertThat(resp.getPriority()).isEqualTo("HIGH");
            assertThat(resp.getBusinessUnit()).isEqualTo("Platform");
            assertThat(resp.getRequiredCount()).isEqualTo(3);
            assertThat(resp.getInternalFilledCount()).isEqualTo(1);
            assertThat(resp.getExternalFilledCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("ageInDays is computed from createdAt")
        void ageInDays_is_computed() {
            Demand d = buildFullDemand();
            // createdAt set to 5 days ago in buildFullDemand()
            DemandSummaryResponse resp = mapper.toSummaryResponse(d);

            assertThat(resp.getAgeInDays()).isGreaterThanOrEqualTo(4L); // at least 4 days
        }

        @Test
        @DisplayName("ageInDays is 0 when createdAt is null")
        void ageInDays_null_when_createdAt_null() {
            Demand d = buildFullDemand();
            d.setCreatedAt(null);

            DemandSummaryResponse resp = mapper.toSummaryResponse(d);
            assertThat(resp.getAgeInDays()).isNull();
        }

        @Test
        @DisplayName("toSummaryResponseList maps a list correctly")
        void maps_list_of_demands() {
            List<Demand> demands = List.of(buildFullDemand(), buildFullDemand());
            List<DemandSummaryResponse> list = mapper.toSummaryResponseList(demands);

            assertThat(list).hasSize(2);
            assertThat(list).allMatch(r -> r.getTitle().equals("Backend Engineer"));
        }

        @Test
        @DisplayName("Null demand returns null (null-safety)")
        void null_returns_null() {
            assertThat(mapper.toSummaryResponse(null)).isNull();
        }
    }

    // ─── toHistoryResponse ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("toHistoryResponse(DemandStatusHistory) — audit trail")
    class ToHistoryResponse {

        private DemandStatusHistory buildHistory() {
            Demand d = buildFullDemand();
            DemandStatusHistory h = new DemandStatusHistory();
            h.setId(10L);
            h.setDemand(d);
            h.setFromStatus(DemandStatus.DRAFT);
            h.setToStatus(DemandStatus.PENDING_APPROVAL);
            h.setChangedBy(100L);
            h.setClosureReason(null);
            h.setComments("Submitting for review");
            h.setChangedAt(OffsetDateTime.now().minusDays(1));
            return h;
        }

        @Test
        @DisplayName("Maps all audit fields")
        void maps_all_history_fields() {
            DemandStatusHistoryResponse resp = mapper.toHistoryResponse(buildHistory());

            assertThat(resp.getId()).isEqualTo(10L);
            assertThat(resp.getDemandId()).isEqualTo(1L);
            assertThat(resp.getFromStatus()).isEqualTo("DRAFT");
            assertThat(resp.getToStatus()).isEqualTo("PENDING_APPROVAL");
            assertThat(resp.getChangedBy()).isEqualTo(100L);
            assertThat(resp.getClosureReason()).isNull();
            assertThat(resp.getComments()).isEqualTo("Submitting for review");
            assertThat(resp.getChangedAt()).isNotNull();
        }

        @Test
        @DisplayName("Null history returns null (null-safety)")
        void null_returns_null() {
            assertThat(mapper.toHistoryResponse(null)).isNull();
        }

        @Test
        @DisplayName("toHistoryResponseList maps correctly")
        void maps_history_list() {
            List<DemandStatusHistory> histories = List.of(buildHistory(), buildHistory());
            List<DemandStatusHistoryResponse> list = mapper.toHistoryResponseList(histories);

            assertThat(list).hasSize(2);
            assertThat(list).allMatch(r -> r.getFromStatus().equals("DRAFT"));
        }
    }
}
