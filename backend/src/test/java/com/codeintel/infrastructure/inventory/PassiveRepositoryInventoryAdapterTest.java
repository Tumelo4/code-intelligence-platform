package com.codeintel.infrastructure.inventory;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PassiveRepositoryInventoryAdapterTest {
    @TempDir
    Path temporary;

    @Test
    void inventoriesJavaMavenModulesMetadataAndRepositoryCategories() throws Exception {
        Files.createDirectories(temporary.resolve("src/main/java/example"));
        Files.createDirectories(temporary.resolve("src/test/java/example"));
        Files.createDirectories(temporary.resolve("module/src/main/java/example"));
        Files.createDirectories(temporary.resolve("module/src/test/java/example"));
        Files.createDirectories(temporary.resolve(".github/workflows"));
        Files.createDirectories(temporary.resolve("src/main/resources/db/migration"));
        Files.createDirectories(temporary.resolve("target/generated-sources"));
        Files.createDirectories(temporary.resolve("vendor"));
        Files.writeString(temporary.resolve("src/main/java/example/App.java"), "class App {}");
        Files.writeString(temporary.resolve("module/src/main/java/example/Module.java"), "class Module {}");
        Files.writeString(temporary.resolve(".github/workflows/build.yml"), "name: build");
        Files.writeString(temporary.resolve("src/main/resources/db/migration/V1__init.sql"), "select 1;");
        Files.writeString(temporary.resolve("build.sh"), "#!/bin/sh");
        Files.writeString(temporary.resolve("Dockerfile"), "FROM scratch");
        Files.writeString(temporary.resolve("README.md"), "docs");
        Files.writeString(temporary.resolve("pom.xml"), rootPom());
        Files.writeString(temporary.resolve("module/pom.xml"), modulePom());

        var report = adapter().inspect(temporary);

        assertThat(report.languages()).containsExactly("JAVA");
        assertThat(report.buildSystems()).containsExactly("MAVEN");
        assertThat(report.paths().sourceRoots()).containsExactly("module/src/main/java", "src/main/java");
        assertThat(report.paths().testRoots()).containsExactly("module/src/test/java", "src/test/java");
        assertThat(report.paths().scripts()).containsExactly("build.sh");
        assertThat(report.paths().ciConfiguration()).containsExactly(".github/workflows/build.yml");
        assertThat(report.paths().dockerFiles()).containsExactly("Dockerfile");
        assertThat(report.paths().migrations()).contains("src/main/resources/db/migration/V1__init.sql");
        assertThat(report.paths().generatedDirectories()).contains("target/generated-sources");
        assertThat(report.paths().vendoredDirectories()).containsExactly("vendor");
        assertThat(report.paths().buildOutputDirectories()).containsExactly("target");
        assertThat(report.mavenProjects()).hasSize(2);
        var root = report.mavenProjects().stream().filter(project -> project.pomPath().equals("pom.xml"))
                .findFirst().orElseThrow();
        assertThat(root.groupId()).isEqualTo("example");
        assertThat(root.artifactId()).isEqualTo("root");
        assertThat(root.javaVersion()).isEqualTo("21");
        assertThat(root.modules()).containsExactly("module");
        assertThat(root.dependencies()).containsExactly("org.example:library");
        assertThat(root.surefireDeclared()).isTrue();
        assertThat(root.failsafeDeclared()).isTrue();
        assertThat(root.plugins()).extracting("artifactId")
                .containsExactly("maven-failsafe-plugin", "maven-surefire-plugin");
        assertThat(root.plugins().getFirst().executionIds()).containsExactly("integration-tests");
        assertThat(root.plugins().getFirst().goals()).containsExactly("integration-test", "verify");
    }

    @Test
    void rejectsDoctypeAndExternalEntityInput() throws Exception {
        Files.writeString(temporary.resolve("pom.xml"), """
                <?xml version="1.0"?>
                <!DOCTYPE project [<!ENTITY secret SYSTEM "file:///etc/passwd">]>
                <project><artifactId>&secret;</artifactId></project>
                """);

        assertThatThrownBy(() -> adapter().inspect(temporary))
                .isInstanceOf(InventorySafetyException.class)
                .hasMessageContaining("failed safely");
    }

    @Test
    void discoversMavenProjectBelowMixedRepositoryRoot() throws Exception {
        Files.createDirectories(temporary.resolve("backend/src/main/java/example"));
        Files.createDirectories(temporary.resolve("backend/src/test/java/example"));
        Files.writeString(temporary.resolve("backend/pom.xml"), """
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                  <groupId>example</groupId><artifactId>backend</artifactId><version>1</version>
                </project>
                """);
        Files.writeString(temporary.resolve("backend/src/main/java/example/App.java"), "class App {}");

        var report = adapter().inspect(temporary);

        assertThat(report.mavenProjects()).extracting("pomPath").containsExactly("backend/pom.xml");
        assertThat(report.paths().sourceRoots()).containsExactly("backend/src/main/java");
        assertThat(report.paths().testRoots()).containsExactly("backend/src/test/java");
    }

    @Test
    void rejectsEscapingMavenModule() throws Exception {
        Files.writeString(temporary.resolve("pom.xml"), """
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                  <artifactId>unsafe</artifactId><modules><module>../outside</module></modules>
                </project>
                """);

        assertThatThrownBy(() -> adapter().inspect(temporary))
                .isInstanceOf(InventorySafetyException.class)
                .hasMessageContaining("escapes repository");
    }

    @Test
    void rejectsSymbolicLinksAndConfiguredFileLimit() throws Exception {
        Files.writeString(temporary.resolve("one.java"), "class One {}");
        Files.writeString(temporary.resolve("two.java"), "class Two {}");
        assertThatThrownBy(() -> new PassiveRepositoryInventoryAdapter(
                new InventoryLimits(1, 10, 10000)).inspect(temporary))
                .isInstanceOf(InventorySafetyException.class)
                .hasMessageContaining("file count");

        Files.delete(temporary.resolve("two.java"));
        Files.createSymbolicLink(temporary.resolve("link.java"), temporary.resolve("one.java"));
        assertThatThrownBy(() -> adapter().inspect(temporary))
                .isInstanceOf(InventorySafetyException.class)
                .hasMessageContaining("symbolic link");
    }

    private PassiveRepositoryInventoryAdapter adapter() {
        return new PassiveRepositoryInventoryAdapter(new InventoryLimits(100, 10, 100000));
    }

    private static String rootPom() {
        return """
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                  <modelVersion>4.0.0</modelVersion><groupId>example</groupId>
                  <artifactId>root</artifactId><version>1</version><packaging>pom</packaging>
                  <properties><java.version>21</java.version></properties>
                  <modules><module>module</module></modules>
                  <dependencies><dependency><groupId>org.example</groupId><artifactId>library</artifactId></dependency></dependencies>
                  <build><plugins>
                    <plugin><artifactId>maven-surefire-plugin</artifactId></plugin>
                    <plugin><artifactId>maven-failsafe-plugin</artifactId><executions><execution>
                      <id>integration-tests</id><goals><goal>integration-test</goal><goal>verify</goal></goals>
                    </execution></executions></plugin>
                  </plugins></build>
                </project>
                """;
    }

    private static String modulePom() {
        return """
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                  <parent><groupId>example</groupId><artifactId>root</artifactId><version>1</version></parent>
                  <artifactId>module</artifactId>
                </project>
                """;
    }
}
