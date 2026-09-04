package com.codeintel.presentation;

import com.codeintel.application.git.AnalyzeGitIntelligence;
import com.codeintel.application.git.GetGitIntelligence;
import com.codeintel.domain.acquisition.AcquisitionRevision;
import com.codeintel.domain.git.ChangeCoupling;
import com.codeintel.domain.git.FileHistory;
import com.codeintel.domain.git.GitCommit;
import com.codeintel.domain.git.GitIntelligenceResult;
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
@RequestMapping("/api/git-intelligence")
public class GitIntelligenceController {
    private final AnalyzeGitIntelligence analyze;
    private final GetGitIntelligence get;
    public GitIntelligenceController(AnalyzeGitIntelligence analyze, GetGitIntelligence get) {
        this.analyze = analyze;
        this.get = get;
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
            List<GitCommit> commits, List<FileHistory> files, List<ChangeCoupling> couplings,
            boolean historyTruncated, Instant analyzedAt) {
        static Response from(GitIntelligenceResult result) {
            return new Response(result.repositoryId().value(), result.acquisitionRevision().kind(),
                    result.acquisitionRevision().value(), result.report().commits(), result.report().files(),
                    result.report().couplings(), result.report().historyTruncated(), result.analyzedAt());
        }
    }
}
