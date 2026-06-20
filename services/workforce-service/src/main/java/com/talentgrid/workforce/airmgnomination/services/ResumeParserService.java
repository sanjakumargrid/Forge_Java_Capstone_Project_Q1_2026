package com.talentgrid.workforce.airmgnomination.services;

import com.talentgrid.workforce.engineerprofilemanagement.dto.EmployeeProfileUpdatedPayload;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class ResumeParserService {

    private static final Pattern DRIVE_FILE_ID_PATTERN = Pattern.compile("/d/([^/]+)");
    private static final Pattern QUERY_ID_PATTERN = Pattern.compile("[?&]id=([^&]+)");
    private static final Pattern MARKDOWN_LINK_PATTERN = Pattern.compile("^\\[(?:[^\\]]*)\\]\\((https?://[^)]+)\\)$");

    public String parseResumeText(EmployeeProfileUpdatedPayload payload) {
        if (payload == null || payload.getEmployeeId() == null) {
            throw new IllegalArgumentException("Employee profile payload must contain employeeId");
        }

        if (!StringUtils.hasText(payload.getResumeDriveLink())) {
            throw new IllegalArgumentException("resumeDriveLink is required to parse resume text");
        }

        List<String> candidateUrls = resolveResumeUrls(payload.getResumeDriveLink());

        try {
            log.info("[AI-RESUME] Starting parse | employeeId={} | resumeDriveLink={} | normalizedUrl={}",
                    payload.getEmployeeId(), payload.getResumeDriveLink(), candidateUrls);

            IOException lastError = null;
            for (String candidateUrl : candidateUrls) {
                try {
                    DownloadedResume downloadedResume = downloadResume(candidateUrl);
                    String parsedText = extractTextWithTika(downloadedResume);

                    if (!StringUtils.hasText(parsedText)) {
                        log.warn("[AI-RESUME] Resume parsed but text is empty | employeeId={} | source={} | contentType={}",
                                payload.getEmployeeId(), downloadedResume.sourceUrl(), downloadedResume.contentType());
                        continue;
                    }

                    log.info("[AI-RESUME] Parsed resume text | employeeId={} | source={} | contentType={} | fileName={} | textLength={} | preview={}",
                            payload.getEmployeeId(),
                            downloadedResume.sourceUrl(),
                            downloadedResume.contentType(),
                            downloadedResume.fileName(),
                            parsedText.length(),
                            previewText(parsedText));

                    log.info("[AI-RESUME] Full parsed resume text | employeeId={} | source={} | text={}",
                            payload.getEmployeeId(),
                            downloadedResume.sourceUrl(),
                            parsedText);

                    return parsedText;
                } catch (IOException ex) {
                    lastError = ex;
                    log.warn("[AI-RESUME] Resume candidate failed | employeeId={} | source={} | error={}",
                            payload.getEmployeeId(), candidateUrl, ex.getMessage());
                }
            }

            if (lastError != null) {
                throw lastError;
            }

            return "";
        } catch (IOException ex) {
            log.error("[AI-RESUME] Failed to parse resume | employeeId={} | resumeDriveLink={} | normalizedUrl={} | error={}",
                    payload.getEmployeeId(), payload.getResumeDriveLink(), candidateUrls, ex.getMessage(), ex);
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Failed to parse resume from drive link: " + ex.getMessage(),
                    ex
            );
        }
    }

    private DownloadedResume downloadResume(String resumeDriveLink) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(resumeDriveLink).toURL().openConnection();
        connection.setConnectTimeout(15_000);
        connection.setReadTimeout(30_000);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", "TalentGrid-Workforce/1.0");
        connection.setRequestProperty("Accept", "*/*");

        int status = connection.getResponseCode();
        InputStream inputStream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
        if (inputStream == null) {
            throw new IOException("Resume download returned no data");
        }

        byte[] bytes;
        try (InputStream is = inputStream) {
            bytes = readAllBytes(is);
        }

        if (bytes.length == 0) {
            throw new IOException("Resume download returned an empty file");
        }

        String contentType = connection.getContentType();
        String fileName = guessFileName(resumeDriveLink, contentType);
        return new DownloadedResume(resumeDriveLink, fileName, contentType, bytes);
    }

    private String extractTextWithTika(DownloadedResume downloadedResume) throws IOException {
        Metadata metadata = new Metadata();
        if (StringUtils.hasText(downloadedResume.fileName())) {
            metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, downloadedResume.fileName());
        }
        if (StringUtils.hasText(downloadedResume.contentType())) {
            metadata.set(Metadata.CONTENT_TYPE, downloadedResume.contentType());
        }

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(downloadedResume.bytes())) {
            BodyContentHandler handler = new BodyContentHandler(-1);
            AutoDetectParser parser = new AutoDetectParser();
            parser.parse(inputStream, handler, metadata, new ParseContext());
            return normalizeWhitespace(handler.toString());
        } catch (SAXException | TikaException ex) {
            throw new IOException("Apache Tika failed to extract text", ex);
        }
    }

    private List<String> resolveResumeUrls(String resumeDriveLink) {
        String trimmed = sanitizeResumeDriveLink(resumeDriveLink);
        LinkedHashSet<String> candidates = new LinkedHashSet<>();

        if (trimmed.contains("docs.google.com/document")) {
            String fileId = extractDriveFileId(trimmed);
            if (fileId != null) {
                candidates.add("https://docs.google.com/document/d/" + fileId + "/export?format=docx");
                candidates.add("https://docs.google.com/document/d/" + fileId + "/export?format=txt");
                return new ArrayList<>(candidates);
            }
        }

        if (trimmed.contains("drive.google.com")) {
            String fileId = extractDriveFileId(trimmed);
            if (fileId != null) {
                candidates.add("https://drive.google.com/uc?export=download&id=" + fileId);
                return new ArrayList<>(candidates);
            }
        }

        candidates.add(trimmed);
        return new ArrayList<>(candidates);
    }

    private String sanitizeResumeDriveLink(String resumeDriveLink) {
        String trimmed = resumeDriveLink == null ? "" : resumeDriveLink.trim();
        if (!StringUtils.hasText(trimmed)) {
            return "";
        }

        Matcher markdownMatcher = MARKDOWN_LINK_PATTERN.matcher(trimmed);
        if (markdownMatcher.matches()) {
            return markdownMatcher.group(1).trim();
        }

        return trimmed;
    }

    private String extractDriveFileId(String url) {
        Matcher pathMatcher = DRIVE_FILE_ID_PATTERN.matcher(url);
        if (pathMatcher.find()) {
            return pathMatcher.group(1);
        }

        Matcher queryMatcher = QUERY_ID_PATTERN.matcher(url);
        if (queryMatcher.find()) {
            return queryMatcher.group(1);
        }

        return null;
    }

    private String guessFileName(String sourceUrl, String contentType) {
        try {
            String path = URI.create(sourceUrl).getPath();
            if (path != null && path.contains("/")) {
                String candidate = path.substring(path.lastIndexOf('/') + 1);
                if (StringUtils.hasText(candidate) && candidate.length() < 255) {
                    return candidate;
                }
            }
        } catch (Exception ignored) {
            // Fall back below.
        }

        if (StringUtils.hasText(contentType)) {
            String lowerContentType = contentType.toLowerCase();
            if (lowerContentType.contains("pdf")) {
                return "resume.pdf";
            }
            if (lowerContentType.contains("word") || lowerContentType.contains("officedocument")) {
                return "resume.docx";
            }
            if (lowerContentType.startsWith("text/")) {
                return "resume.txt";
            }
        }

        return "resume";
    }

    private String normalizeWhitespace(String input) {
        if (input == null) {
            return "";
        }

        return input.replace('\u0000', ' ')
                .replaceAll("[\\t\\x0B\\f\\r]+", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    private String previewText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        int maxPreview = 300;
        if (text.length() <= maxPreview) {
            return text;
        }

        return text.substring(0, maxPreview) + "...";
    }

    private byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int read;
        while ((read = inputStream.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        return buffer.toByteArray();
    }

    private record DownloadedResume(
            String sourceUrl,
            String fileName,
            String contentType,
            byte[] bytes
    ) {
    }
}
