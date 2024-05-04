package de.tobchen.health.patientindex.feed.serializers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.integration.ip.tcp.serializer.SoftEndOfStreamException;

public class MllpSerializer implements Serializer<byte[]>, Deserializer<byte[]>
{
    public static final byte START_BLOCK_CHAR = 0x0b;
    public static final byte END_BLOCK_CHAR = 0x1c;
    public static final byte CARRIAGE_RETURN = 0x0d;

    private final Logger logger = LoggerFactory.getLogger(MllpSerializer.class);

    @Override
    public byte[] deserialize(InputStream inputStream) throws IOException
    {
        var result = new ByteArrayOutputStream();

        var state = DeserializerState.BEFORE_ANYTHING;

        while (state != DeserializerState.AFTER_EVERYTHING)
        {
            var i = inputStream.read();

            switch (state)
            {
            case BEFORE_ANYTHING:
                if (i < 0)
                {
                    logger.debug("Stream closed before start block character");
                    throw new SoftEndOfStreamException("Stream closed before start block character");
                }
                else if (i == START_BLOCK_CHAR)
                {
                    state = DeserializerState.AFTER_START_BLOCK_CHAR;
                }
                else
                {
                    logger.debug("Missing start block character");
                    throw new IOException("Missing start block character");
                }
                break;
            case AFTER_START_BLOCK_CHAR:
                if (i < 0)
                {
                    logger.debug("Stream closed before end block character");
                    throw new IOException("Stream closed before end block character");
                }
                else if (i == END_BLOCK_CHAR)
                {
                    state = DeserializerState.AFTER_END_BLOCK_CHAR;
                }
                else
                {
                    result.write(i);
                }             
                break;
            case AFTER_END_BLOCK_CHAR:
                if (i < 0)
                {
                    logger.debug("Stream closed before carriage return");
                    throw new IOException("Stream closed before carriage return");
                }
                else if (i == CARRIAGE_RETURN)
                {
                    state = DeserializerState.AFTER_EVERYTHING;
                }
                else
                {
                    result.write(END_BLOCK_CHAR);
                    result.write(i);
                }
                break;
            case AFTER_EVERYTHING:
                logger.debug("Somehow landed in AFTER_EVERYTHING block");
                break;
            }
        }

        return result.toByteArray();
    }

    @Override
    public void serialize(byte[] object, OutputStream outputStream) throws IOException
    {
        outputStream.write(START_BLOCK_CHAR);
        outputStream.write(object);
        outputStream.write(END_BLOCK_CHAR);
        outputStream.write(CARRIAGE_RETURN);
    }
    
    private enum DeserializerState
    {
        BEFORE_ANYTHING,
        AFTER_START_BLOCK_CHAR,
        AFTER_END_BLOCK_CHAR,
        AFTER_EVERYTHING
    }
}
