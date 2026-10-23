package com.example.eCommerce.audit.repo;

import com.example.eCommerce.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuditLogRepo extends JpaRepository<AuditLog, Long>
{
    Page<AuditLog> findByCustomerId(UUID customerId, Pageable pageable);
}
