package de.tobchen.health.patientindex.components;

import java.sql.Timestamp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import de.tobchen.health.patientindex.model.dto.AuditDto;
import de.tobchen.health.patientindex.model.entities.AuditEntity;
import de.tobchen.health.patientindex.model.repositories.AuditRepository;
import de.tobchen.health.patientindex.model.repositories.PatientRepository;

@Component
public class AuditLogger
{
    private final Logger logger = LoggerFactory.getLogger(AuditLogger.class);

    private final AuditRepository auditRepository;
    private final PatientRepository patientRepository;

    public AuditLogger(AuditRepository auditRepository, PatientRepository patientRepository)
    {
        this.auditRepository = auditRepository;
        this.patientRepository = patientRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @EventListener
    public void onAuditEvent(PayloadApplicationEvent<AuditDto> event)
    {
        var auditEvent = event.getPayload();

        var patientEntity = patientRepository.findByResourceId(auditEvent.patientResourceId());
        if (patientEntity.isEmpty())
        {
            logger.warn("Cannot find patient with resource id: {}", auditEvent.patientResourceId());
        }
        else
        {
            var auditEntity = new AuditEntity(auditEvent.categorySystem(), auditEvent.categoryCode(),
                auditEvent.codeSystem(), auditEvent.codeCode(), auditEvent.action(),
                new Timestamp(auditEvent.recordedAt().getTime()), patientEntity.get(),
                auditEvent.sourceDisplay(), auditEvent.sourceAddress(),
                auditEvent.destinationDisplay(), auditEvent.destinationAddress(),
                auditEvent.query());
            
            auditEntity = auditRepository.save(auditEntity);

            logger.debug("Saved audit event, id: {}", auditEntity.getId());
        }
    }
}
