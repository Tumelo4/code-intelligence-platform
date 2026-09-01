package com.codeintel.infrastructure.analysis;

import com.codeintel.application.analysis.AnalyzeRepository;
import com.codeintel.application.analysis.GetStaticAnalysis;
import com.codeintel.application.ports.outbound.AcquisitionRecordStore;
import com.codeintel.application.ports.outbound.RepositoryInventoryStore;
import com.codeintel.application.ports.outbound.StaticAnalysisStore;
import com.codeintel.application.ports.outbound.StaticAnalyzerPort;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AnalysisConfiguration {
    @Bean AnalysisThresholds analysisThresholds() { return AnalysisThresholds.defaults(); }
    @Bean StaticAnalyzerPort staticAnalyzerPort(AnalysisThresholds thresholds) {
        return new JavaParserStaticAnalyzerAdapter(thresholds);
    }
    @Bean AnalyzeRepository analyzeRepository(AcquisitionRecordStore acquisitions,
            RepositoryInventoryStore inventories, StaticAnalyzerPort analyzer, StaticAnalysisStore analyses) {
        return new AnalyzeRepository(acquisitions, inventories, analyzer, analyses, Clock.systemUTC());
    }
    @Bean GetStaticAnalysis getStaticAnalysis(StaticAnalysisStore store) { return new GetStaticAnalysis(store); }
}
