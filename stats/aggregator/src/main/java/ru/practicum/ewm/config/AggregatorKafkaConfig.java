package ru.practicum.ewm.config;

import lombok.Getter;
import lombok.Setter;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Duration;
import java.util.Properties;

@Getter
@Setter
@Configuration
@ConfigurationProperties("kafka")
public class AggregatorKafkaConfig {
    private String producerTopic;
    private String consumerTopic;
    private Duration consumeAttemptTimeout;
    private Properties producerProperties;
    private Properties consumerProperties;

    @Bean
    public KafkaProducer<String, SpecificRecordBase> producer() {
        return new KafkaProducer<>(getProducerProperties());
    }

    @Bean
    public KafkaConsumer<String, UserActionAvro> consumer() {
        return new KafkaConsumer<>(getConsumerProperties());
    }
}
