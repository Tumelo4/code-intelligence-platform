package com.codeintel.domain.scoring;

public record FileHotspot(String file, int score, int staticRisk, int commitActivity,
        int churn, int ownershipConcentration, int coupling) {
    public FileHotspot {
        if (invalidRelativeFile(file) || invalid(score) || invalid(staticRisk)
                || invalid(commitActivity) || invalid(churn) || invalid(ownershipConcentration)
                || invalid(coupling)) {
            throw new IllegalArgumentException("file hotspot fields are invalid");
        }
    }

    private static boolean invalid(int value) { return value < 0 || value > 100; }

    private static boolean invalidRelativeFile(String file) {
        return file == null || file.isBlank() || file.startsWith("/") || file.contains("\\")
                || file.equals("..") || file.startsWith("../") || file.endsWith("/..")
                || file.contains("/../");
    }
}
