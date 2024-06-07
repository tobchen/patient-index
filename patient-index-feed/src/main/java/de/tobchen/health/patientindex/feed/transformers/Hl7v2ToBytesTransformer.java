package de.tobchen.health.patientindex.feed.transformers;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.transformer.AbstractPayloadTransformer;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.Parser;

public class Hl7v2ToBytesTransformer extends AbstractPayloadTransformer<Message, byte[]>
{
    private final Logger logger = LoggerFactory.getLogger(Hl7v2ToBytesTransformer.class);

    private final Parser parser;

    public Hl7v2ToBytesTransformer(Parser parser)
    {
        this.parser = parser;
    }

    @Override
    protected byte[] transformPayload(Message payload)
    {
        String encodedHl7Message;
        try {
            encodedHl7Message = parser.encode(payload);
        } catch (HL7Exception e) {
            logger.error("Cannot encode HL7 message", e);
            throw new RuntimeException(e);
        }

        return encodedHl7Message.getBytes(StandardCharsets.UTF_8);
    }
        
}
