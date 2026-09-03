package com.codeintel.domain.git;

import java.util.List;

public record GitIntelligenceReport(List<GitCommit> commits, List<FileHistory> files,
        List<ChangeCoupling> couplings, boolean historyTruncated) {
    public GitIntelligenceReport {
        commits = List.copyOf(commits);
        files = List.copyOf(files);
        couplings = List.copyOf(couplings);
    }
}
