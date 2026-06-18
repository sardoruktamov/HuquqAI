package api.ailawyer.uz.service;

import api.ailawyer.uz.entity.AuditLogEntity;
import api.ailawyer.uz.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Tizim harakatlarini alohida audit_logs jadvaliga yozadi.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Async("auditExecutor")
    @Transactional
    public void logAction(String entityName, String entityId, String action, Integer performedBy, String details) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.setEntityName(entityName);
        entity.setEntityId(entityId);
        entity.setAction(action);
        entity.setPerformedBy(performedBy);
        entity.setPerformedAt(LocalDateTime.now());
        entity.setDetails(details);
        auditLogRepository.save(entity);
        log.debug("Audit log yozildi entity={}, entityId={}, action={}, performedBy={}",
                entityName, entityId, action, performedBy);
    }
}
