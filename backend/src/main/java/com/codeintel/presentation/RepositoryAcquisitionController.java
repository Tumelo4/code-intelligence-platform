package com.codeintel.presentation;

import com.codeintel.application.acquisition.AcquireRepository;
import com.codeintel.application.acquisition.GetLatestAcquisition;
import com.codeintel.domain.acquisition.AcquiredRepository;
import com.codeintel.domain.acquisition.AcquisitionRevision;
import com.codeintel.domain.repository.RepositoryId;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/repository-acquisitions")
public class RepositoryAcquisitionController {
    private final AcquireRepository acquireRepository;
    private final GetLatestAcquisition getLatestAcquisition;

    public RepositoryAcquisitionController(
            AcquireRepository acquireRepository, GetLatestAcquisition getLatestAcquisition) {
        this.acquireRepository = acquireRepository;
        this.getLatestAcquisition = getLatestAcquisition;
    }

    @PostMapping("/{repositoryId}")
    @ResponseStatus(HttpStatus.CREATED)
    public AcquisitionResponse acquire(
            @PathVariable UUID repositoryId, @RequestBody AcquisitionRequest request) {
        if (request == null || request.requestedRevision() == null) {
            throw new IllegalArgumentException("requestedRevision is required");
        }
        return AcquisitionResponse.from(acquireRepository.execute(
                new RepositoryId(repositoryId), request.requestedRevision()));
    }

    @GetMapping("/{repositoryId}")
    public AcquisitionResponse latest(@PathVariable UUID repositoryId) {
        return AcquisitionResponse.from(getLatestAcquisition.execute(new RepositoryId(repositoryId)));
    }

    public record AcquisitionRequest(String requestedRevision) {
    }

    public record AcquisitionResponse(
            UUID repositoryId,
            AcquisitionRevision.Kind revisionKind,
            String revision,
            String requestedRevision,
            int skippedSubmodules,
            Instant acquiredAt) {
        static AcquisitionResponse from(AcquiredRepository acquired) {
            return new AcquisitionResponse(acquired.repositoryId().value(), acquired.revision().kind(),
                    acquired.revision().value(), acquired.requestedRevision(),
                    acquired.skippedSubmodules(), acquired.acquiredAt());
        }
    }
}
