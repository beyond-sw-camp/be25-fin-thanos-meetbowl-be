package com.meetbowl.infrastructure.persistence.admin;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import com.meetbowl.domain.admin.AdminAuditLog;
import com.meetbowl.domain.admin.AdminAuditLogRepositoryPort;
import com.meetbowl.domain.admin.AdminAuditLogSearchCondition;
import com.meetbowl.domain.common.Paged;

@Repository
public class JpaAdminAuditLogRepositoryAdapter implements AdminAuditLogRepositoryPort {
    private static final String[] KNOWN_TARGET_TYPES = {
        "USER", "AFFILIATE", "DEPARTMENT", "TEAM", "POSITION", "ORGANIZATION_EXCEL"
    };

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

    @Override
    public Paged<AdminAuditLog> findPage(AdminAuditLogSearchCondition condition) {
        PageRequest pageRequest =
                PageRequest.of(
                        condition.page() - 1,
                        condition.size(),
                        Sort.by(Sort.Direction.DESC, "occurredAt"));
        var page = repository.findAll(specification(condition), pageRequest);
        return new Paged<>(
                page.getContent().stream().map(AdminAuditLogEntity::toDomain).toList(),
                page.getTotalElements());
    }

    private Specification<AdminAuditLogEntity> specification(
            AdminAuditLogSearchCondition condition) {
        return (root, query, builder) -> {
            Predicate predicate = builder.conjunction();
            if (condition.actorUserId() != null) {
                predicate =
                        builder.and(
                                predicate,
                                builder.equal(root.get("actorId"), condition.actorUserId()));
            }
            if (condition.actorName() != null) {
                predicate =
                        builder.and(
                                predicate,
                                builder.equal(root.get("actorName"), condition.actorName()));
            }
            if (condition.targetType() != null) {
                predicate =
                        builder.and(
                                predicate,
                                builder.equal(root.get("targetType"), condition.targetType()));
            }
            if (condition.targetId() != null) {
                predicate =
                        builder.and(
                                predicate,
                                builder.equal(root.get("targetId"), condition.targetId()));
            }
            if (condition.result() != null) {
                predicate =
                        builder.and(
                                predicate, builder.equal(root.get("result"), condition.result()));
            }
            if (condition.from() != null) {
                predicate =
                        builder.and(
                                predicate,
                                builder.greaterThanOrEqualTo(
                                        root.get("occurredAt"), condition.from()));
            }
            if (condition.to() != null) {
                predicate =
                        builder.and(
                                predicate,
                                builder.lessThanOrEqualTo(root.get("occurredAt"), condition.to()));
            }
            if (condition.actionType() != null) {
                predicate = builder.and(predicate, actionTypePredicate(condition, root, builder));
            }
            return predicate;
        };
    }

    private Predicate actionTypePredicate(
            AdminAuditLogSearchCondition condition,
            jakarta.persistence.criteria.Root<AdminAuditLogEntity> root,
            jakarta.persistence.criteria.CriteriaBuilder builder) {
        String actionType = condition.actionType();
        String targetType = condition.targetType();
        if (targetType != null && actionType.startsWith(targetType + "_")) {
            String strippedActionName = actionType.substring(targetType.length() + 1);
            return builder.or(
                    builder.equal(root.get("actionName"), actionType),
                    builder.equal(root.get("actionName"), strippedActionName));
        }
        for (String knownTargetType : KNOWN_TARGET_TYPES) {
            if (actionType.startsWith(knownTargetType + "_")) {
                String strippedActionName = actionType.substring(knownTargetType.length() + 1);
                return builder.and(
                        builder.equal(root.get("targetType"), knownTargetType),
                        builder.or(
                                builder.equal(root.get("actionName"), actionType),
                                builder.equal(root.get("actionName"), strippedActionName)));
            }
        }
        return builder.equal(root.get("actionName"), actionType);
    }
}
