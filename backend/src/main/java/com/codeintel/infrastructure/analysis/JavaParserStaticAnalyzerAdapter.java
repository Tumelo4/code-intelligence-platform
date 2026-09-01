package com.codeintel.infrastructure.analysis;

import com.codeintel.application.ports.outbound.StaticAnalyzerPort;
import com.codeintel.domain.acquisition.AcquisitionRevision;
import com.codeintel.domain.analysis.AnalysisFinding;
import com.codeintel.domain.analysis.AnalysisReport;
import com.codeintel.domain.analysis.FindingType;
import com.codeintel.domain.analysis.JavaClassMetrics;
import com.codeintel.domain.analysis.JavaFileMetrics;
import com.codeintel.domain.analysis.JavaMethodMetrics;
import com.codeintel.domain.analysis.SourceRange;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.WhileStmt;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class JavaParserStaticAnalyzerAdapter implements StaticAnalyzerPort {
    private final AnalysisThresholds thresholds;
    private final JavaParser parser;

    public JavaParserStaticAnalyzerAdapter(AnalysisThresholds thresholds) {
        this.thresholds = thresholds;
        this.parser = new JavaParser(new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE));
    }

    @Override
    public AnalysisReport analyze(Path immutableOriginal, List<String> sourceRoots,
            AcquisitionRevision revision) {
        try {
            Path root = immutableOriginal.toRealPath();
            List<Path> files = discover(root, sourceRoots);
            List<JavaFileMetrics> metrics = new ArrayList<>();
            List<MethodContext> methods = new ArrayList<>();
            List<ClassContext> classes = new ArrayList<>();
            for (Path file : files) parse(root, file, metrics, methods, classes);
            List<AnalysisFinding> findings = findings(revision, methods, classes);
            metrics.sort(Comparator.comparing(JavaFileMetrics::file));
            findings.sort(Comparator.comparing(AnalysisFinding::file)
                    .thenComparing(f -> f.range().startLine()).thenComparing(f -> f.type().name())
                    .thenComparing(AnalysisFinding::id));
            return new AnalysisReport(metrics, findings);
        } catch (StaticAnalysisSafetyException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new StaticAnalysisSafetyException("Java static analysis failed safely", exception);
        }
    }

    private List<Path> discover(Path root, List<String> roots) throws Exception {
        Set<Path> files = new LinkedHashSet<>();
        for (String declared : roots) {
            Path value = Path.of(declared);
            Path source = root.resolve(value).normalize();
            if (value.isAbsolute() || !source.startsWith(root) || !Files.isDirectory(source, LinkOption.NOFOLLOW_LINKS)) {
                throw new StaticAnalysisSafetyException("Java source root is unsafe");
            }
            try (var paths = Files.walk(source)) {
                for (Path path : paths.sorted().toList()) {
                    if (Files.isSymbolicLink(path)) throw new StaticAnalysisSafetyException("Java source contains a symbolic link");
                    if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                            && path.getFileName().toString().endsWith(".java")) {
                        if (Files.size(path) > thresholds.maximumFileBytes())
                            throw new StaticAnalysisSafetyException("Java source exceeds size limit");
                        if (!files.add(path) || files.size() > thresholds.maximumFiles())
                            throw new StaticAnalysisSafetyException("Java source count is unsafe");
                    }
                }
            }
        }
        return files.stream().sorted().toList();
    }

    private void parse(Path root, Path file, List<JavaFileMetrics> output,
            List<MethodContext> methods, List<ClassContext> classes) throws Exception {
        var result = parser.parse(file);
        if (!result.isSuccessful() || result.getResult().isEmpty())
            throw new StaticAnalysisSafetyException("Java source could not be parsed: " + relative(root, file));
        var unit = result.getResult().orElseThrow();
        String relative = relative(root, file);
        List<JavaClassMetrics> fileClasses = new ArrayList<>();
        for (TypeDeclaration<?> type : unit.findAll(TypeDeclaration.class)) {
            SourceRange range = range(type);
            List<JavaMethodMetrics> typeMethods = new ArrayList<>();
            for (CallableDeclaration<?> callable : type.getMembers().stream()
                    .filter(member -> member instanceof MethodDeclaration || member instanceof ConstructorDeclaration)
                    .map(member -> (CallableDeclaration<?>) member).toList()) {
                JavaMethodMetrics metric = methodMetrics(callable);
                typeMethods.add(metric);
                methods.add(new MethodContext(relative, type.getNameAsString(), metric, fingerprint(callable)));
            }
            JavaClassMetrics metric = new JavaClassMetrics(type.getNameAsString(), range, range.lineCount(),
                    type.getFields().stream().mapToInt(field -> field.getVariables().size()).sum(), typeMethods);
            fileClasses.add(metric);
            classes.add(new ClassContext(relative, metric));
        }
        int methodCount = fileClasses.stream().mapToInt(value -> value.methods().size()).sum();
        int loc = range(unit).lineCount();
        int dependencies = (int) unit.getImports().stream().map(Object::toString).distinct().count();
        output.add(new JavaFileMetrics(relative, loc, fileClasses.size(), methodCount, dependencies, fileClasses));
    }

    private JavaMethodMetrics methodMetrics(CallableDeclaration<?> callable) {
        SourceRange range = range(callable);
        int branches = callable.findAll(IfStmt.class).size() + callable.findAll(SwitchEntry.class).size()
                + callable.findAll(ConditionalExpr.class).size() + callable.findAll(CatchClause.class).size();
        int loops = callable.findAll(ForStmt.class).size() + callable.findAll(ForEachStmt.class).size()
                + callable.findAll(WhileStmt.class).size() + callable.findAll(DoStmt.class).size();
        int booleanBranches = (int) callable.findAll(BinaryExpr.class).stream()
                .filter(value -> value.getOperator() == BinaryExpr.Operator.AND
                        || value.getOperator() == BinaryExpr.Operator.OR).count();
        return new JavaMethodMetrics(callable.getNameAsString(), range, range.lineCount(),
                1 + branches + loops + booleanBranches, maxNesting(callable, 0),
                callable.getParameters().size(), branches, loops);
    }

    private static int maxNesting(Node node, int depth) {
        int next = isControl(node) ? depth + 1 : depth;
        int maximum = next;
        for (Node child : node.getChildNodes()) maximum = Math.max(maximum, maxNesting(child, next));
        return maximum;
    }

    private static boolean isControl(Node node) {
        return node instanceof IfStmt || node instanceof ForStmt || node instanceof ForEachStmt
                || node instanceof WhileStmt || node instanceof DoStmt || node instanceof SwitchEntry
                || node instanceof CatchClause || node instanceof ConditionalExpr;
    }

    private List<AnalysisFinding> findings(AcquisitionRevision revision, List<MethodContext> methods,
            List<ClassContext> classes) throws Exception {
        List<AnalysisFinding> values = new ArrayList<>();
        for (MethodContext context : methods) {
            var metric = context.metric();
            if (metric.loc() > thresholds.longMethodLoc()) values.add(finding(revision, FindingType.LONG_METHOD, context.file(), metric.range(), "method LOC=" + metric.loc(), context.owner() + "." + metric.name()));
            if (metric.cyclomaticComplexity() > thresholds.highComplexity()) values.add(finding(revision, FindingType.HIGH_COMPLEXITY, context.file(), metric.range(), "cyclomatic complexity=" + metric.cyclomaticComplexity(), context.owner() + "." + metric.name()));
            if (metric.nestingDepth() > thresholds.deepNesting()) values.add(finding(revision, FindingType.DEEP_NESTING, context.file(), metric.range(), "nesting depth=" + metric.nestingDepth(), context.owner() + "." + metric.name()));
            if (metric.parameterCount() > thresholds.manyParameters()) values.add(finding(revision, FindingType.TOO_MANY_PARAMETERS, context.file(), metric.range(), "parameter count=" + metric.parameterCount(), context.owner() + "." + metric.name()));
        }
        for (ClassContext context : classes) {
            var metric = context.metric();
            if (metric.loc() > thresholds.largeClassLoc() || metric.methods().size() > thresholds.largeClassMethods()) values.add(finding(revision, FindingType.LARGE_CLASS, context.file(), metric.range(), "class LOC=" + metric.loc() + ", methods=" + metric.methods().size(), metric.name()));
            if (metric.loc() > thresholds.godClassLoc() && metric.methods().size() > thresholds.godClassMethods() && metric.fieldCount() > thresholds.godClassFields()) values.add(finding(revision, FindingType.GOD_CLASS, context.file(), metric.range(), "class LOC=" + metric.loc() + ", methods=" + metric.methods().size() + ", fields=" + metric.fieldCount(), metric.name()));
        }
        Map<String, List<MethodContext>> duplicates = new HashMap<>();
        methods.stream().filter(value -> value.fingerprint() != null)
                .forEach(value -> duplicates.computeIfAbsent(value.fingerprint(), ignored -> new ArrayList<>()).add(value));
        for (var group : duplicates.values()) if (group.size() > 1) {
            group.sort(Comparator.comparing(MethodContext::file).thenComparing(value -> value.metric().range().startLine()));
            String peers = group.stream().map(value -> value.file() + ":" + value.metric().range().startLine()).reduce((a, b) -> a + ", " + b).orElseThrow();
            for (MethodContext context : group) values.add(finding(revision, FindingType.DUPLICATED_LOGIC,
                    context.file(), context.metric().range(), "matching normalized statements at " + peers,
                    context.owner() + "." + context.metric().name()));
        }
        return values;
    }

    private AnalysisFinding finding(AcquisitionRevision revision, FindingType type, String file,
            SourceRange range, String evidence, String area) throws Exception {
        String id = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(
                (revision.kind() + "|" + revision.value() + "|" + type + "|" + file + "|"
                        + range.startLine() + "|" + range.endLine() + "|" + evidence)
                        .getBytes(StandardCharsets.UTF_8))).substring(0, 24);
        return new AnalysisFinding(id, type.name().replace('_', ' '), type, area, "MEDIUM", "HIGH",
                file, range, evidence, "Deterministic threshold exceeded",
                "This structure increases maintenance and change risk", "Refactor into smaller cohesive units",
                "MEDIUM", 50);
    }

    private String fingerprint(CallableDeclaration<?> callable) {
        List<String> statements = callable.findAll(Statement.class).stream()
                .map(value -> value.getClass().getSimpleName()).toList();
        return statements.size() < thresholds.duplicateStatements() ? null : String.join("|", statements);
    }

    private static SourceRange range(Node node) {
        var value = node.getRange().orElseThrow(() -> new StaticAnalysisSafetyException("AST range is missing"));
        return new SourceRange(value.begin.line, value.end.line);
    }

    private static String relative(Path root, Path file) { return root.relativize(file).toString().replace('\\', '/'); }
    private record MethodContext(String file, String owner, JavaMethodMetrics metric, String fingerprint) { }
    private record ClassContext(String file, JavaClassMetrics metric) { }
}
