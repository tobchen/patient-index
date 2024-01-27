package de.tobchen.health.patientindex.feed.components;

import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Component;

import de.tobchen.health.patientindex.feed.model.repositories.MessageRepository;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;

@Component
public class MessageSender
{
    private final Tracer tracer;

    private final MessageRepository repository;
    
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public MessageSender(OpenTelemetry openTelemetry, MessageRepository repository)
    {
        this.tracer = openTelemetry.getTracer(MessageSender.class.getName());

        this.repository = repository;

        // TODO Queue all pending messages
    }

    public void queue(Long messageId)
    {
        executor.submit(new MessageTask(messageId));
    }

    private synchronized void send(Long messageId)
    {
        var span = tracer.spanBuilder("MessageSender.send").startSpan();

        try (var scope = span.makeCurrent())
        {

        }
        finally
        {
            span.end();
        }
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

    private record Message(String patientId, Instant updatedAt, String otherPatientId, boolean wasSent) { }
}
