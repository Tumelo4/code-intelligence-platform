package com.codeintel.domain.inventory;

import java.util.List;

public record MavenPluginDescriptor(
        String groupId, String artifactId, String version, List<String> executionIds,
        List<String> goals) {
    public MavenPluginDescriptor {
        groupId = normalize(groupId);
        artifactId = require(artifactId, "artifactId");
        version = normalize(version);
        executionIds = List.copyOf(executionIds);
        goals = List.copyOf(goals);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String require(String value, String name) {
        String normalized = normalize(value);
        if (normalized == null) throw new IllegalArgumentException(name + " is required");
        return normalized;
    }
}
