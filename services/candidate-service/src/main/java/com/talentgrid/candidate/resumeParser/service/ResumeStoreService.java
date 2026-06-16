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

    private final Storage storage = StorageOptions.getDefaultInstance().getService();

    public String saveResume(MultipartFile file) {
        try {
            String originalName = file.getOriginalFilename();
            String safeName = originalName != null ? originalName.replaceAll("[^a-zA-Z0-9.-]", "_") : "resume.pdf";
            String uniqueFileName = UUID.randomUUID().toString() + "_" + safeName;


            BlobId blobId = BlobId.of(bucketName, uniqueFileName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType(file.getContentType())
                    .build();


            storage.create(blobInfo, file.getBytes());


            return "https://storage.googleapis.com/" + bucketName + "/" + uniqueFileName;

        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to Google Cloud Storage: " + e.getMessage());
        }
    }
}
