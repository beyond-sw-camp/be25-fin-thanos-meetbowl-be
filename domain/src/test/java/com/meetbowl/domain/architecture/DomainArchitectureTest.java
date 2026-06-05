package com.meetbowl.domain.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** domain은 업무 규칙의 중심이므로 다른 애플리케이션 계층을 알면 안 된다. */
@AnalyzeClasses(
        packages = "com.meetbowl.domain",
        importOptions = ImportOption.DoNotIncludeTests.class)
class DomainArchitectureTest {

    @ArchTest
    static final ArchRule domain_should_not_depend_on_outer_layers =
            noClasses()
                    .that()
                    .resideInAPackage("com.meetbowl.domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.meetbowl.api..",
                            "com.meetbowl.application..",
                            "com.meetbowl.infrastructure..");
}
