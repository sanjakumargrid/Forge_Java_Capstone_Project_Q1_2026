package com.talentgrid.workforce.airmgnomination.services;

import com.talentgrid.workforce.airmgnomination.entity.DemandRecommendation;
import com.talentgrid.workforce.airmgnomination.repository.DemandRecommendationRepository;
import com.talentgrid.workforce.benchreport.repository.BenchReportRepository;
import com.talentgrid.workforce.engineerprofilemanagement.entity.InternalEmployee;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DemandRecommendationService {

    private final BenchReportRepository benchReportRepository;
    private final DemandRecommendationRepository demandRecommendationRepository;

    @Transactional
    public void calculateAndStoreRecommendations(Long demandId, float[] demandVector) {
        if (demandVector == null || demandVector.length == 0) {
            log.warn("[DEMAND-REC] Invalid demand vector for demandId={} — skipping recommendations", demandId);
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate ninetyDays = today.plusDays(90);

        log.info("[DEMAND-REC] Fetching bench candidate pool for demandId={}", demandId);

        List<InternalEmployee> candidatePool = benchReportRepository
                .findByAvailabilityDateBetweenAndIsDeletedFalseOrderByAvailabilityDateAsc(today, ninetyDays);

        log.info("[DEMAND-REC] Found {} bench candidates. Calculating semantic match...", candidatePool.size());

        List<DemandRecommendation> recommendations = candidatePool.stream()
                .map(emp -> {
                    float[] resumeEmbedding = emp.getResumeEmbedding();
                    if (resumeEmbedding == null) {
                        log.warn("[DEMAND-REC] Employee {} has NULL resume embedding", emp.getEmployeeId());
                    } else if (resumeEmbedding.length != demandVector.length) {
                        log.warn("[DEMAND-REC] Employee {} has resume embedding length {} but demand vector length is {}", 
                                emp.getEmployeeId(), resumeEmbedding.length, demandVector.length);
                    }
                    double similarity = calculateCosineSimilarity(demandVector, resumeEmbedding);
                    // Scale cosine similarity [-1, 1] to [0, 100] AI score
                    double aiScore = Math.max(0.0, Math.min(100.0, similarity * 100.0));

                    return DemandRecommendation.builder()
                            .demandId(demandId)
                            .employee(emp)
                            .aiScore(aiScore)
                            .availabilityDate(emp.getAvailabilityDate())
                            .build();
                })
                .sorted(Comparator.comparingDouble(DemandRecommendation::getAiScore).reversed()
                        .thenComparing(rec -> Objects.requireNonNullElse(rec.getAvailabilityDate(), LocalDate.MAX)))
                .limit(10)
                .collect(Collectors.toList());

        // Clear existing recommendations to perform a fresh upsert
        demandRecommendationRepository.deleteByDemandId(demandId);

        if (!recommendations.isEmpty()) {
            demandRecommendationRepository.saveAll(recommendations);
            log.info("[DEMAND-REC] Saved top {} recommendations for demandId={}", recommendations.size(), demandId);
        } else {
            log.info("[DEMAND-REC] No valid recommendations found for demandId={}", demandId);
        }
    }

    private double calculateCosineSimilarity(float[] vectorA, float[] vectorB) {
        if (vectorA == null || vectorB == null || vectorA.length == 0 || vectorB.length == 0) {
            return 0.0;
        }
        if (vectorA.length != vectorB.length) {
            return 0.0;
        }
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += vectorA[i] * vectorA[i];
            normB += vectorB[i] * vectorB[i];
        }

        if (normA == 0.0 || normB == 0.0) {
            log.warn("[DEMAND-REC] Zero norm detected. normA={}, normB={}", normA, normB);
            return 0.0;
        }
        
        double similarity = dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
        log.info("[DEMAND-REC] dotProduct={}, normA={}, normB={}, similarity={}", dotProduct, normA, normB, similarity);
        return similarity;
    }
}
