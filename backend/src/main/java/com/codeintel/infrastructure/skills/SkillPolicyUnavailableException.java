package com.codeintel.infrastructure.skills;

public final class SkillPolicyUnavailableException extends RuntimeException {
    public SkillPolicyUnavailableException(String message) {
        super(message);
    }

    public SkillPolicyUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
