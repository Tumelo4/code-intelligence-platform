package com.codeintel.domain.inventory;

import java.util.List;

public record MavenProjectDescriptor(
        String pomPath,
        String groupId,
        String artifactId,
        String version,
        String packaging,
        String javaVersion,
        List<String> modules,
        List<String> dependencies,
        List<MavenPluginDescriptor> plugins,
        boolean surefireDeclared,
        boolean failsafeDeclared) {
    public MavenProjectDescriptor {
        if (pomPath == null || pomPath.isBlank() || artifactId == null || artifactId.isBlank()) {
            throw new IllegalArgumentException("POM path and artifactId are required");
        }
        modules = List.copyOf(modules);
        dependencies = List.copyOf(dependencies);
        plugins = List.copyOf(plugins);
    }
}
