package com.codeintel.domain.inventory;

import java.util.List;

public record RepositoryPathInventory(
        List<String> sourceRoots,
        List<String> testRoots,
        List<String> manifests,
        List<String> lockfiles,
        List<String> scripts,
        List<String> ciConfiguration,
        List<String> dockerFiles,
        List<String> migrations,
        List<String> documentation,
        List<String> generatedDirectories,
        List<String> vendoredDirectories,
        List<String> buildOutputDirectories) {
    public RepositoryPathInventory {
        sourceRoots = immutable(sourceRoots);
        testRoots = immutable(testRoots);
        manifests = immutable(manifests);
        lockfiles = immutable(lockfiles);
        scripts = immutable(scripts);
        ciConfiguration = immutable(ciConfiguration);
        dockerFiles = immutable(dockerFiles);
        migrations = immutable(migrations);
        documentation = immutable(documentation);
        generatedDirectories = immutable(generatedDirectories);
        vendoredDirectories = immutable(vendoredDirectories);
        buildOutputDirectories = immutable(buildOutputDirectories);
    }

    private static List<String> immutable(List<String> values) {
        return List.copyOf(values);
    }
}
