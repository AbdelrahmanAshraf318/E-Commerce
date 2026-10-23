package com.example.eCommerce.audit.dtos;

import com.example.eCommerce.audit.enums.AuditAction;

import java.time.LocalDateTime;

public record AuditLogResponse(Long id, AuditAction action, String details, String actor, LocalDateTime occurredAt)
{
}
