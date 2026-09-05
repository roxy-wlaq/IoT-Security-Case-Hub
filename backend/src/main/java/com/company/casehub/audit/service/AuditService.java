package com.company.casehub.audit.service;

import com.company.casehub.audit.dto.AuditLogQuery;
import com.company.casehub.audit.dto.AuditLogResponse;
import com.company.casehub.audit.entity.AuditAction;
import com.company.casehub.audit.entity.AuditRecordEntity;
import com.company.casehub.audit.repository.AuditRecordRepository;
import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.testcase.dto.PagedResponse;
import jakarta.persistence.criteria.Predicate;
import java.lang.reflect.Array;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Append-only audit trail (Phase 26). Audit is governance: it never mutates
 * business state and never exposes update/delete workflows.
 *
 * <p><b>Transaction guarantee:</b> {@link #record} uses {@code REQUIRED}, so a
 * business mutation that records an audit event commits or rolls the audit row
 * together with the mutation in the same transaction. A successful audited
 * action therefore cannot end up without its audit record because of ordering.
 * The login flow runs outside a business transaction; its recording methods use
 * {@code REQUIRES_NEW} and are wrapped by the caller so an audit failure can
 * never weaken authentication.</p>
 */
@Service
public class AuditService {

    private static final int MAX_PAGE_SIZE = 200;
    private static final Pattern SENSITIVE_DETAIL_KEY = Pattern.compile(
            "(?i).*(password|session|csrf|secret|token|credential).*");

    private final AuditRecordRepository repository;

    public AuditService(AuditRecordRepository repository) {
        this.repository = repository;
    }

    /** Resolves the authenticated principal for services that do not receive one. */
    public static UserPrincipal currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal
                ? principal
                : null;
    }

    @Transactional
    public void record(AuditAction action, UserPrincipal actor, String resourceType, UUID resourceId,
                       String resourceLabel, Map<String, Object> detail) {
        record(action, actor != null ? actor.getId() : null,
                actor != null ? actor.getUsername() : null, resourceType,
                resourceId == null ? null : resourceId.toString(), resourceLabel, detail);
    }

    @Transactional
    public void record(AuditAction action, UUID actorId, String actorUsername, String resourceType,
                       String resourceId, String resourceLabel, Map<String, Object> detail) {
        if (actorId == null && (actorUsername == null || actorUsername.isBlank())) {
            UserPrincipal resolved = currentActor();
            if (resolved == null) {
                throw new IllegalArgumentException(
                        "An audit record requires an actor: no principal was provided or authenticated.");
            }
            actorId = resolved.getId();
            actorUsername = resolved.getUsername();
        }
        actorUsername = actorUsername == null ? null : actorUsername.trim();
        if (actorUsername == null || actorUsername.isBlank()) {
            throw new IllegalArgumentException("An audit record requires a non-blank actor username.");
        }
        if (action != AuditAction.LOGIN_FAILURE && actorId == null) {
            throw new IllegalArgumentException("An audit record requires an actor id for this action.");
        }
        AuditRecordEntity entity = new AuditRecordEntity();
        entity.setOccurredAt(Instant.now());
        entity.setAction(action);
        entity.setActorId(actorId);
        entity.setActorUsername(actorUsername);
        entity.setResourceType(resourceType);
        entity.setResourceId(resourceId);
        entity.setResourceLabel(resourceLabel);
        entity.setDetail(sanitize(detail));
        repository.save(entity);
    }

    /**
     * Login events run on the authentication path. {@code REQUIRES_NEW} keeps
     * them out of any accidental surrounding transaction; the caller wraps these
     * calls in a try/catch so audit problems never break authentication.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLoginSuccess(UserPrincipal actor, String ip) {
        record(AuditAction.LOGIN, actor.getId(), actor.getUsername(), "AUTHENTICATION",
                actor.getId().toString(), actor.getUsername(), Map.of("outcome", "success", "ip", ip));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLoginFailure(String username, String ip, String reason) {
        record(AuditAction.LOGIN_FAILURE, null, username, "AUTHENTICATION", null, username,
                Map.of("outcome", "failure", "reason", reason, "ip", ip));
    }

    @Transactional(readOnly = true)
    public PagedResponse<AuditLogResponse> query(AuditLogQuery query) {
        int page = Math.max(query.page(), 0);
        int size = Math.min(Math.max(query.size(), 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id")));
        Specification<AuditRecordEntity> spec = (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (query.action() != null) {
                predicates.add(cb.equal(root.get("action"), query.action()));
            }
            if (query.resourceType() != null && !query.resourceType().isBlank()) {
                predicates.add(cb.equal(root.get("resourceType"), query.resourceType().trim()));
            }
            if (query.resourceId() != null && !query.resourceId().isBlank()) {
                predicates.add(cb.equal(root.get("resourceId"), query.resourceId().trim()));
            }
            if (query.actorUsername() != null && !query.actorUsername().isBlank()) {
                predicates.add(cb.equal(root.get("actorUsername"), query.actorUsername().trim()));
            }
            if (query.occurredFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), query.occurredFrom()));
            }
            if (query.occurredTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("occurredAt"), query.occurredTo()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Page<AuditLogResponse> result = repository.findAll(spec, pageable).map(AuditLogResponse::from);
        return PagedResponse.from(result);
    }

    /**
     * Defence in depth: audit call sites never pass sensitive values, but any
     * detail key that looks like authentication material is dropped before
     * persistence so governance data can never become a credential store.
     */
    private Map<String, Object> sanitize(Map<String, Object> detail) {
        if (detail == null || detail.isEmpty()) {
            return null;
        }
        Map<String, Object> safe = new LinkedHashMap<>();
        detail.forEach((key, value) -> {
            if (!SENSITIVE_DETAIL_KEY.matcher(key).matches()) {
                safe.put(key, sanitizeValue(value));
            }
        });
        return safe.isEmpty() ? null : safe;
    }

    private Object sanitizeValue(Object value) {
        if (value instanceof Map<?, ?> nested) {
            Map<String, Object> safe = new LinkedHashMap<>();
            nested.forEach((key, nestedValue) -> {
                String keyText = String.valueOf(key);
                if (!SENSITIVE_DETAIL_KEY.matcher(keyText).matches()) {
                    safe.put(keyText, sanitizeValue(nestedValue));
                }
            });
            return safe;
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(this::sanitizeValue).toList();
        }
        if (value != null && value.getClass().isArray()) {
            List<Object> sanitized = new ArrayList<>(Array.getLength(value));
            for (int i = 0; i < Array.getLength(value); i++) {
                sanitized.add(sanitizeValue(Array.get(value, i)));
            }
            return sanitized;
        }
        return value;
    }
}
