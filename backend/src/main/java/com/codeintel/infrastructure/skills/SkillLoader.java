package com.codeintel.infrastructure.skills;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

public final class SkillLoader {
    public String load(Path checkoutRoot) {
        Path skillFile = safeFile(checkoutRoot, "SKILL.md");
        try {
            String content = Files.readString(skillFile, StandardCharsets.UTF_8);
            if (!hasCodebaseReviewIdentity(content)) {
                throw new SkillPolicyUnavailableException("SKILL.md is not the codebase-review policy");
            }
            return content;
        } catch (IOException exception) {
            throw new SkillPolicyUnavailableException("could not read SKILL.md", exception);
        }
    }

    static Path safeFile(Path root, String relativePath) {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path candidate = normalizedRoot.resolve(relativePath).normalize();
        if (!candidate.startsWith(normalizedRoot) || Files.isSymbolicLink(candidate)) {
            throw new SkillPolicyUnavailableException("required policy file is missing or unsafe: " + relativePath);
        }
        try {
            Path realRoot = normalizedRoot.toRealPath();
            Path realCandidate = candidate.toRealPath();
            if (!realCandidate.startsWith(realRoot)
                    || !Files.isRegularFile(realCandidate, LinkOption.NOFOLLOW_LINKS)) {
                throw new SkillPolicyUnavailableException(
                        "required policy file is missing or unsafe: " + relativePath);
            }
            return realCandidate;
        } catch (IOException exception) {
            throw new SkillPolicyUnavailableException(
                    "required policy file is missing or unsafe: " + relativePath, exception);
        }
    }

    private static boolean hasCodebaseReviewIdentity(String content) {
        String[] lines = content.split("\\R", -1);
        if (lines.length == 0 || !lines[0].trim().equals("---")) {
            return false;
        }
        boolean expectedName = false;
        for (int index = 1; index < lines.length; index++) {
            String line = lines[index].trim();
            if (line.equals("---")) {
                return expectedName;
            }
            if (line.equals("name: codebase-review")) {
                expectedName = true;
            }
        }
        return false;
    }
}
