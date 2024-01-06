package de.tobchen.health.patientindex.components;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.tobchen.health.patientindex.configurations.PidFeedConfig;

public class PidFeedDispatcher
{
    private final PidFeedConfig pidFeedConfig;

    // TODO Graceful shutdown
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public PidFeedDispatcher(PidFeedConfig pidFeedConfig)
    {
        this.pidFeedConfig = pidFeedConfig;
    }

    private void sendMessage(Long id)
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
