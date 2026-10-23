package com.example.eCommerce.audit.controller;

import com.example.eCommerce.audit.dtos.AuditLogResponse;
import com.example.eCommerce.audit.service.AuditLogService;
import com.example.eCommerce.common.dto.PageResponse;
import com.example.eCommerce.user.utils.AuthenticationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read-only: entries are only ever written by the services that perform the audited actions. */
@RestController
@RequestMapping("/api/v1/users/me/activity")
@RequiredArgsConstructor
@Tag(name = "Activity", description = "The signed-in customer's audit log")
@SecurityRequirement(name = "bearerAuth")
public class AuditLogController
{
    private final AuditLogService auditLogService;

    @GetMapping
    @Operation(summary = "Account activity, newest first")
    public PageResponse<AuditLogResponse> getActivity(
            @PageableDefault(size = 20, sort = {"occurredAt", "id"}, direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication)
    {
        return auditLogService.getActivity(AuthenticationUtil.getUserId(authentication), pageable);
    }
}
