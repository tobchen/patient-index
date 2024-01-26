package de.tobchen.health.patientindex.feed.components;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Component;

import de.tobchen.health.patientindex.feed.model.repositories.MessageRepository;

@Component
public class MessageSender
{
    private final MessageRepository repository;
    
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public MessageSender(MessageRepository repository)
    {
        this.repository = repository;

        // TODO Queue all pending messages
    }

    public void queue(Long messageId)
    {
        executor.submit(new MessageTask(messageId));
    }

    private synchronized void send(Long messageId)
    {

    }

    private class MessageTask implements Runnable
    {
        private Long messageId;

        public MessageTask(Long messageId)
        {
            this.messageId = messageId;
        }

        @Override
        public void run()
        {
            var messageId = this.messageId;
            if (messageId != null)
            {
                send(messageId);
            }
        }
    }
}
