package com.codeintel.infrastructure.skills;

import com.codeintel.domain.skill.CommitSha;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class SkillVersionService {
    public CommitSha resolveHead(Path checkoutRoot, String expectedBranch) {
        Path gitRoot = checkoutRoot.resolve(".git");
        try {
            String head = Files.readString(gitRoot.resolve("HEAD"), StandardCharsets.UTF_8).trim();
            if (!head.startsWith("ref: refs/heads/")) {
                throw new SkillPolicyUnavailableException("skill checkout must be attached to a branch");
            }
            String ref = head.substring("ref: ".length());
            if (!ref.equals("refs/heads/" + expectedBranch)) {
                throw new SkillPolicyUnavailableException("skill checkout branch does not match request");
            }
            Path looseRef = gitRoot.resolve(ref).normalize();
            String sha = Files.isRegularFile(looseRef)
                    ? Files.readString(looseRef, StandardCharsets.UTF_8).trim()
                    : findPackedRef(gitRoot.resolve("packed-refs"), ref);
            return new CommitSha(sha);
        } catch (IOException | IllegalArgumentException exception) {
            throw new SkillPolicyUnavailableException("could not resolve pinned skill revision", exception);
        }
    }

    private String findPackedRef(Path packedRefs, String ref) throws IOException {
        if (!Files.isRegularFile(packedRefs)) {
            throw new SkillPolicyUnavailableException("skill branch revision is unavailable");
        }
        List<String> lines = Files.readAllLines(packedRefs, StandardCharsets.UTF_8);
        return lines.stream()
                .filter(line -> !line.startsWith("#") && !line.startsWith("^") && line.endsWith(" " + ref))
                .map(line -> line.substring(0, line.indexOf(' ')))
                .findFirst()
                .orElseThrow(() -> new SkillPolicyUnavailableException("skill branch revision is unavailable"));
    }
}
