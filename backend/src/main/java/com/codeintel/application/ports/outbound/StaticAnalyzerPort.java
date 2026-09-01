package com.codeintel.application.ports.outbound;

import com.codeintel.domain.acquisition.AcquisitionRevision;
import com.codeintel.domain.analysis.AnalysisReport;
import java.nio.file.Path;
import java.util.List;

public interface StaticAnalyzerPort {
    AnalysisReport analyze(Path immutableOriginal, List<String> sourceRoots,
            AcquisitionRevision revision);
}
