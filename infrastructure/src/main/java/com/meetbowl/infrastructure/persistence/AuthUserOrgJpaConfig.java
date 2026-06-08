package com.meetbowl.infrastructure.persistence;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.meetbowl.infrastructure.persistence.admin.AdminAuditLogEntity;
import com.meetbowl.infrastructure.persistence.admin.SpringDataAdminAuditLogRepository;
import com.meetbowl.infrastructure.persistence.auth.LoginSessionEntity;
import com.meetbowl.infrastructure.persistence.auth.SpringDataLoginSessionRepository;
import com.meetbowl.infrastructure.persistence.mail.MailRetentionPolicyEntity;
import com.meetbowl.infrastructure.persistence.mail.SpringDataMailRetentionPolicyRepository;
import com.meetbowl.infrastructure.persistence.organization.DepartmentEntity;
import com.meetbowl.infrastructure.persistence.organization.JobEntity;
import com.meetbowl.infrastructure.persistence.organization.OrganizationEntity;
import com.meetbowl.infrastructure.persistence.organization.PositionEntity;
import com.meetbowl.infrastructure.persistence.organization.SpringDataDepartmentRepository;
import com.meetbowl.infrastructure.persistence.organization.SpringDataJobRepository;
import com.meetbowl.infrastructure.persistence.organization.SpringDataOrganizationRepository;
import com.meetbowl.infrastructure.persistence.organization.SpringDataPositionRepository;
import com.meetbowl.infrastructure.persistence.user.SpringDataUserRepository;
import com.meetbowl.infrastructure.persistence.user.SpringDataUserSettingRepository;
import com.meetbowl.infrastructure.persistence.user.UserEntity;
import com.meetbowl.infrastructure.persistence.user.UserSettingEntity;

@EntityScan(
        basePackageClasses = {
            UserEntity.class,
            UserSettingEntity.class,
            LoginSessionEntity.class,
            OrganizationEntity.class,
            DepartmentEntity.class,
            PositionEntity.class,
            JobEntity.class,
            AdminAuditLogEntity.class,
            MailRetentionPolicyEntity.class
        })
@EnableJpaRepositories(
        basePackageClasses = {
            SpringDataUserRepository.class,
            SpringDataUserSettingRepository.class,
            SpringDataLoginSessionRepository.class,
            SpringDataOrganizationRepository.class,
            SpringDataDepartmentRepository.class,
            SpringDataPositionRepository.class,
            SpringDataJobRepository.class,
            SpringDataAdminAuditLogRepository.class,
            SpringDataMailRetentionPolicyRepository.class
        })
@Configuration
public class AuthUserOrgJpaConfig {}
