package com.talentgrid.candidate.resumeParser.service;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class ResumeStoreService {

    @Value("${gcp.bucket.name}")
    private String bucketName;

    // If running outside GCP, you'd load credentials here.
    // If running on GCP (Cloud Run, GKE, App Engine), getDefaultInstance() automatically authenticates.
    private final Storage storage = StorageOptions.getDefaultInstance().getService();

    public String saveResume(MultipartFile file) {
        try {
            // 1. Generate a unique, safe filename
            String originalName = file.getOriginalFilename();
            String safeName = originalName != null ? originalName.replaceAll("[^a-zA-Z0-9.-]", "_") : "resume.pdf";
            String uniqueFileName = UUID.randomUUID().toString() + "_" + safeName;

            // 2. Define the Blob (the file object in GCP)
            BlobId blobId = BlobId.of(bucketName, uniqueFileName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType(file.getContentType())
                    .build();

            // 3. Upload the file directly to the GCP bucket
            storage.create(blobInfo, file.getBytes());

            // 4. Return the standard public Google Cloud Storage URL
            return "https://storage.googleapis.com/" + bucketName + "/" + uniqueFileName;

        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to Google Cloud Storage: " + e.getMessage());
        }
    }
}
