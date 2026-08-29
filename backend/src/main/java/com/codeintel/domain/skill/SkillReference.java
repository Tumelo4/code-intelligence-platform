package com.codeintel.domain.skill;

public enum SkillReference {
    SKILL("SKILL.md", true),
    RUNTIME_SAFETY("references/runtime-safety.md", true),
    REVIEW_CHECKLISTS("references/review-checklists.md", false),
    LARGE_REPOSITORY("references/large-repository.md", false),
    REPORT_TEMPLATE("references/report-template.md", false);

    private final String path;
    private final boolean required;

    SkillReference(String path, boolean required) {
        this.path = path;
        this.required = required;
    }

    public String path() {
        return path;
    }

    public boolean required() {
        return required;
    }
}
