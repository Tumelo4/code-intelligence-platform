package com.codeintel.domain.inventory;

import java.util.List;

public record InventoryReport(
        List<String> languages,
        List<String> buildSystems,
        RepositoryPathInventory paths,
        List<MavenProjectDescriptor> mavenProjects,
        int inspectedFiles,
        List<FileEvidence> fileEvidence) {
    public InventoryReport {
        languages = List.copyOf(languages);
        buildSystems = List.copyOf(buildSystems);
        mavenProjects = List.copyOf(mavenProjects);
        fileEvidence = fileEvidence == null ? List.of() : List.copyOf(fileEvidence);
        if (paths == null || inspectedFiles < 0) {
            throw new IllegalArgumentException("inventory report is invalid");
        }
    }

    /** Keeps callers and stored reports from the Java-first milestone compatible. */
    public InventoryReport(List<String> languages, List<String> buildSystems,
            RepositoryPathInventory paths, List<MavenProjectDescriptor> mavenProjects,
            int inspectedFiles) {
        this(languages, buildSystems, paths, mavenProjects, inspectedFiles, List.of());
    }
}
