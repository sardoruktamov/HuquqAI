package api.ailawyer.uz.service;

import api.ailawyer.uz.entity.AuditLogEntity;
import api.ailawyer.uz.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    @Test
    void logAction_persistsAuditRecord() {
        auditLogService.logAction("lawyer_profile", "9", "APPROVE", 1, null);

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLogEntity saved = captor.getValue();
        assertEquals("lawyer_profile", saved.getEntityName());
        assertEquals("9", saved.getEntityId());
        assertEquals("APPROVE", saved.getAction());
        assertEquals(1, saved.getPerformedBy());
        assertNotNull(saved.getPerformedAt());
    }
}
