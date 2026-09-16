package com.codeintel.infrastructure.inventory;

import com.codeintel.application.inventory.GetRepositoryInventory;
import com.codeintel.application.inventory.GetGitJoinedFileEvidence;
import com.codeintel.application.git.GetGitIntelligence;
import com.codeintel.application.inventory.InventoryRepository;
import com.codeintel.application.ports.outbound.AcquisitionRecordStore;
import com.codeintel.application.ports.outbound.RepositoryInventoryPort;
import com.codeintel.application.ports.outbound.RepositoryInventoryStore;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InventoryConfiguration {
    @Bean
    InventoryLimits inventoryLimits(
            @Value("${repository.inventory.maximum-files:100000}") int maximumFiles,
            @Value("${repository.inventory.maximum-modules:500}") int maximumModules,
            @Value("${repository.inventory.maximum-pom-bytes:2097152}") long maximumPomBytes,
            @Value("${repository.inventory.maximum-evidence-file-bytes:2097152}") long maximumEvidenceFileBytes) {
        return new InventoryLimits(maximumFiles, maximumModules, maximumPomBytes,
                maximumEvidenceFileBytes);
    }

    @Bean
    RepositoryInventoryPort repositoryInventoryPort(InventoryLimits limits) {
        return new PassiveRepositoryInventoryAdapter(limits);
    }

    @Bean
    InventoryRepository inventoryRepository(AcquisitionRecordStore acquisitionStore,
            RepositoryInventoryPort inventoryPort, RepositoryInventoryStore inventoryStore) {
        return new InventoryRepository(acquisitionStore, inventoryPort, inventoryStore,
                Clock.systemUTC());
    }

    @Bean
    GetRepositoryInventory getRepositoryInventory(RepositoryInventoryStore inventoryStore) {
        return new GetRepositoryInventory(inventoryStore);
    }

    @Bean
    GetGitJoinedFileEvidence getGitJoinedFileEvidence(GetRepositoryInventory inventories,
            GetGitIntelligence git) {
        return new GetGitJoinedFileEvidence(inventories, git);
    }
}
