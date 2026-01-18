package ru.practicum.ewm.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Properties;

@Getter
@Setter
@ConfigurationProperties(prefix = "collector.kafka")
@Configuration
@Slf4j
public class KafkaConfig {

    private String uri;
    private Map<String, String> topics;
    private Map<String, String> producer;

    @Bean
    public Producer<String, SpecificRecordBase> kafkaProducer() {
        Properties config = new Properties();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, uri);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, producer.get("key-serializer"));
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, producer.get("value-serializer"));
        return new KafkaProducer<>(config);
    }

    public String getTopic(String topicEnum) {
        String topicName = topics.getOrDefault(topicEnum, null);
        if (topicName == null) {
            log.error("Topic {} not found in the configuration.", topicEnum);
            throw new IllegalArgumentException("Undefined Kafka topic: " + topicEnum);
        }
        return topicName;
    }
}
