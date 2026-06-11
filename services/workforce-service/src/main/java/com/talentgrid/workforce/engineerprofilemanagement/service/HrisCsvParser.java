package com.talentgrid.workforce.engineerprofilemanagement.service;

import com.talentgrid.workforce.engineerprofilemanagement.dto.HrisImportRowError;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Parses HRIS CSV aligned with bench export columns, plus optional {Skills}.
 */
@Component
public class HrisCsvParser {

    private static final Set<String> REQUIRED_HEADERS_LOWER = Set.of(
            "employee id",
            "name",
            "email"
    );

    @SuppressWarnings("deprecation")
    private static CSVFormat csvFormat() {
        return CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .withIgnoreHeaderCase()
                .withTrim()
                .withIgnoreEmptyLines();
    }

    public ParseResult parse(MultipartFile file, int maxRows) throws IOException {
        return parse(file.getBytes(), maxRows);
    }

    public ParseResult parse(byte[] data, int maxRows) throws IOException {
        return parse(new ByteArrayInputStream(data), maxRows);
    }

    private ParseResult parse(java.io.InputStream inputStream, int maxRows) throws IOException {
        List<HrisImportRowError> errors = new ArrayList<>();
        List<HrisImportPreparedRow> rows = new ArrayList<>();

        CSVFormat format = csvFormat();

        try (PushbackReader pbr = new PushbackReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            int first = pbr.read();
            if (first != '\uFEFF' && first != -1) {
                pbr.unread(first);
            }
            try (CSVParser parser = CSVParser.parse(pbr, format)) {
                List<String> headerNames = parser.getHeaderNames();
                if (headerNames == null || headerNames.isEmpty()) {
                    errors.add(new HrisImportRowError(0, "file", "CSV must include a header row"));
                    return new ParseResult(rows, errors);
                }
                Set<String> present = headerNames.stream()
                        .map(h -> h == null ? "" : h.trim().toLowerCase(Locale.ROOT))
                        .collect(Collectors.toSet());
                for (String required : REQUIRED_HEADERS_LOWER) {
                    if (!present.contains(required)) {
                        errors.add(new HrisImportRowError(0, "file",
                                "Missing required column: \"" + titleCaseHeader(required) + "\""));
                    }
                }
                if (!errors.isEmpty()) {
                    return new ParseResult(rows, errors);
                }

                int dataRow = 0;
                for (CSVRecord record : parser) {
                    dataRow++;
                    if (dataRow > maxRows) {
                        errors.add(new HrisImportRowError(0, "file",
                                "Maximum " + maxRows + " data rows allowed per import"));
                        return new ParseResult(List.of(), errors);
                    }
                    rows.add(mapRecord(dataRow, record));
                }
            }
        }

        return new ParseResult(rows, errors);
    }

    private static String titleCaseHeader(String lower) {
        // "employee id" -> "Employee ID" for message only
        String[] parts = lower.split(" ");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            if (!parts[i].isEmpty()) {
                sb.append(Character.toUpperCase(parts[i].charAt(0)));
                if (parts[i].length() > 1) {
                    sb.append(parts[i].substring(1));
                }
            }
        }
        return sb.toString();
    }

    private static HrisImportPreparedRow mapRecord(int dataRowNumber, CSVRecord record) {
        return HrisImportPreparedRow.builder()
                .rowNumber(dataRowNumber)
                .employeeId(trimToNull(get(record, "Employee ID")))
                .name(trimToNull(get(record, "Name")))
                .email(trimToNull(get(record, "Email")))
                .levelRaw(trimToNull(get(record, "Level")))
                .availabilityDateRaw(trimToNull(get(record, "Availability Date")))
                .locationRaw(trimToNull(get(record, "Location")))
                .contractTypeRaw(trimToNull(get(record, "Contract Type")))
                .currentProjectRaw(trimToNull(get(record, "Current Project")))
                .utilisationPctRaw(trimToNull(get(record, "Utilisation %")))
                .managerIdRaw(trimToNull(get(record, "Manager ID")))
                .hrisSyncStatusRaw(trimToNull(get(record, "HRIS Sync Status")))
                .skillsRaw(trimToNull(get(record, "Skills")))
                .build();
    }

    private static String get(CSVRecord record, String column) {
        try {
            if (!record.isMapped(column)) {
                return null;
            }
            return record.get(column);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    public record ParseResult(List<HrisImportPreparedRow> rows, List<HrisImportRowError> errors) {
    }
}
