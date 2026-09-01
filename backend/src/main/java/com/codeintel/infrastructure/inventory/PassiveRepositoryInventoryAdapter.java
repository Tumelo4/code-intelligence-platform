package com.codeintel.infrastructure.inventory;

import com.codeintel.application.ports.outbound.RepositoryInventoryPort;
import com.codeintel.domain.inventory.InventoryReport;
import com.codeintel.domain.inventory.MavenPluginDescriptor;
import com.codeintel.domain.inventory.MavenProjectDescriptor;
import com.codeintel.domain.inventory.RepositoryPathInventory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.helpers.DefaultHandler;

public final class PassiveRepositoryInventoryAdapter implements RepositoryInventoryPort {
    private static final Set<String> MANIFESTS = Set.of("pom.xml", "build.gradle", "build.gradle.kts",
            "settings.gradle", "settings.gradle.kts", "package.json", "pyproject.toml", "go.mod");
    private static final Set<String> LOCKFILES = Set.of("package-lock.json", "yarn.lock", "pnpm-lock.yaml",
            "gradle.lockfile", "poetry.lock", "cargo.lock", "go.sum");
    private static final Set<String> SCRIPT_EXTENSIONS = Set.of("sh", "bash", "zsh", "cmd", "bat", "ps1");
    private static final Set<String> GENERATED = Set.of("generated", "generated-sources", "generated-test-sources",
            ".apt_generated");
    private static final Set<String> VENDORED = Set.of("vendor", "vendors", "node_modules");
    private static final Set<String> BUILD_OUTPUT = Set.of("target", "build", "out", "dist");

    private final InventoryLimits limits;

    public PassiveRepositoryInventoryAdapter(InventoryLimits limits) {
        this.limits = limits;
    }

