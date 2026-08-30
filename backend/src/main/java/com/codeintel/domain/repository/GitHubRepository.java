package com.codeintel.domain.repository;

public record GitHubRepository(String owner, String name) {
    private static final String PART = "[A-Za-z0-9_.-]+";

    public GitHubRepository {
        if (owner == null || !owner.matches(PART)) {
            throw new IllegalArgumentException("owner is invalid");
        }
        if (name == null || !name.matches(PART)) {
            throw new IllegalArgumentException("name is invalid");
        }
    }

    public String fullName() {
        return owner + "/" + name;
    }
}
