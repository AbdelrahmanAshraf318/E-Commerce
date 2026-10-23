package com.example.eCommerce.audit.service;

import com.example.eCommerce.audit.dtos.AuditLogResponse;
import com.example.eCommerce.audit.entity.AuditLog;
import com.example.eCommerce.audit.enums.AuditAction;
import com.example.eCommerce.audit.repo.AuditLogRepo;
import com.example.eCommerce.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogService
{
    private final AuditLogRepo auditLogRepo;

    /**
     * MANDATORY: the entry joins the caller's transaction, so it is written only if the action itself commits.
     * A failed password change must not leave a "Password changed" entry behind - and calling this outside a
     * transaction is a bug, so it fails loudly instead of silently writing on its own.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(UUID customerId, AuditAction action, String details)
    {
        auditLogRepo.save(new AuditLog(customerId, action, details));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(UUID customerId, AuditAction action)
    {
        record(customerId, action, null);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> getActivity(UUID customerId, Pageable pageable)
    {
        return PageResponse.from(auditLogRepo.findByCustomerId(customerId, pageable), AuditLogService::toResponse);
    }

    private static AuditLogResponse toResponse(AuditLog log)
    {
        return new AuditLogResponse(log.getId(), log.getAction(), log.getDetails(), log.getActor(), log.getOccurredAt());
    }
}
