package com.codeintel.infrastructure.scoring;

import com.codeintel.application.ports.outbound.GitIntelligenceStore;
import com.codeintel.application.ports.outbound.ScoringPort;
import com.codeintel.application.ports.outbound.ScoringStore;
import com.codeintel.application.ports.outbound.StaticAnalysisStore;
import com.codeintel.application.scoring.GetScoring;
import com.codeintel.application.scoring.ScoreRepository;
import com.codeintel.domain.analysis.FindingType;
import java.time.Clock;
import java.util.EnumSet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ScoringConfiguration {
    @Bean
    ScoringPort scoringPort() {
        return new DeterministicScoringAdapter(EnumSet.allOf(FindingType.class));
    }

    @Bean
    ScoreRepository scoreRepository(StaticAnalysisStore analyses, GitIntelligenceStore gitIntelligence,
            ScoringPort scorer, ScoringStore scores) {
        return new ScoreRepository(analyses, gitIntelligence, scorer, scores, Clock.systemUTC());
    }

    @Bean
    GetScoring getScoring(ScoringStore scores) { return new GetScoring(scores); }
}
