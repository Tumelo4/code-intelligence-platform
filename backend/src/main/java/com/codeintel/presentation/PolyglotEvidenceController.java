package com.codeintel.presentation;

import com.codeintel.application.inventory.GetGitJoinedFileEvidence;
import com.codeintel.domain.acquisition.AcquisitionRevision;
import com.codeintel.domain.inventory.GitJoinedFileEvidence;
import com.codeintel.domain.repository.RepositoryId;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/polyglot-evidence")
public class PolyglotEvidenceController {
    private final GetGitJoinedFileEvidence join;

    public PolyglotEvidenceController(GetGitJoinedFileEvidence join) {
        this.join = join;
    }

    @GetMapping("/{repositoryId}")
    public Response gitJoined(@PathVariable UUID repositoryId) {
        var result = join.execute(new RepositoryId(repositoryId));
        return new Response(repositoryId, result.inventory().acquisitionRevision().kind(),
                result.inventory().acquisitionRevision().value(), result.historyTruncated(),
                result.files());
    }

    public record Response(UUID repositoryId, AcquisitionRevision.Kind revisionKind,
            String revision, boolean historyTruncated, List<GitJoinedFileEvidence> files) { }
}
