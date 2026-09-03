package com.codeintel.domain.git;

public record ChangeCoupling(String firstFile, String secondFile, int cochangeCount,
        int firstFileCommits, int secondFileCommits, double strength) {
    public ChangeCoupling {
        if (!validFile(firstFile) || !validFile(secondFile) || firstFile.compareTo(secondFile) >= 0
                || cochangeCount < 1 || firstFileCommits < cochangeCount
                || secondFileCommits < cochangeCount || !Double.isFinite(strength)
                || strength < 0 || strength > 1) {
            throw new IllegalArgumentException("change coupling fields are invalid");
        }
    }

    private static boolean validFile(String file) {
        return file != null && !file.isBlank() && !file.startsWith("/") && !file.contains("\\")
                && !file.equals("..") && !file.startsWith("../") && !file.endsWith("/..")
                && !file.contains("/../");
    }
}
