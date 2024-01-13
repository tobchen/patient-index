package de.tobchen.health.patientindex.components;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.event.EventListener;

import de.tobchen.health.patientindex.configurations.PidFeedConfig;
import de.tobchen.health.patientindex.model.dto.PidFeedMsgDto;

public class PidFeedDispatcher
{
    private final PidFeedConfig pidFeedConfig;

    // TODO Start previously stored tasks
    // TODO Graceful shutdown
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public PidFeedDispatcher(PidFeedConfig pidFeedConfig)
    {
        this.pidFeedConfig = pidFeedConfig;
    }

    @EventListener
    public void onPidFeedMsg(PayloadApplicationEvent<PidFeedMsgDto> event)
    {
        executor.submit(new SendMessageTask(event.getPayload().messageId()));
    }

    synchronized private void sendMessage(Long id)
    {
        // TODO Implement: Send, if status is QUEUED
    }

    private class SendMessageTask implements Runnable
    {
        private final Long id;

        public SendMessageTask(Long id)
        {
            this.id = id;
        }

        @Override
        public void run()
        {
            sendMessage(id);
        }
    }
}
