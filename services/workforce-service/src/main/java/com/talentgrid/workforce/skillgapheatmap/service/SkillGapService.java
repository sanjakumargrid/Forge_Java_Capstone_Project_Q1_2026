package com.talentgrid.workforce.skillgapheatmap.service;

import com.talentgrid.workforce.skillgapheatmap.dto.RefreshResponse;
import com.talentgrid.workforce.skillgapheatmap.dto.SkillGapResponse;
import com.talentgrid.workforce.skillgapheatmap.dto.SkillGapSummaryResponse;
import com.talentgrid.workforce.skillgapheatmap.dto.SkillTrendResponse;

public interface SkillGapService {

    SkillGapResponse getSkillGap();

    SkillGapSummaryResponse getSummary();

    SkillTrendResponse getTrends();

    RefreshResponse refresh();
}
