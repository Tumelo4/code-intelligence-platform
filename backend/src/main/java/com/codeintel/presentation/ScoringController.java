package com.codeintel.presentation;

import com.codeintel.application.scoring.GetScoring;
import com.codeintel.application.scoring.ScoreRepository;
import com.codeintel.domain.acquisition.AcquisitionRevision;
import com.codeintel.domain.repository.RepositoryId;
import com.codeintel.domain.scoring.FileHotspot;
import com.codeintel.domain.scoring.FindingPriority;
import com.codeintel.domain.scoring.ScoringResult;
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
@RequestMapping("/api/scoring")
public class ScoringController {
    private final ScoreRepository score;
    private final GetScoring get;
    public ScoringController(ScoreRepository score, GetScoring get) {
        this.score = score;
        this.get = get;
    }
    @PostMapping("/{repositoryId}") @ResponseStatus(HttpStatus.CREATED)
    public Response create(@PathVariable UUID repositoryId) {
        return Response.from(score.execute(new RepositoryId(repositoryId)));
    }
    @GetMapping("/{repositoryId}")
    public Response latest(@PathVariable UUID repositoryId) {
        return Response.from(get.execute(new RepositoryId(repositoryId)));
    }
    public record Response(UUID repositoryId, AcquisitionRevision.Kind revisionKind, String revision,
            int healthScore, List<FileHotspot> hotspots, List<FindingPriority> priorities,
            Instant scoredAt) {
        static Response from(ScoringResult result) {
            return new Response(result.repositoryId().value(), result.acquisitionRevision().kind(),
                    result.acquisitionRevision().value(), result.report().healthScore(),
                    result.report().hotspots(), result.report().priorities(), result.scoredAt());
        }
    }
}
