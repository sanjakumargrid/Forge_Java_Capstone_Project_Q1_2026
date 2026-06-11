package com.talentgrid.workforce.engineerprofilemanagement.service.impl;

import com.talentgrid.workforce.engineerprofilemanagement.dto.EmployeeImportedPayload;
import com.talentgrid.workforce.engineerprofilemanagement.dto.HrisImportCommitResponse;
import com.talentgrid.workforce.engineerprofilemanagement.dto.HrisImportResponse;
import com.talentgrid.workforce.engineerprofilemanagement.dto.HrisImportRowError;
import com.talentgrid.workforce.engineerprofilemanagement.dto.HrisImportValidationResponse;
import com.talentgrid.workforce.engineerprofilemanagement.entity.InternalEmployee;
import com.talentgrid.workforce.engineerprofilemanagement.enums.ContractType;
import com.talentgrid.workforce.engineerprofilemanagement.enums.HrisSyncStatus;
import com.talentgrid.workforce.engineerprofilemanagement.enums.Level;
import com.talentgrid.workforce.engineerprofilemanagement.exception.HrisImportValidationException;
import com.talentgrid.workforce.engineerprofilemanagement.repository.InternalEmployeeRepository;
import com.talentgrid.workforce.engineerprofilemanagement.service.HrisCsvParser;
import com.talentgrid.workforce.engineerprofilemanagement.service.HrisImportPreparedRow;
import com.talentgrid.workforce.engineerprofilemanagement.service.HrisImportService;
import com.talentgrid.workforce.kafka.producer.WorkforceKafkaProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class HrisImportServiceImpl implements HrisImportService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
            Pattern.CASE_INSENSITIVE
    );

    private final HrisCsvParser csvParser;
    private final InternalEmployeeRepository repository;
    private final WorkforceKafkaProducer workforceKafkaProducer;
    private final TransactionTemplate transactionTemplate;

    private final int maxImportRows;
    private final int processBatchSize;

    public HrisImportServiceImpl(
            HrisCsvParser csvParser,
            InternalEmployeeRepository repository,
            WorkforceKafkaProducer workforceKafkaProducer,
            PlatformTransactionManager transactionManager,
            @Value("${hris.import.max-rows:1000}") int maxImportRows,
            @Value("${hris.import.process-batch-size:100}") int processBatchSize) {
        this.csvParser = csvParser;
        this.repository = repository;
        this.workforceKafkaProducer = workforceKafkaProducer;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.maxImportRows = maxImportRows;
        this.processBatchSize = Math.max(1, Math.min(processBatchSize, maxImportRows));
    }

    @Override
    public HrisImportResponse importCsv(MultipartFile file, boolean commit, String correlationId) {
        if (file == null || file.isEmpty()) {
            HrisImportValidationResponse validation = toResponse(0,
                    List.of(new HrisImportRowError(0, "file", "CSV file is required and must not be empty")));
            return HrisImportResponse.builder()
                    .commitRequested(commit)
                    .validation(validation)
                    .commitResult(null)
                    .build();
        }

        final byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read CSV: " + e.getMessage(), e);
        }

        ValidationOutcome outcome;
        try {
            outcome = validateContent(bytes, file.getOriginalFilename(), true);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to parse CSV: " + e.getMessage(), e);
        }

        HrisImportValidationResponse validation = outcome.response();

        if (!commit) {
            return HrisImportResponse.builder()
                    .commitRequested(false)
                    .validation(validation)
                    .commitResult(null)
                    .build();
        }

        if (!validation.isCanCommit()) {
            throw new HrisImportValidationException(validation);
        }

        List<PersistedRow> persisted = transactionTemplate.execute(status -> persistAll(outcome.rows()));

        int events = 0;
        if (persisted != null) {
            for (PersistedRow pr : persisted) {
                workforceKafkaProducer.publishEmployeeImported(
                        EmployeeImportedPayload.builder()
                                .employeeId(pr.employeeId())
                                .email(pr.email())
                                .action(pr.created() ? "CREATED" : "UPDATED")
                                .importedAt(Instant.now())
                                .build(),
                        correlationId
                );
                events++;
            }
        }

        int created = persisted == null ? 0 : (int) persisted.stream().filter(PersistedRow::created).count();
        int updated = persisted == null ? 0 : (int) persisted.stream().filter(pr -> !pr.created()).count();
        int total = outcome.rows().size();

        log.info("[HRIS-IMPORT] Committed {} rows | created={} | updated={} | kafkaEvents={}",
                total, created, updated, events);

        HrisImportCommitResponse commitResult = HrisImportCommitResponse.builder()
                .totalRows(total)
                .created(created)
                .updated(updated)
                .eventsPublished(events)
                .committedAt(Instant.now())
                .build();

        return HrisImportResponse.builder()
                .commitRequested(true)
                .validation(validation)
                .commitResult(commitResult)
                .build();
    }

    private ValidationOutcome validateContent(byte[] bytes, String originalFilename, boolean checkDatabase)
            throws IOException {

        if (bytes.length == 0) {
            List<HrisImportRowError> errors = List.of(
                    new HrisImportRowError(0, "file", "CSV file is required and must not be empty"));
            return new ValidationOutcome(toResponse(0, errors), List.of());
        }

        if (originalFilename != null && !originalFilename.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            List<HrisImportRowError> errors = List.of(
                    new HrisImportRowError(0, "file", "Upload must be a .csv file"));
            return new ValidationOutcome(toResponse(0, errors), List.of());
        }

        HrisCsvParser.ParseResult parse = csvParser.parse(bytes, maxImportRows);
        List<HrisImportRowError> errors = new ArrayList<>(parse.errors());
        List<HrisImportPreparedRow> rows = new ArrayList<>(parse.rows());

        for (HrisImportPreparedRow row : rows) {
            validateRowFields(row, errors);
        }

        validateDuplicatesInFile(rows, errors);

        if (checkDatabase && errors.stream().noneMatch(e -> e.getRowNumber() == 0 && "file".equals(e.getField()))) {
            validateAgainstDatabase(rows, errors);
        }

        return new ValidationOutcome(toResponse(rows.size(), errors), rows);
    }

    private HrisImportValidationResponse toResponse(int totalRows, List<HrisImportRowError> errors) {
        Set<Integer> badRows = errors.stream()
                .map(HrisImportRowError::getRowNumber)
                .filter(r -> r > 0)
                .collect(Collectors.toSet());
        int invalidRows = badRows.size();
        int validRows = Math.max(0, totalRows - invalidRows);
        boolean canCommit = totalRows > 0 && errors.isEmpty();

        return HrisImportValidationResponse.builder()
                .totalRows(totalRows)
                .validRows(validRows)
                .invalidRows(invalidRows)
                .canCommit(canCommit)
                .errors(new ArrayList<>(errors))
                .build();
    }

    private void validateRowFields(HrisImportPreparedRow row, List<HrisImportRowError> errors) {
        int r = row.getRowNumber();
        if (!StringUtils.hasText(row.getEmployeeId())) {
            errors.add(new HrisImportRowError(r, "employeeId", "Employee ID is required"));
        } else if (row.getEmployeeId().length() > 50) {
            errors.add(new HrisImportRowError(r, "employeeId", "Employee ID must be at most 50 characters"));
        }

        if (!StringUtils.hasText(row.getName())) {
            errors.add(new HrisImportRowError(r, "name", "Name is required"));
        } else if (row.getName().length() > 150) {
            errors.add(new HrisImportRowError(r, "name", "Name must be at most 150 characters"));
        }

        if (!StringUtils.hasText(row.getEmail())) {
            errors.add(new HrisImportRowError(r, "email", "Email is required"));
        } else {
            if (row.getEmail().length() > 150) {
                errors.add(new HrisImportRowError(r, "email", "Email must be at most 150 characters"));
            }
            if (!EMAIL_PATTERN.matcher(row.getEmail()).matches()) {
                errors.add(new HrisImportRowError(r, "email", "Invalid email format"));
            }
        }

        if (StringUtils.hasText(row.getLevelRaw()) && parseLevel(row.getLevelRaw()).isEmpty()) {
            errors.add(new HrisImportRowError(r, "level",
                    "Level must be one of: SENIOR, MID, JUNIOR"));
        }

        if (StringUtils.hasText(row.getAvailabilityDateRaw())) {
            try {
                LocalDate.parse(row.getAvailabilityDateRaw().trim());
            } catch (DateTimeParseException ex) {
                errors.add(new HrisImportRowError(r, "availabilityDate",
                        "Availability Date must be ISO-8601 date (yyyy-MM-dd)"));
            }
        }

        if (StringUtils.hasText(row.getLocationRaw()) && row.getLocationRaw().length() > 100) {
            errors.add(new HrisImportRowError(r, "location", "Location must be at most 100 characters"));
        }

        if (StringUtils.hasText(row.getContractTypeRaw()) && parseContractType(row.getContractTypeRaw()).isEmpty()) {
            errors.add(new HrisImportRowError(r, "contractType",
                    "Contract Type must be one of: FULL_TIME, CONTRACT, PART_TIME"));
        }

        if (StringUtils.hasText(row.getCurrentProjectRaw()) && row.getCurrentProjectRaw().length() > 150) {
            errors.add(new HrisImportRowError(r, "currentProject", "Current Project must be at most 150 characters"));
        }

        if (StringUtils.hasText(row.getUtilisationPctRaw())) {
            try {
                int pct = Integer.parseInt(row.getUtilisationPctRaw().trim());
                if (pct < 0 || pct > 100) {
                    errors.add(new HrisImportRowError(r, "utilisationPct", "Utilisation % must be between 0 and 100"));
                }
            } catch (NumberFormatException ex) {
                errors.add(new HrisImportRowError(r, "utilisationPct", "Utilisation % must be a whole number"));
            }
        }

        if (StringUtils.hasText(row.getManagerIdRaw())) {
            try {
                Long.parseLong(row.getManagerIdRaw().trim());
            } catch (NumberFormatException ex) {
                errors.add(new HrisImportRowError(r, "managerId", "Manager ID must be a whole number"));
            }
        }

        if (StringUtils.hasText(row.getHrisSyncStatusRaw()) && parseHrisSyncStatus(row.getHrisSyncStatusRaw()).isEmpty()) {
            errors.add(new HrisImportRowError(r, "hrisSyncStatus",
                    "HRIS Sync Status must be one of: PENDING, SYNCED, FAILED"));
        }
    }

    private static Optional<Level> parseLevel(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Optional.empty();
        }
        String u = raw.trim();
        for (Level l : Level.values()) {
            if (l.name().equalsIgnoreCase(u)) {
                return Optional.of(l);
            }
        }
        return Optional.empty();
    }

    private static Optional<ContractType> parseContractType(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Optional.empty();
        }
        String u = raw.trim();
        for (ContractType c : ContractType.values()) {
            if (c.name().equalsIgnoreCase(u)) {
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }

    private static Optional<HrisSyncStatus> parseHrisSyncStatus(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Optional.empty();
        }
        String u = raw.trim();
        for (HrisSyncStatus s : HrisSyncStatus.values()) {
            if (s.name().equalsIgnoreCase(u)) {
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }

    private void validateDuplicatesInFile(List<HrisImportPreparedRow> rows, List<HrisImportRowError> errors) {
        Map<String, List<Integer>> byEmployeeId = new HashMap<>();
        Map<String, List<Integer>> byEmail = new HashMap<>();

        for (HrisImportPreparedRow row : rows) {
            if (StringUtils.hasText(row.getEmployeeId())) {
                String key = row.getEmployeeId().trim().toLowerCase(Locale.ROOT);
                byEmployeeId.computeIfAbsent(key, k -> new ArrayList<>()).add(row.getRowNumber());
            }
            if (StringUtils.hasText(row.getEmail())) {
                String key = row.getEmail().trim().toLowerCase(Locale.ROOT);
                byEmail.computeIfAbsent(key, k -> new ArrayList<>()).add(row.getRowNumber());
            }
        }

        for (List<Integer> rowNums : byEmployeeId.values()) {
            if (rowNums.size() > 1) {
                for (int rn : rowNums) {
                    errors.add(new HrisImportRowError(rn, "employeeId", "Duplicate Employee ID within this file"));
                }
            }
        }
        for (List<Integer> rowNums : byEmail.values()) {
            if (rowNums.size() > 1) {
                for (int rn : rowNums) {
                    errors.add(new HrisImportRowError(rn, "email", "Duplicate email within this file"));
                }
            }
        }
    }

    private void validateAgainstDatabase(List<HrisImportPreparedRow> rows, List<HrisImportRowError> errors) {
        for (HrisImportPreparedRow row : rows) {
            if (!StringUtils.hasText(row.getEmployeeId()) || !StringUtils.hasText(row.getEmail())) {
                continue;
            }
            String employeeId = row.getEmployeeId().trim();
            String email = row.getEmail().trim();

            Optional<InternalEmployee> byEmail = repository.findByEmailIgnoreCaseAndIsDeletedFalse(email);
            if (byEmail.isPresent() && !employeeId.equals(byEmail.get().getEmployeeId())) {
                errors.add(new HrisImportRowError(row.getRowNumber(), "email",
                        "Email is already assigned to another employee (employeeId=" + byEmail.get().getEmployeeId() + ")"));
            }
        }
    }

    private List<PersistedRow> persistAll(List<HrisImportPreparedRow> rows) {
        List<String> ids = rows.stream()
                .map(HrisImportPreparedRow::getEmployeeId)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();

        Map<String, InternalEmployee> existing = new HashMap<>();
        if (!ids.isEmpty()) {
            for (InternalEmployee e : repository.findByEmployeeIdInAndIsDeletedFalse(ids)) {
                existing.put(e.getEmployeeId(), e);
            }
        }

        List<PersistedRow> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < rows.size(); i += processBatchSize) {
            int end = Math.min(i + processBatchSize, rows.size());
            for (int j = i; j < end; j++) {
                HrisImportPreparedRow row = rows.get(j);
                String employeeId = row.getEmployeeId().trim();
                InternalEmployee entity = existing.get(employeeId);
                boolean created = entity == null;
                if (entity == null) {
                    entity = new InternalEmployee();
                    entity.setEmployeeId(employeeId);
                    entity.setCreatedAt(now);
                }

                applyRowToEntity(row, entity, now);
                InternalEmployee saved = repository.save(entity);
                existing.put(employeeId, saved);
                result.add(new PersistedRow(saved.getEmployeeId(), saved.getEmail(), created));
            }
        }

        return result;
    }

    private void applyRowToEntity(HrisImportPreparedRow row, InternalEmployee entity, LocalDateTime now) {
        entity.setName(row.getName().trim());
        entity.setEmail(row.getEmail().trim());

        if (StringUtils.hasText(row.getLevelRaw())) {
            entity.setLevel(parseLevel(row.getLevelRaw()).orElse(null));
        } else {
            entity.setLevel(null);
        }

        if (StringUtils.hasText(row.getAvailabilityDateRaw())) {
            entity.setAvailabilityDate(LocalDate.parse(row.getAvailabilityDateRaw().trim()));
        } else {
            entity.setAvailabilityDate(null);
        }

        entity.setLocation(trimToNull(row.getLocationRaw()));

        if (StringUtils.hasText(row.getContractTypeRaw())) {
            entity.setContractType(parseContractType(row.getContractTypeRaw()).orElse(null));
        } else {
            entity.setContractType(null);
        }

        entity.setCurrentProject(trimToNull(row.getCurrentProjectRaw()));

        if (StringUtils.hasText(row.getUtilisationPctRaw())) {
            entity.setUtilisationPct(Integer.parseInt(row.getUtilisationPctRaw().trim()));
        } else {
            entity.setUtilisationPct(null);
        }

        if (StringUtils.hasText(row.getManagerIdRaw())) {
            entity.setManagerId(Long.parseLong(row.getManagerIdRaw().trim()));
        } else {
            entity.setManagerId(null);
        }

        if (StringUtils.hasText(row.getHrisSyncStatusRaw())) {
            entity.setHrisSyncStatus(parseHrisSyncStatus(row.getHrisSyncStatusRaw()).orElse(HrisSyncStatus.SYNCED));
        } else {
            entity.setHrisSyncStatus(HrisSyncStatus.SYNCED);
        }

        entity.setSkills(parseSkills(row.getSkillsRaw()));
        if (entity.getSkills() != null && entity.getSkills().length > 0) {
            entity.setSkillsVector(null);
            entity.setLastEmbeddedAt(null);
        }

        entity.setIsDeleted(Boolean.FALSE);
        entity.setDeletedAt(null);
        entity.setUpdatedAt(now);
    }

    private static String trimToNull(String s) {
        if (!StringUtils.hasText(s)) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String[] parseSkills(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String[] parts = raw.split("[|;]");
        List<String> list = Arrays.stream(parts)
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
        if (list.isEmpty()) {
            return null;
        }
        return list.toArray(new String[0]);
    }

    private record ValidationOutcome(HrisImportValidationResponse response, List<HrisImportPreparedRow> rows) {
    }

    private record PersistedRow(String employeeId, String email, boolean created) {
    }
}
