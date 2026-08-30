package com.codeintel.infrastructure.acquisition;

import com.codeintel.application.acquisition.AcquireRepository;
import com.codeintel.application.acquisition.GetLatestAcquisition;
import com.codeintel.application.ports.outbound.AcquisitionRecordStore;
import com.codeintel.application.ports.outbound.AcquisitionSourcePort;
import com.codeintel.application.ports.outbound.ArchiveAcquisitionPort;
import com.codeintel.application.ports.outbound.GitAcquisitionPort;
import com.codeintel.application.ports.outbound.LocalAcquisitionPort;
import com.codeintel.application.ports.outbound.RepositoryStore;
import com.codeintel.infrastructure.repository.GitHubAppAccessProbe;
import com.codeintel.infrastructure.repository.GitHubAppApiAccessProbe;
import java.nio.file.Path;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AcquisitionConfiguration {
    @Bean
    AcquisitionLimits acquisitionLimits(
            @Value("${repository.acquisition.maximum-files:100000}") int maximumFiles,
            @Value("${repository.acquisition.maximum-expanded-bytes:1073741824}") long maximumBytes,
            @Value("${repository.acquisition.maximum-file-bytes:104857600}") long maximumFileBytes) {
        return new AcquisitionLimits(maximumFiles, maximumBytes, maximumFileBytes);
    }

    @Bean
    AcquisitionSourcePort acquisitionSourcePort(
            @Value("${repository.connection.zip-staging-root:/tmp/code-intelligence-uploads}")
            Path zipStagingRoot) {
        return new RepositoryAcquisitionSourceAdapter(zipStagingRoot);
    }

    @Bean
    AcquisitionNetworkController acquisitionNetworkController() {
        return new ScopedAcquisitionNetworkController();
    }

    @Bean
    GitCredentialProvider gitCredentialProvider(GitHubAppAccessProbe gitHubProbe) {
        return gitHubProbe instanceof GitHubAppApiAccessProbe apiProbe
                ? new GitHubAppGitCredentialProvider(apiProbe)
                : new NoCredentialsProvider();
    }

    @Bean
    GitAcquisitionPort gitAcquisitionPort(
            @Value("${repository.acquisition.workspace-root:/tmp/code-intelligence-acquisitions}")
            Path workspaceRoot,
            AcquisitionLimits limits,
            AcquisitionNetworkController networkController,
            GitCredentialProvider credentialProvider) {
        return new JGitAcquisitionAdapter(workspaceRoot, limits, networkController,
                credentialProvider, Clock.systemUTC());
    }

    @Bean
    ArchiveAcquisitionPort archiveAcquisitionPort(
            @Value("${repository.acquisition.workspace-root:/tmp/code-intelligence-acquisitions}")
            Path workspaceRoot,
            AcquisitionLimits limits) {
        return new ZipAcquisitionAdapter(workspaceRoot, new SafeZipExtractor(limits), Clock.systemUTC());
    }

    @Bean
    LocalAcquisitionPort localAcquisitionPort(
            @Value("${repository.acquisition.workspace-root:/tmp/code-intelligence-acquisitions}")
            Path workspaceRoot,
            AcquisitionLimits limits) {
        return new LocalSnapshotAcquisitionAdapter(workspaceRoot, limits, Clock.systemUTC());
    }

    @Bean
    AcquireRepository acquireRepository(RepositoryStore repositoryStore,
            AcquisitionSourcePort sourcePort, GitAcquisitionPort gitPort,
            ArchiveAcquisitionPort archivePort, LocalAcquisitionPort localPort,
            AcquisitionRecordStore recordStore) {
        return new AcquireRepository(repositoryStore, sourcePort, gitPort, archivePort, localPort,
                recordStore);
    }

    @Bean
    GetLatestAcquisition getLatestAcquisition(AcquisitionRecordStore recordStore) {
        return new GetLatestAcquisition(recordStore);
    }
}
