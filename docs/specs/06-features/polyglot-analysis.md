# FEATURE-POLYGLOT: Language-Neutral Analysis With Language-Aware Depth

## Status

Approved direction — implementation in progress. Passive language discovery and exact-revision language-neutral file evidence are delivered through repository inventory; Git-history joins and parser-aware analysis remain pending.

## Goal

Analyze polyglot repositories without treating Java as the product boundary. Every safely acquired text/code file should receive language-neutral file-level evidence, while language-specific parsers add deeper metrics and findings only where supported.

## Support levels

1. **Basic, all languages:** exact-revision file inventory, Git change frequency, churn, ownership concentration, and coupling for every eligible text/code file. Unknown extensions remain analyzable and are labeled `UNKNOWN`; binary, vendored, generated, and build-output files are explicitly excluded or marked as such.
2. **Language-aware:** parser-provided function/class boundaries, complexity, duplication, and language-specific findings for registered languages. Unsupported parsers do not make the entire repository fail and do not invent code-health scores.
3. **Automated refactoring:** available only for a separately declared, verified language/finding combination. Analysis support does not imply safe refactoring support.

This tiered model follows the distinction in [CodeScene's support guide](https://helpcenter.codescene.com/articles/5079181-which-programming-languages-does-codescene-support) between all-text file-level analysis and deeper language-aware analysis. It is a product-direction reference, not a claim that this platform implements CodeScene's proprietary metrics.

## Requirements

- REQ-POLY-001: Detect common languages deterministically from case-insensitive filenames/extensions, and preserve unknown text/code files at the basic level.
- REQ-POLY-002: Use bounded, read-only, symlink-safe file discovery from the exact immutable acquisition revision without executing repository code.
- REQ-POLY-003: Expose normalized repository-relative file identity, language, bytes, line count, and support level for eligible text files; exclude binary and categorized generated/vendor/output paths explicitly.
- REQ-POLY-004: Combine language-neutral file evidence with exact-revision Git history for every file, including those without a parser.
- REQ-POLY-005: Dispatch deeper analysis through language-specific ports. Keep JavaParser as the Java adapter and add other parsers incrementally, with clear unsupported/error states per language.
- REQ-POLY-006: Make scoring compare like support levels and never assign a deep code-health finding or automated-refactoring eligibility to a file without the required analyzer.
- REQ-POLY-007: Preserve deterministic ordering, privacy boundaries, persistence, and restart-safe API behavior across mixed-language repositories.

## Migration boundary

Milestones 5–8 describe historically verified Java-first capabilities and should not be rewritten as polyglot evidence. Existing Java analysis API and stored reports remain compatible while a new language-neutral file report is introduced. Git intelligence is already file-language-neutral; its aggregation can be reused. The Java 21 backend runtime remains an implementation choice, not a limit on analyzed source languages.

The inventory report now includes sorted `fileEvidence` entries: normalized relative file, detected language or `UNKNOWN`, byte count, UTF-8 physical line count, and `BASIC` or explicit exclusion status (`BINARY`, `TOO_LARGE`, `GENERATED`, `VENDORED`, `BUILD_OUTPUT`). Evidence is never derived by executing or parsing repository code. Existing stored inventory JSON without this field reads as an empty evidence list until reinventory runs.

## Acceptance

Mixed Java, TypeScript, Python, Go, Rust, C/C++, and unknown-text fixtures produce deterministic basic file evidence. A Java parser failure is isolated from basic evidence for non-Java files. No parser executes repository code or downloads dependencies. API output states the support level for every file, and file-level evidence survives restart. Deep findings appear only for languages with a registered, verified analyzer.
