package com.meetbowl.infrastructure.persistence.admin;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataAdminAuditLogRepository
        extends JpaRepository<AdminAuditLogEntity, UUID> {}
