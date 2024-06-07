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

        var readSB = false;
        var readEB = false;

        while (true)
        {
            var i = inputStream.read();

            if (!readSB)
            {
                if (i < 0)
                {
                    logger.debug("Stream closed before start block character");
                    throw new SoftEndOfStreamException("Stream closed before start block character");
                }
                else if (i == START_BLOCK_CHAR)
                {
                    logger.debug("Read start block character");
                    readSB = true;
                }
                else
                {
                    logger.debug("Missing start block character");
                    throw new IOException("Missing start block character");
                }
            }
            else if (readEB)
            {
                if (i == CARRIAGE_RETURN)
                {
                    logger.debug("Read carriage return after end block character");
                    break;
                }
                else
                {
                    logger.debug("Did not read carriage return after end block character");
                    result.write(END_BLOCK_CHAR);
                    if (i == END_BLOCK_CHAR)
                    {
                        logger.debug("Read end block character");
                    }
                    else
                    {
                        result.write(i);
                        readEB = false;
                    }
                }
            }
            else if (i == END_BLOCK_CHAR)
            {
                logger.debug("Read end block character");
                readEB = true;
            }
            else
            {
                result.write(i);
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
}
