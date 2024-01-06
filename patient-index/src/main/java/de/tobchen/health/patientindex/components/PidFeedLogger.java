package de.tobchen.health.patientindex.components;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.lookup.StringLookup;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import de.tobchen.health.patientindex.configurations.GeneralConfig;
import de.tobchen.health.patientindex.configurations.IheConfig;
import de.tobchen.health.patientindex.configurations.PidFeedConfig;
import de.tobchen.health.patientindex.model.dto.PatientCreationDto;
import de.tobchen.health.patientindex.model.dto.PatientMergeDto;
import de.tobchen.health.patientindex.model.dto.PidFeedMsgDto;
import de.tobchen.health.patientindex.model.entities.PidFeedMsgEntity;
import de.tobchen.health.patientindex.model.repositories.PidFeedMsgRepository;

@Component
public class PidFeedLogger
{
    private static final String ADMIT_MSG =
        "MSH|^~\\&|^${thisAppOid}^ISO|^${thisFacilityOid}^ISO|^${otherAppOid}^ISO|^${otherFacilityOid}^ISO|${msgDt}||ADT^A01|${msgId}|${mode}|2.3.1\r"
        + "EVN||${evnDt}\r"
        + "PID|||${pid}^^^&${pidOid}&ISO|| \r"
        + "PV1||N\r";

    private static final String MERGE_MSG =
        "MSH|^~\\&|^${thisAppOid}^ISO|^${thisFacilityOid}^ISO|^${otherAppOid}^ISO|^${otherFacilityOid}^ISO|${msgDt}||ADT^A40|${msgId}|${mode}|2.3.1\r"
        + "EVN||${evnDt}\r"
        + "PID|||${pid}^^^&${pidOid}&ISO|| \r"
        + "MRG|${prevPid}^^^&${pidOid}&ISO\r";

    private final ApplicationEventPublisher publisher;

    private final PidFeedMsgRepository repository;
    
    private final MessageVariableLookup lookup;
    private final StringSubstitutor substitutor;

    public PidFeedLogger(ApplicationEventPublisher publisher, PidFeedMsgRepository repository,
        GeneralConfig generalConfig, IheConfig iheConfig, PidFeedConfig pidFeedConfig)
    {
        this.publisher = publisher;

        this.repository = repository;

        lookup = new MessageVariableLookup(generalConfig, iheConfig, pidFeedConfig);
        substitutor = new StringSubstitutor(lookup);
    }

    @EventListener
    public void onPatientCreation(PayloadApplicationEvent<PatientCreationDto> event)
    {
        Long msgEntityId = createMessage(ADMIT_MSG, event.getPayload().resourceId(), null);

        publisher.publishEvent(new PidFeedMsgDto(msgEntityId));
    }

    @EventListener
    public void onPatientMerge(PayloadApplicationEvent<PatientMergeDto> event)
    {
        var mergeEvent = event.getPayload();

        Long msgEntityId = createMessage(MERGE_MSG,
            mergeEvent.targetResourceId(), mergeEvent.sourceResourceId());

        publisher.publishEvent(new PidFeedMsgDto(msgEntityId));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private Long createMessage(String template, String pid, @Nullable String prevPid)
    {
        var message = replace(template, pid, prevPid);
        
        var entity = new PidFeedMsgEntity(message);
        
        return repository.save(entity).getId();
    }

    synchronized private String replace(String template, String pid, @Nullable String prevPid)
    {
        // TODO Event- & Message-DateTime
        lookup.update(pid, prevPid);
        return substitutor.replace(template);
    }

    private class MessageVariableLookup implements StringLookup
    {
        private final Map<String, String> map = new HashMap<>();

        public MessageVariableLookup(GeneralConfig generalConfig,
            IheConfig iheConfig, PidFeedConfig pidFeedConfig)
        {
            var mode = generalConfig.productionMode();

            map.put("thisAppOid", iheConfig.appOid());
            map.put("thisFacilityOid", iheConfig.facilityOid());
            map.put("otherAppOid", null);
            map.put("otherFacilityOid", null);
            map.put("mode", mode != null && mode.booleanValue() ? "P" : "T");
            map.put("pidOid", iheConfig.pidOid());
        }

        public void update(String pid, @Nullable String prevPid)
        {
            map.put("pid", pid);
            
            if (prevPid != null)
            {
                map.put("prevPid", prevPid);
            }
            else
            {
                map.remove("prevPid");
            }
        }

        @Override
        public String lookup(String key)
        {
            return map.getOrDefault(key, "");
        }
    }
}
