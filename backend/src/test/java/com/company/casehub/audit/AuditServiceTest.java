package com.company.casehub.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.casehub.audit.dto.AuditLogQuery;
import com.company.casehub.audit.entity.AuditAction;
import com.company.casehub.audit.entity.AuditRecordEntity;
import com.company.casehub.audit.repository.AuditRecordRepository;
import com.company.casehub.audit.service.AuditService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditRecordRepository repository;

    @Test
    void recursivelyRemovesSensitiveNestedDetailKeys() {
        AuditService service = new AuditService(repository);
        UUID actorId = UUID.randomUUID();
        Map<String, Object> detail = Map.of(
                "safe", "kept",
                "request", Map.of("password", "secret", "safeNested", "kept"),
                "items", List.of(Map.of("csrfToken", "csrf", "label", "kept")));

        service.record(AuditAction.PROJECT_CREATE, actorId, "admin", "PROJECT", "p-1", "P-1", detail);

        ArgumentCaptor<AuditRecordEntity> captor = ArgumentCaptor.forClass(AuditRecordEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getDetail()).containsEntry("safe", "kept");
        assertThat(captor.getValue().getDetail()).containsKey("request");
        assertThat(captor.getValue().getDetail().get("request").toString()).contains("safeNested");
        assertThat(captor.getValue().getDetail().get("request").toString()).doesNotContain("password", "secret");
        assertThat(captor.getValue().getDetail().get("items").toString()).doesNotContain("csrfToken", "csrf");
    }

    @Test
    void businessRecordRequiresActorIdAndUsername() {
        AuditService service = new AuditService(repository);

        assertThatThrownBy(() -> service.record(AuditAction.PROJECT_CREATE, null, "admin", "PROJECT", "p-1", "P-1", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void loginFailureKeepsNullActorIdAndNormalizesSubmittedUsername() {
        AuditService service = new AuditService(repository);

        service.recordLoginFailure("  alice  ", "127.0.0.1", "INVALID_CREDENTIALS");

        ArgumentCaptor<AuditRecordEntity> captor = ArgumentCaptor.forClass(AuditRecordEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getActorId()).isNull();
        assertThat(captor.getValue().getActorUsername()).isEqualTo("alice");
    }

    @Test
    void queryUsesDeterministicOccurredAtAndIdOrdering() {
        AuditService service = new AuditService(repository);
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.query(new AuditLogQuery(0, 20, null, null, null, null,
                Instant.EPOCH, Instant.now()));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(any(Specification.class), captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("occurredAt").getDirection().name()).isEqualTo("DESC");
        assertThat(captor.getValue().getSort().getOrderFor("id").getDirection().name()).isEqualTo("DESC");
    }
}
