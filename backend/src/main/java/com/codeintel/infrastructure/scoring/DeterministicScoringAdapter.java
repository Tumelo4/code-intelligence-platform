package com.codeintel.infrastructure.scoring;

import com.codeintel.application.ports.outbound.ScoringPort;
import com.codeintel.domain.analysis.AnalysisFinding;
import com.codeintel.domain.analysis.FindingType;
import com.codeintel.domain.analysis.JavaFileMetrics;
import com.codeintel.domain.analysis.StaticAnalysisResult;
import com.codeintel.domain.git.ChangeCoupling;
import com.codeintel.domain.git.FileHistory;
import com.codeintel.domain.git.GitIntelligenceResult;
import com.codeintel.domain.scoring.EligibilityReason;
import com.codeintel.domain.scoring.FileHotspot;
import com.codeintel.domain.scoring.FindingPriority;
import com.codeintel.domain.scoring.ScoringReport;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class DeterministicScoringAdapter implements ScoringPort {
    private static final int ELIGIBILITY_THRESHOLD = 50;
    private final Set<FindingType> supportedFindingTypes;

    public DeterministicScoringAdapter(Set<FindingType> supportedFindingTypes) {
        if (supportedFindingTypes == null) {
            throw new IllegalArgumentException("supported finding types are required");
        }
        this.supportedFindingTypes = Set.copyOf(supportedFindingTypes);
    }

    @Override
    public ScoringReport score(StaticAnalysisResult analysis, GitIntelligenceResult gitIntelligence) {
        validateIdentity(analysis, gitIntelligence);
        Map<String, JavaFileMetrics> metrics = unique(analysis.report().files(), JavaFileMetrics::file,
                "duplicate static file metrics");
        Map<String, FileHistory> histories = unique(gitIntelligence.report().files(), FileHistory::file,
                "duplicate Git file history");
        validateFindings(analysis.report().findings());

        long maximumCommits = metrics.keySet().stream().map(histories::get).filter(java.util.Objects::nonNull)
                .mapToLong(FileHistory::commitCount).max().orElse(0);
        long maximumChurn = metrics.keySet().stream().map(histories::get).filter(java.util.Objects::nonNull)
                .mapToLong(DeterministicScoringAdapter::churn).max().orElse(0);
        Map<String, Integer> staticRisk = staticRisk(analysis.report().findings());
        Map<String, Integer> coupling = coupling(gitIntelligence.report().couplings());

        List<FileHotspot> hotspots = metrics.keySet().stream()
                .map(file -> hotspot(file, histories.get(file), staticRisk.getOrDefault(file, 0),
                        coupling.getOrDefault(file, 0), maximumCommits, maximumChurn))
                .sorted(Comparator.comparingInt(FileHotspot::score).reversed()
                        .thenComparing(FileHotspot::file))
                .toList();
        Map<String, FileHotspot> hotspotsByFile = hotspots.stream()
                .collect(Collectors.toMap(FileHotspot::file, Function.identity()));
        List<FindingPriority> priorities = priorities(analysis.report().findings(), metrics.keySet(),
                hotspotsByFile, gitIntelligence.report().historyTruncated());
        return new ScoringReport(health(hotspots), hotspots, priorities);
    }

    private static void validateIdentity(StaticAnalysisResult analysis, GitIntelligenceResult git) {
        if (analysis == null || git == null || !analysis.repositoryId().equals(git.repositoryId())
                || !analysis.acquisitionRevision().equals(git.acquisitionRevision())) {
            throw new ScoringSafetyException("scoring inputs must share repository and exact revision");
        }
    }

    private static <T> Map<String, T> unique(List<T> values, Function<T, String> key,
            String message) {
        Map<String, T> result = new HashMap<>();
        for (T value : values) {
            String name = key.apply(value);
            if (!safeRelativeFile(name) || result.putIfAbsent(name, value) != null) {
                throw new ScoringSafetyException(message);
            }
        }
        return result;
    }

    private static void validateFindings(List<AnalysisFinding> findings) {
        Set<String> ids = new HashSet<>();
        for (AnalysisFinding finding : findings) {
            severity(finding.severity());
            confidence(finding.confidence());
            if (!safeRelativeFile(finding.file()) || !ids.add(finding.id())) {
                throw new ScoringSafetyException("findings must have unique IDs and safe files");
            }
        }
    }

    private static Map<String, Integer> staticRisk(List<AnalysisFinding> findings) {
        Map<String, Integer> result = new HashMap<>();
        for (AnalysisFinding finding : findings) {
            result.compute(finding.file(), (file, score) -> Math.min(100,
                    Math.addExact(score == null ? 0 : score, findingRisk(finding))));
        }
        return result;
    }

    private static Map<String, Integer> coupling(List<ChangeCoupling> couplings) {
        Map<String, Integer> result = new HashMap<>();
        for (ChangeCoupling value : couplings) {
            int score = BigDecimal.valueOf(value.strength()).movePointRight(2)
                    .setScale(0, RoundingMode.HALF_UP).intValueExact();
            result.merge(value.firstFile(), score, Math::max);
            result.merge(value.secondFile(), score, Math::max);
        }
        return result;
    }

    private static FileHotspot hotspot(String file, FileHistory history, int staticRisk,
            int coupling, long maximumCommits, long maximumChurn) {
        int activity = history == null ? 0 : percent(history.commitCount(), maximumCommits);
        int churn = history == null ? 0 : percent(churn(history), maximumChurn);
        int ownership = history == null ? 0 : percent(history.authors().stream()
                .mapToLong(author -> author.commits()).max().orElse(0), history.commitCount());
        int score = weighted(staticRisk * 45L + activity * 25L + churn * 15L
                + ownership * 10L + coupling * 5L, 100);
        return new FileHotspot(file, score, staticRisk, activity, churn, ownership, coupling);
    }

    private List<FindingPriority> priorities(List<AnalysisFinding> findings, Set<String> metricFiles,
            Map<String, FileHotspot> hotspots, boolean historyTruncated) {
        List<RankedFinding> ranked = new ArrayList<>();
        for (AnalysisFinding finding : findings) {
            int hotspot = hotspots.getOrDefault(finding.file(),
                    new FileHotspot(finding.file(), 0, 0, 0, 0, 0, 0)).score();
            int score = weighted(hotspot * 70L + findingRisk(finding) * 30L, 100);
            EnumSet<EligibilityReason> reasons = EnumSet.noneOf(EligibilityReason.class);
            if (confidence(finding.confidence()) < 75) reasons.add(EligibilityReason.LOW_CONFIDENCE);
            if (score < ELIGIBILITY_THRESHOLD) reasons.add(EligibilityReason.LOW_PRIORITY);
            if (!metricFiles.contains(finding.file())) reasons.add(EligibilityReason.MISSING_FILE_METRICS);
            if (historyTruncated) reasons.add(EligibilityReason.TRUNCATED_HISTORY);
            if (!supportedFindingTypes.contains(finding.type())) {
                reasons.add(EligibilityReason.UNSUPPORTED_FINDING_TYPE);
            }
            ranked.add(new RankedFinding(finding, score, List.copyOf(reasons)));
        }
        ranked.sort(Comparator.comparingInt(RankedFinding::score).reversed()
                .thenComparing(value -> value.finding().file())
                .thenComparingInt(value -> value.finding().range().startLine())
                .thenComparing(value -> value.finding().type().name())
                .thenComparing(value -> value.finding().id()));
        List<FindingPriority> result = new ArrayList<>();
        for (int index = 0; index < ranked.size(); index++) {
            RankedFinding value = ranked.get(index);
            result.add(new FindingPriority(value.finding().id(), value.finding().file(), value.score(),
                    index + 1, value.reasons().isEmpty(), value.reasons()));
        }
        return List.copyOf(result);
    }

    private static int health(List<FileHotspot> hotspots) {
        if (hotspots.isEmpty()) return 100;
        int count = Math.min(10, hotspots.size());
        long total = hotspots.subList(0, count).stream().mapToLong(FileHotspot::score).sum();
        return 100 - weighted(total, count);
    }

    private static int findingRisk(AnalysisFinding finding) {
        return weighted((long) severity(finding.severity()) * confidence(finding.confidence()), 100);
    }

    private static int severity(String value) {
        return switch (normalized(value, "severity")) {
            case "CRITICAL" -> 100;
            case "HIGH" -> 75;
            case "MEDIUM" -> 50;
            case "LOW" -> 25;
            default -> throw new ScoringSafetyException("unknown finding severity");
        };
    }

    private static int confidence(String value) {
        return switch (normalized(value, "confidence")) {
            case "HIGH" -> 100;
            case "MEDIUM" -> 75;
            case "LOW" -> 50;
            default -> throw new ScoringSafetyException("unknown finding confidence");
        };
    }

    private static String normalized(String value, String field) {
        if (value == null) throw new ScoringSafetyException("finding " + field + " is required");
        return value.strip().toUpperCase(Locale.ROOT);
    }

    private static long churn(FileHistory history) {
        return Math.addExact((long) history.linesAdded(), history.linesDeleted());
    }

    private static int percent(long numerator, long denominator) {
        if (denominator == 0) return 0;
        return BigDecimal.valueOf(numerator).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 0, RoundingMode.HALF_UP).intValueExact();
    }

    private static int weighted(long numerator, int denominator) {
        return BigDecimal.valueOf(numerator).divide(BigDecimal.valueOf(denominator), 0,
                RoundingMode.HALF_UP).intValueExact();
    }

    private static boolean safeRelativeFile(String file) {
        return file != null && !file.isBlank() && !file.startsWith("/") && !file.contains("\\")
                && !file.equals("..") && !file.startsWith("../") && !file.endsWith("/..")
                && !file.contains("/../");
    }

    private record RankedFinding(AnalysisFinding finding, int score,
            List<EligibilityReason> reasons) { }
}
