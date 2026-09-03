package com.codeintel.application.ports.outbound;

import com.codeintel.domain.acquisition.AcquisitionRevision;
import com.codeintel.domain.git.GitIntelligenceReport;
import java.nio.file.Path;

public interface GitAnalysisPort {
    GitIntelligenceReport analyze(Path immutableOriginal, AcquisitionRevision revision);
}
