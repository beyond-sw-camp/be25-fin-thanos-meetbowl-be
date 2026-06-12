package com.meetbowl.infrastructure.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.springframework.data.repository.Repository;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * 챗봇 대화는 휘발성 데이터이므로 어떤 계층에도 영속 매핑이 생기면 안 된다.
 *
 * <p>JPA Entity나 Spring Data Repository가 챗봇 패키지에 추가되면 질문/답변/출처가 DB에 남는 경로가 열리므로, 그런 클래스가 생기지 않도록
 * 아키텍처 수준에서 강제한다.
 */
@AnalyzeClasses(packages = "com.meetbowl", importOptions = ImportOption.DoNotIncludeTests.class)
class ChatbotPersistenceArchitectureTest {

    @ArchTest
    static final ArchRule chatbot_should_not_define_jpa_entities =
            noClasses()
                    .that()
                    .resideInAPackage("..chatbot..")
                    .should()
                    .beAnnotatedWith(Entity.class)
                    .orShould()
                    .beAnnotatedWith(Table.class);

    @ArchTest
    static final ArchRule chatbot_should_not_define_spring_data_repositories =
            noClasses()
                    .that()
                    .resideInAPackage("..chatbot..")
                    .should()
                    .beAssignableTo(Repository.class);
}
