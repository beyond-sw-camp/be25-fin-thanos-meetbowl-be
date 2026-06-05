package com.meetbowl.infrastructure.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** infrastructure는 adapter 구현 계층이다. HTTP API나 UseCase 구현에 의존하지 않고 domain port를 구현한다. */
@AnalyzeClasses(
        packages = "com.meetbowl.infrastructure",
        importOptions = ImportOption.DoNotIncludeTests.class)
class InfrastructureArchitectureTest {

    @ArchTest
    static final ArchRule infrastructure_should_not_depend_on_api_or_application =
            noClasses()
                    .that()
                    .resideInAPackage("com.meetbowl.infrastructure..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("com.meetbowl.api..", "com.meetbowl.application..");
}
