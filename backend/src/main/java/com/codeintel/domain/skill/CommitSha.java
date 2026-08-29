package com.codeintel.domain.skill;

import java.util.Locale;

public record CommitSha(String value) {
    public CommitSha {
        if (value == null || !value.matches("[0-9a-fA-F]{40}")) {
            throw new IllegalArgumentException("commit SHA must contain exactly 40 hexadecimal characters");
        }
        value = value.toLowerCase(Locale.ROOT);
    }
}
