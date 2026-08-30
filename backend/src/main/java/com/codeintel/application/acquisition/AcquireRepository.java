package com.codeintel.application.acquisition;

import com.codeintel.application.ports.outbound.AcquisitionRecordStore;
import com.codeintel.application.ports.outbound.AcquisitionSourcePort;
import com.codeintel.application.ports.outbound.ArchiveAcquisitionPort;
import com.codeintel.application.ports.outbound.GitAcquisitionPort;
import com.codeintel.application.ports.outbound.LocalAcquisitionPort;
import com.codeintel.application.ports.outbound.RepositoryStore;
import com.codeintel.domain.acquisition.AcquiredRepository;
import com.codeintel.domain.acquisition.GitRemoteAcquisitionSource;
import com.codeintel.domain.acquisition.LocalDirectoryAcquisitionSource;
import com.codeintel.domain.acquisition.RepositoryAcquisitionRequest;
import com.codeintel.domain.acquisition.ZipArchiveAcquisitionSource;
import com.codeintel.domain.repository.RepositoryId;
import java.util.Objects;

public final class AcquireRepository {
    private final RepositoryStore repositoryStore;
    private final AcquisitionSourcePort sourcePort;
    private final GitAcquisitionPort gitPort;
    private final ArchiveAcquisitionPort archivePort;
    private final LocalAcquisitionPort localPort;
    private final AcquisitionRecordStore recordStore;

    public AcquireRepository(RepositoryStore repositoryStore, AcquisitionSourcePort sourcePort,
            GitAcquisitionPort gitPort, ArchiveAcquisitionPort archivePort,
            LocalAcquisitionPort localPort, AcquisitionRecordStore recordStore) {
        this.repositoryStore = Objects.requireNonNull(repositoryStore);
        this.sourcePort = Objects.requireNonNull(sourcePort);
        this.gitPort = Objects.requireNonNull(gitPort);
        this.archivePort = Objects.requireNonNull(archivePort);
        this.localPort = Objects.requireNonNull(localPort);
        this.recordStore = Objects.requireNonNull(recordStore);
    }

    public AcquiredRepository execute(RepositoryId repositoryId, String requestedRevision) {
        repositoryStore.find(repositoryId)
                .orElseThrow(() -> new AcquisitionNotFoundException("validated repository connection not found"));
        var connection = repositoryStore.findSource(repositoryId)
                .orElseThrow(() -> new AcquisitionNotFoundException("repository acquisition source not found"));
        var source = sourcePort.resolve(connection);
        RepositoryAcquisitionRequest request = new RepositoryAcquisitionRequest(
                repositoryId, source, requestedRevision);
        AcquiredRepository acquired;
        if (source instanceof GitRemoteAcquisitionSource) {
            acquired = gitPort.acquire(request);
        } else if (source instanceof ZipArchiveAcquisitionSource) {
            acquired = archivePort.acquire(request);
        } else if (source instanceof LocalDirectoryAcquisitionSource) {
            acquired = localPort.acquire(request);
        } else {
            throw new IllegalArgumentException("unsupported acquisition source");
        }
        recordStore.save(acquired);
        return acquired;
    }
}
