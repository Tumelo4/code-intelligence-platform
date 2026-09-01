package com.codeintel.domain.inventory;

import java.util.List;

public record InventoryReport(
        List<String> languages,
        List<String> buildSystems,
        RepositoryPathInventory paths,
        List<MavenProjectDescriptor> mavenProjects,
        int inspectedFiles) {
    public InventoryReport {
        languages = List.copyOf(languages);
        buildSystems = List.copyOf(buildSystems);
        mavenProjects = List.copyOf(mavenProjects);
        if (paths == null || inspectedFiles < 0) {
            throw new IllegalArgumentException("inventory report is invalid");
        }
    }
}
