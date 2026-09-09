package com.minghan.credit.service;

import com.minghan.credit.entity.AuditAction;
import com.minghan.credit.entity.AuditEntityType;
import com.minghan.credit.entity.AuditLog;
import com.minghan.credit.repository.AuditLogRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    // Audit 必須加入既有 business transaction，避免原交易 rollback 後仍留下成功紀錄。
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(
            AuditAction action,
            AuditEntityType entityType,
            Long entityId) {
        // JWT filter 已建立 SecurityContext；Audit 只保存 username，不接觸 token 或 credential。
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AuthenticationCredentialsNotFoundException(
                    "Authenticated user is required for audit logging");
        }

        AuditLog auditLog = new AuditLog(
                authentication.getName(),
                action,
                entityType,
                entityId);
        auditLogRepository.save(auditLog);
    }
}
