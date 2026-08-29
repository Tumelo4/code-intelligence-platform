package com.codeintel.application.ports.outbound;

public interface SkillPort {
    String loadPinnedPolicyRevision(String requestedRevision);
}
