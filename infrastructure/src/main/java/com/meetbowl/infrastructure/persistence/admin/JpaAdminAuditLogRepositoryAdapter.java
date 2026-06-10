package com.meetbowl.infrastructure.persistence.admin;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.admin.AdminAuditLog;
import com.meetbowl.domain.admin.AdminAuditLogRepositoryPort;

@Repository
public class JpaAdminAuditLogRepositoryAdapter implements AdminAuditLogRepositoryPort {
    private final SpringDataAdminAuditLogRepository repository;

    public JpaAdminAuditLogRepositoryAdapter(SpringDataAdminAuditLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public AdminAuditLog save(AdminAuditLog adminAuditLog) {
        return repository.save(AdminAuditLogEntity.from(adminAuditLog)).toDomain();
    }

    @Override
    public Optional<AdminAuditLog> findById(UUID auditLogId) {
        return repository.findById(auditLogId).map(AdminAuditLogEntity::toDomain);
    }
}
