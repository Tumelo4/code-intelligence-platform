package com.codeintel.domain.inventory;

/** Language-neutral file evidence from the immutable acquisition tree. */
public record FileEvidence(String file, String language, long bytes, int lines, Status status) {
    public enum Status { BASIC, BINARY, TOO_LARGE, GENERATED, VENDORED, BUILD_OUTPUT }

    public FileEvidence {
        if (file == null || file.isBlank() || file.startsWith("/") || file.contains("\\")
                || file.equals("..") || file.startsWith("../") || file.endsWith("/..")
                || file.contains("/../") || language == null || language.isBlank()
                || bytes < 0 || lines < 0 || status == null || (status != Status.BASIC && lines != 0)) {
            throw new IllegalArgumentException("file evidence fields are invalid");
        }
    }
}
