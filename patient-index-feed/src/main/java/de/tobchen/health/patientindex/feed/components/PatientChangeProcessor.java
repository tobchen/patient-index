package de.tobchen.health.patientindex.feed.components;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PatientChangeProcessor
{
    @RabbitListener(queues = "#{queue.name}")
    public void onPatientChange(Message message)
    {
        
    }
}
