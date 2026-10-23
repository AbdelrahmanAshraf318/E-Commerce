package com.example.eCommerce.audit.entity;

import com.example.eCommerce.audit.enums.AuditAction;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One row per thing that happened to a customer's account. Rows are append-only (@Immutable: Hibernate never
 * issues an UPDATE for them).
 * <p>
 * "When" and "who" are not set by the code that records the event - AuditingEntityListener fills them:
 * OCCURRED_AT from @CreatedDate, ACTOR from @CreatedBy via the AuditorAware bean in JpaConfig
 * (the signed-in user's email, or "system" when nobody is signed in yet, e.g. during login or sign-up).
 */
@Entity
@Immutable
@Table(
        name = "AUDIT_LOG",
        indexes = @Index(name = "IDX_AUDIT_LOG_CUSTOMER_TIME", columnList = "CUSTOMER_ID, OCCURRED_AT")
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // for JPA only
public class AuditLog
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AUDIT_LOG_ID")
    private Long id;

    // A plain column, deliberately not a foreign key: the trail must survive the account it describes being deleted.
    @Column(name = "CUSTOMER_ID", nullable = false, updatable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ACTION", nullable = false, length = 40, updatable = false)
    private AuditAction action;

    @Column(name = "DETAILS", length = 500, updatable = false)
    private String details;

    @CreatedDate
    @Column(name = "OCCURRED_AT", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    @CreatedBy
    @Column(name = "ACTOR", nullable = false, updatable = false)
    private String actor;

    public AuditLog(UUID customerId, AuditAction action, String details)
    {
        this.customerId = customerId;
        this.action = action;
        this.details = details;
    }
}
