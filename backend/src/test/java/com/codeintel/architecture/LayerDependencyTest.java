package com.codeintel.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class LayerDependencyTest {
    private final JavaClasses productionClasses = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("com.codeintel");

    private static final ArchRule DOMAIN_IS_FRAMEWORK_INDEPENDENT = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta.persistence..", "..application..",
                    "..infrastructure..", "..presentation..");

    private static final ArchRule APPLICATION_DEPENDS_ONLY_INWARD = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..infrastructure..", "..presentation..", "org.eclipse.jgit..",
                    "com.github.dockerjava..", "com.openai..", "org.springframework..");

    private static final ArchRule PRESENTATION_DOES_NOT_DEPEND_ON_INFRASTRUCTURE = noClasses()
            .that().resideInAPackage("..presentation..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..");

    private static final ArchRule INFRASTRUCTURE_DOES_NOT_DEPEND_ON_PRESENTATION = noClasses()
            .that().resideInAPackage("..infrastructure..")
            .should().dependOnClassesThat().resideInAPackage("..presentation..");

    @Test
    void domainIsFrameworkIndependent() {
        DOMAIN_IS_FRAMEWORK_INDEPENDENT.check(productionClasses);
    }

    @Test
    void applicationDependsOnlyInward() {
        APPLICATION_DEPENDS_ONLY_INWARD.check(productionClasses);
    }

    @Test
    void presentationDoesNotDependOnInfrastructure() {
        PRESENTATION_DOES_NOT_DEPEND_ON_INFRASTRUCTURE.check(productionClasses);
    }

    @Test
    void infrastructureDoesNotDependOnPresentation() {
        INFRASTRUCTURE_DOES_NOT_DEPEND_ON_PRESENTATION.check(productionClasses);
    }
}
