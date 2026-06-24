package com.meetbowl.infrastructure.persistence;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.meetbowl.infrastructure.persistence.admin.AdminAuditLogEntity;
import com.meetbowl.infrastructure.persistence.admin.SpringDataAdminAuditLogRepository;
import com.meetbowl.infrastructure.persistence.auth.PasswordResetRequestEntity;
import com.meetbowl.infrastructure.persistence.auth.SpringDataPasswordResetRequestRepository;
import com.meetbowl.infrastructure.persistence.mail.MailRetentionPolicyEntity;
import com.meetbowl.infrastructure.persistence.mail.SpringDataMailRetentionPolicyRepository;
import com.meetbowl.infrastructure.persistence.organization.AffiliateEntity;
import com.meetbowl.infrastructure.persistence.organization.DepartmentEntity;
import com.meetbowl.infrastructure.persistence.organization.PositionEntity;
import com.meetbowl.infrastructure.persistence.organization.SpringDataAffiliateRepository;
import com.meetbowl.infrastructure.persistence.organization.SpringDataDepartmentRepository;
import com.meetbowl.infrastructure.persistence.organization.SpringDataPositionRepository;
import com.meetbowl.infrastructure.persistence.organization.SpringDataTeamRepository;
import com.meetbowl.infrastructure.persistence.organization.TeamEntity;
import com.meetbowl.infrastructure.persistence.user.SpringDataUserRepository;
import com.meetbowl.infrastructure.persistence.user.SpringDataUserSettingRepository;
import com.meetbowl.infrastructure.persistence.user.UserEntity;
import com.meetbowl.infrastructure.persistence.user.UserSettingEntity;

@EntityScan(
        basePackageClasses = {
            UserEntity.class,
            UserSettingEntity.class,
            AffiliateEntity.class,
            DepartmentEntity.class,
            PositionEntity.class,
            TeamEntity.class,
            AdminAuditLogEntity.class,
            MailRetentionPolicyEntity.class,
            PasswordResetRequestEntity.class
        })
@EnableJpaRepositories(
        basePackageClasses = {
            SpringDataUserRepository.class,
            SpringDataUserSettingRepository.class,
            SpringDataAffiliateRepository.class,
            SpringDataDepartmentRepository.class,
            SpringDataPositionRepository.class,
            SpringDataTeamRepository.class,
            SpringDataAdminAuditLogRepository.class,
            SpringDataMailRetentionPolicyRepository.class,
            SpringDataPasswordResetRequestRepository.class
        })
@Configuration
public class AuthUserOrgJpaConfig {}
