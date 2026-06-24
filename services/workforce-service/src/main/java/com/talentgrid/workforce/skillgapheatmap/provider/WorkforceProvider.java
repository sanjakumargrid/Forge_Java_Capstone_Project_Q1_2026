package com.talentgrid.workforce.skillgapheatmap.provider;

import com.talentgrid.workforce.skillgapheatmap.provider.model.EngineerResponse;

import java.util.List;

/**
 * Port interface for fetching bench engineers with their current skills.
 * Production implementation: BenchReportWorkforceProvider (calls BenchReportService in-process).
 */
public interface WorkforceProvider {

    List<EngineerResponse> getBenchEngineers();
}
