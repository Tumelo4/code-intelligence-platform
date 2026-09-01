package com.codeintel.presentation;

import com.codeintel.application.analysis.AnalyzeRepository;
import com.codeintel.application.analysis.GetStaticAnalysis;
import com.codeintel.domain.acquisition.AcquisitionRevision;
import com.codeintel.domain.analysis.AnalysisFinding;
import com.codeintel.domain.analysis.JavaFileMetrics;
import com.codeintel.domain.analysis.StaticAnalysisResult;
import com.codeintel.domain.repository.RepositoryId;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/static-analyses")
public class StaticAnalysisController {
    private final AnalyzeRepository analyze;
    private final GetStaticAnalysis get;
    public StaticAnalysisController(AnalyzeRepository analyze, GetStaticAnalysis get) {
        this.analyze = analyze; this.get = get;
    }
    @PostMapping("/{repositoryId}") @ResponseStatus(HttpStatus.CREATED)
    public Response create(@PathVariable UUID repositoryId) {
        return Response.from(analyze.execute(new RepositoryId(repositoryId)));
    }
    @GetMapping("/{repositoryId}")
    public Response latest(@PathVariable UUID repositoryId) {
        return Response.from(get.execute(new RepositoryId(repositoryId)));
    }
    public record Response(UUID repositoryId, AcquisitionRevision.Kind revisionKind, String revision,
            List<JavaFileMetrics> files, List<AnalysisFinding> findings, Instant analyzedAt) {
        static Response from(StaticAnalysisResult result) {
            return new Response(result.repositoryId().value(), result.acquisitionRevision().kind(),
                    result.acquisitionRevision().value(), result.report().files(),
                    result.report().findings(), result.analyzedAt());
        }
    }
}
