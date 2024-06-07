package de.tobchen.health.patientindex.feed.configurations;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.ip.dsl.Tcp;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.hl7v2.parser.Parser;
import de.tobchen.health.patientindex.feed.serializers.MllpSerializer;
import de.tobchen.health.patientindex.feed.transformers.BytesToPatientTransformer;
import de.tobchen.health.patientindex.feed.transformers.Hl7v2ToBytesTransformer;
import de.tobchen.health.patientindex.feed.transformers.PatientToHl7v2AdtTransformer;

@Configuration
public class IntegrationConfig
{
    @Bean
    public IntegrationFlow flow(
        FhirContext context, Parser hl7Parser,
        ConnectionFactory connectionFactory, Queue queue,
        PatientToHl7v2AdtTransformer transformer,
        @Value("${patient-index.feed.receiver.host}") String host,
        @Value("${patient-index.feed.receiver.port}") int port)
    {
        var serializer = new MllpSerializer();

        return IntegrationFlow
            .from(Amqp.inboundAdapter(connectionFactory, queue))
            .transform(new BytesToPatientTransformer(context))
            .transform(transformer)
            .transform(new Hl7v2ToBytesTransformer(hl7Parser))
            .handle(Tcp.outboundGateway(Tcp.netClient(host, port)
                .deserializer(serializer)
                .serializer(serializer)
                .connectTimeout(15)
                .soTimeout(10000)))
            .nullChannel();
    }
}
