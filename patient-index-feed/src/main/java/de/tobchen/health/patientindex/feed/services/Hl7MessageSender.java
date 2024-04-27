package de.tobchen.health.patientindex.feed.services;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.app.Connection;
import ca.uhn.hl7v2.llp.LLPException;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.PipeParser;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;

@Service
public class Hl7MessageSender
{
    private final Logger logger = LoggerFactory.getLogger(Hl7MessageSender.class);

    private final Tracer tracer;

    private final HapiContext context;

    private final String serverHost;
    private final int serverPort;

    private final PipeParser parser = new PipeParser();

    @Nullable
    private Connection connection;

    public Hl7MessageSender(OpenTelemetry openTelemetry, HapiContext context,
        @Value("${patient-index.feed.receiver.host}") String serverHost,
        @Value("${patient-index.feed.receiver.port}") int serverPort)
    {
        this.tracer = openTelemetry.getTracer(Hl7MessageSender.class.getName());

        this.context = context;

        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    public Message send(Message message)
    {
        Message response = null;

        var connection = this.connection;

        if (connection == null || !connection.isOpen())
        {
            this.connection = connection = null;

            try
            {
                this.connection = connection = context.newClient(serverHost, serverPort, false);
            }
            catch (HL7Exception e)
            {
                logger.error("Cannot connect", e);
            }
        }

        if (connection != null)
        {
            var initiator = connection.getInitiator();
            try
            {
                response = initiator.sendAndReceive(message);
            }
            catch (HL7Exception | LLPException | IOException e)
            {
                logger.error("Cannot send message or receive response", e);
            }
        }

        return response;
    }
}
