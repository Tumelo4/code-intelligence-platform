package com.codeintel.infrastructure.git;

import com.codeintel.application.ports.outbound.GitAnalysisPort;
import com.codeintel.application.git.AnalyzeGitIntelligence;
import com.codeintel.application.git.GetGitIntelligence;
import com.codeintel.application.ports.outbound.AcquisitionRecordStore;
import com.codeintel.application.ports.outbound.GitIntelligenceStore;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GitIntelligenceConfiguration {
    @Bean
    GitIntelligenceLimits gitIntelligenceLimits(
            @Value("${repository.git-intelligence.maximum-commits:10000}") int maximumCommits,
            @Value("${repository.git-intelligence.maximum-files-per-commit:5000}") int maximumFilesPerCommit,
            @Value("${repository.git-intelligence.maximum-diff-bytes:1073741824}") long maximumDiffBytes,
            @Value("${repository.git-intelligence.minimum-coupling-support:3}") int minimumCouplingSupport,
            @Value("${repository.git-intelligence.minimum-coupling-strength:0.5}") double minimumCouplingStrength,
            @Value("${repository.git-intelligence.maximum-coupling-pairs:10000}") int maximumCouplingPairs) {
        return new GitIntelligenceLimits(maximumCommits, maximumFilesPerCommit, maximumDiffBytes,
                minimumCouplingSupport, minimumCouplingStrength, maximumCouplingPairs);
    }

    @Bean
    GitAnalysisPort gitAnalysisPort(GitIntelligenceLimits limits) {
        return new JGitIntelligenceAdapter(limits);
    }

    @Bean
    AnalyzeGitIntelligence analyzeGitIntelligence(AcquisitionRecordStore acquisitions,
            GitAnalysisPort analyzer, GitIntelligenceStore store) {
        return new AnalyzeGitIntelligence(acquisitions, analyzer, store, Clock.systemUTC());
    }

    @Bean
    GetGitIntelligence getGitIntelligence(GitIntelligenceStore store) {
        return new GetGitIntelligence(store);
    }
}
