package com.meetbowl.application.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * application은 UseCase 조율 계층이다.
 * HTTP/API 세부 구현이나 infrastructure adapter를 직접 알면 안 된다.
 */
@AnalyzeClasses(packages = "com.meetbowl.application", importOptions = ImportOption.DoNotIncludeTests.class)
class ApplicationArchitectureTest {

    @ArchTest
    static final ArchRule application_should_not_depend_on_api_or_infrastructure = noClasses()
            .that()
            .resideInAPackage("com.meetbowl.application..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "com.meetbowl.api..",
                    "com.meetbowl.infrastructure.."
            );
}