    @Override
    public InventoryReport inspect(Path immutableOriginal) {
        try {
            Path root = immutableOriginal.toRealPath();
            if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
                throw new InventorySafetyException("inventory root is not a directory");
            }
            MutablePaths categories = new MutablePaths();
            Set<String> languages = new LinkedHashSet<>();
            Set<String> buildSystems = new LinkedHashSet<>();
            int fileCount = inspectFiles(root, categories, languages, buildSystems);
            List<MavenProjectDescriptor> projects = inspectMaven(root, categories);
            if (!projects.isEmpty()) buildSystems.add("MAVEN");
            return new InventoryReport(sorted(languages), sorted(buildSystems), categories.freeze(),
                    projects, fileCount);
        } catch (InventorySafetyException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new InventorySafetyException("repository inventory failed safely", exception);
        }
    }

    private int inspectFiles(Path root, MutablePaths paths, Set<String> languages,
            Set<String> buildSystems) throws IOException {
        int count = 0;
        try (var stream = Files.walk(root)) {
            for (Path path : stream.sorted(Comparator.comparing(Path::toString)).toList()) {
                if (path.equals(root)) continue;
                if (Files.isSymbolicLink(path)) {
                    throw new InventorySafetyException("repository contains a symbolic link");
                }
                String relative = relative(root, path);
                String name = path.getFileName().toString();
                String lower = name.toLowerCase(Locale.ROOT);
                if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                    categorizeDirectory(relative, lower, paths);
                    continue;
                }
                if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || ++count > limits.maximumFiles()) {
                    throw new InventorySafetyException("repository type or file count is unsafe");
                }
                if (lower.endsWith(".java")) languages.add("JAVA");
                else if (lower.endsWith(".kt")) languages.add("KOTLIN");
                else if (lower.endsWith(".js") || lower.endsWith(".jsx")) languages.add("JAVASCRIPT");
                else if (lower.endsWith(".ts") || lower.endsWith(".tsx")) languages.add("TYPESCRIPT");
                else if (lower.endsWith(".py")) languages.add("PYTHON");
                if (lower.equals("pom.xml")) buildSystems.add("MAVEN");
                else if (lower.startsWith("build.gradle") || lower.startsWith("settings.gradle")) {
                    buildSystems.add("GRADLE");
                }
                categorizeFile(relative, lower, paths);
            }
        }
        return count;
    }

    private List<MavenProjectDescriptor> inspectMaven(Path root, MutablePaths paths) throws Exception {
        ArrayDeque<Path> pending = new ArrayDeque<>();
        try (var files = Files.walk(root)) {
            files.filter(path -> path.getFileName() != null
                            && path.getFileName().toString().equals("pom.xml"))
                    .sorted(Comparator.comparing(Path::toString)).forEach(pending::add);
        }
        if (pending.isEmpty()) return List.of();
        Set<Path> visited = new LinkedHashSet<>();
        List<MavenProjectDescriptor> projects = new ArrayList<>();
        while (!pending.isEmpty()) {
            Path pom = pending.removeFirst().normalize();
            if (!pom.startsWith(root) || !visited.add(pom)) continue;
            if (visited.size() > limits.maximumModules()) {
                throw new InventorySafetyException("Maven module count exceeds configured limit");
            }
            ParsedPom parsed = parsePom(root, pom);
            projects.add(parsed.descriptor());
            addRoot(root, pom.getParent(), parsed.sourceDirectory(), paths.sourceRoots);
            addRoot(root, pom.getParent(), parsed.testSourceDirectory(), paths.testRoots);
            for (String module : parsed.descriptor().modules()) {
                Path modulePath = safeDeclaredPath(root, pom.getParent(), module);
                Path modulePom = modulePath.getFileName() != null
                        && modulePath.getFileName().toString().equals("pom.xml")
                        ? modulePath : modulePath.resolve("pom.xml");
                if (!Files.exists(modulePom, LinkOption.NOFOLLOW_LINKS)) {
                    throw new InventorySafetyException("declared Maven module POM is missing");
                }
                pending.add(modulePom);
            }
        }
        projects.sort(Comparator.comparing(MavenProjectDescriptor::pomPath));
        return List.copyOf(projects);
    }

    private ParsedPom parsePom(Path root, Path pom) throws Exception {
        if (Files.isSymbolicLink(pom) || !Files.isRegularFile(pom, LinkOption.NOFOLLOW_LINKS)
                || Files.size(pom) > limits.maximumPomBytes()) {
            throw new InventorySafetyException("Maven POM is unsafe");
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        Document document;
        try (InputStream input = Files.newInputStream(pom)) {
            var builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new DefaultHandler());
            document = builder.parse(input);
        }
        Element project = document.getDocumentElement();
        String groupId = text(project, "groupId");
        String version = text(project, "version");
        Element parent = child(project, "parent");
        if (groupId == null && parent != null) groupId = text(parent, "groupId");
        if (version == null && parent != null) version = text(parent, "version");
        String artifactId = text(project, "artifactId");
        if (artifactId == null) throw new InventorySafetyException("Maven artifactId is missing");
        Element properties = child(project, "properties");
        String javaVersion = firstNonNull(properties == null ? null : text(properties, "maven.compiler.release"),
                properties == null ? null : text(properties, "maven.compiler.source"),
                properties == null ? null : text(properties, "maven.compiler.target"),
                properties == null ? null : text(properties, "java.version"));
        List<String> modules = texts(child(project, "modules"), "module");
        List<String> dependencies = dependencies(child(project, "dependencies"));
        Element build = child(project, "build");
        List<MavenPluginDescriptor> plugins = new ArrayList<>(
                plugins(build == null ? null : child(build, "plugins")));
        Element pluginManagement = build == null ? null : child(build, "pluginManagement");
        plugins.addAll(plugins(pluginManagement == null ? null : child(pluginManagement, "plugins")));
        plugins = plugins.stream().distinct()
                .sorted(Comparator.comparing(MavenPluginDescriptor::artifactId)).toList();
        boolean surefire = plugins.stream().anyMatch(p -> p.artifactId().equals("maven-surefire-plugin"));
        boolean failsafe = plugins.stream().anyMatch(p -> p.artifactId().equals("maven-failsafe-plugin"));
        String source = build == null ? null : text(build, "sourceDirectory");
        String testSource = build == null ? null : text(build, "testSourceDirectory");
        MavenProjectDescriptor descriptor = new MavenProjectDescriptor(relative(root, pom), groupId,
                artifactId, version, firstNonNull(text(project, "packaging"), "jar"), javaVersion,
                sorted(modules), dependencies, plugins, surefire, failsafe);
        return new ParsedPom(descriptor, firstNonNull(source, "src/main/java"),
                firstNonNull(testSource, "src/test/java"));
    }

    private static List<String> dependencies(Element dependencies) {
        List<String> values = new ArrayList<>();
        for (Element dependency : children(dependencies, "dependency")) {
            String artifact = text(dependency, "artifactId");
            if (artifact != null) values.add(firstNonNull(text(dependency, "groupId"), "") + ":" + artifact);
        }
        return sorted(values);
    }

    private static List<MavenPluginDescriptor> plugins(Element plugins) {
        List<MavenPluginDescriptor> values = new ArrayList<>();
        for (Element plugin : children(plugins, "plugin")) {
            String artifact = text(plugin, "artifactId");
            if (artifact == null) continue;
            List<String> ids = new ArrayList<>();
            List<String> goals = new ArrayList<>();
            Element executions = child(plugin, "executions");
            for (Element execution : children(executions, "execution")) {
                String id = text(execution, "id");
                if (id != null) ids.add(id);
                goals.addAll(texts(child(execution, "goals"), "goal"));
            }
            values.add(new MavenPluginDescriptor(firstNonNull(text(plugin, "groupId"),
                    "org.apache.maven.plugins"), artifact, text(plugin, "version"), sorted(ids), sorted(goals)));
        }
        values.sort(Comparator.comparing(MavenPluginDescriptor::artifactId));
        return List.copyOf(values);
    }

    private static void addRoot(Path root, Path module, String declared, Set<String> output) {
        if (declared == null || declared.contains("${")) return;
        Path candidate = safeDeclaredPath(root, module, declared);
        if (Files.isDirectory(candidate, LinkOption.NOFOLLOW_LINKS)) output.add(relative(root, candidate));
    }

    private static Path safeDeclaredPath(Path root, Path base, String declared) {
        if (declared == null || declared.isBlank()) {
            throw new InventorySafetyException("declared Maven path is blank");
        }
        Path value = Path.of(declared);
        Path resolved = base.resolve(value).normalize();
        if (value.isAbsolute() || !resolved.startsWith(root)) {
            throw new InventorySafetyException("declared Maven path escapes repository");
        }
        return resolved;
    }

    private static void categorizeDirectory(String relative, String lower, MutablePaths paths) {
        if (GENERATED.contains(lower)) paths.generated.add(relative);
        if (VENDORED.contains(lower)) paths.vendored.add(relative);
        if (BUILD_OUTPUT.contains(lower)) paths.buildOutput.add(relative);
        if (lower.equals("migrations") || lower.equals("migration")) paths.migrations.add(relative);
    }

    private static void categorizeFile(String relative, String lower, MutablePaths paths) {
        if (MANIFESTS.contains(lower)) paths.manifests.add(relative);
        if (LOCKFILES.contains(lower)) paths.lockfiles.add(relative);
        int dot = lower.lastIndexOf('.');
        if ((dot >= 0 && SCRIPT_EXTENSIONS.contains(lower.substring(dot + 1)))
                || lower.equals("mvnw") || lower.equals("gradlew")) paths.scripts.add(relative);
        if (relative.startsWith(".github/workflows/") || lower.equals(".gitlab-ci.yml")
                || lower.equals("jenkinsfile") || lower.equals("azure-pipelines.yml")) {
            paths.ci.add(relative);
        }
        if (lower.equals("dockerfile") || lower.startsWith("dockerfile.")
                || lower.startsWith("docker-compose") || lower.startsWith("compose.")) {
            paths.docker.add(relative);
        }
        if (relative.contains("/migration/") || relative.contains("/migrations/")) paths.migrations.add(relative);
        if (lower.startsWith("readme") || lower.endsWith(".md") || lower.endsWith(".adoc")
                || relative.startsWith("docs/")) paths.documentation.add(relative);
    }

    private static Element child(Element parent, String name) {
        if (parent == null) return null;
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element element && name.equals(element.getLocalName())) return element;
        }
        return null;
    }

    private static List<Element> children(Element parent, String name) {
        List<Element> values = new ArrayList<>();
        if (parent == null) return values;
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element element && name.equals(element.getLocalName())) values.add(element);
        }
        return values;
    }

    private static String text(Element parent, String name) {
        Element child = child(parent, name);
        if (child == null) return null;
        String value = child.getTextContent().trim();
        return value.isEmpty() ? null : value;
    }

    private static List<String> texts(Element parent, String name) {
        List<String> values = new ArrayList<>();
        for (Element element : children(parent, name)) {
            String value = element.getTextContent().trim();
            if (!value.isEmpty()) values.add(value);
        }
        return values;
    }

    private static String relative(Path root, Path path) {
        String value = root.relativize(path).toString().replace('\\', '/');
        return value.isEmpty() ? "." : value;
    }

    private static String firstNonNull(String... values) {
        for (String value : values) if (value != null) return value;
        return null;
    }

    private static List<String> sorted(java.util.Collection<String> values) {
        return values.stream().distinct().sorted().toList();
    }

    private record ParsedPom(MavenProjectDescriptor descriptor, String sourceDirectory,
            String testSourceDirectory) {
    }

    private static final class MutablePaths {
        private final Set<String> sourceRoots = new LinkedHashSet<>();
        private final Set<String> testRoots = new LinkedHashSet<>();
        private final Set<String> manifests = new LinkedHashSet<>();
        private final Set<String> lockfiles = new LinkedHashSet<>();
        private final Set<String> scripts = new LinkedHashSet<>();
        private final Set<String> ci = new LinkedHashSet<>();
        private final Set<String> docker = new LinkedHashSet<>();
        private final Set<String> migrations = new LinkedHashSet<>();
        private final Set<String> documentation = new LinkedHashSet<>();
        private final Set<String> generated = new LinkedHashSet<>();
        private final Set<String> vendored = new LinkedHashSet<>();
        private final Set<String> buildOutput = new LinkedHashSet<>();

        private RepositoryPathInventory freeze() {
            return new RepositoryPathInventory(sorted(sourceRoots), sorted(testRoots), sorted(manifests),
                    sorted(lockfiles), sorted(scripts), sorted(ci), sorted(docker), sorted(migrations),
                    sorted(documentation), sorted(generated), sorted(vendored), sorted(buildOutput));
        }
    }
}
