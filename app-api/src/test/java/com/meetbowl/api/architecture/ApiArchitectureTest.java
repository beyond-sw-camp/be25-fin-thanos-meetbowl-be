package com.meetbowl.api.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import org.springframework.web.bind.annotation.RestController;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/** app-api는 HTTP 진입 계층이다. Controller는 UseCase만 호출하고 repository/entity에 직접 접근하지 않는다. */
@AnalyzeClasses(packages = "com.meetbowl.api", importOptions = ImportOption.DoNotIncludeTests.class)
class ApiArchitectureTest {

    @ArchTest
    static final ArchRule api_should_not_depend_on_domain_or_infrastructure =
            noClasses()
                    .that()
                    .resideInAPackage("com.meetbowl.api..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("com.meetbowl.domain..", "com.meetbowl.infrastructure..");

    @ArchTest
    static final ArchRule controllers_should_not_depend_on_repository_types =
            classes()
                    .that()
                    .areAnnotatedWith(RestController.class)
                    .should(notDependOnRepositoryTypes());

    private static ArchCondition<JavaClass> notDependOnRepositoryTypes() {
        return new ArchCondition<>("not depend on repository types") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                item.getDirectDependenciesFromSelf().stream()
                        .filter(dependency -> isRepositoryType(dependency.getTargetClass()))
                        .forEach(
                                dependency ->
                                        events.add(
                                                SimpleConditionEvent.violated(
                                                        item,
                                                        item.getName()
                                                                + " depends on repository type "
                                                                + dependency
                                                                        .getTargetClass()
                                                                        .getName())));
            }
        };
    }

    private static boolean isRepositoryType(JavaClass javaClass) {
        String simpleName = javaClass.getSimpleName();
        return simpleName.endsWith("Repository")
                || simpleName.endsWith("RepositoryPort")
                || javaClass.getPackageName().contains(".persistence.");
    }
}
