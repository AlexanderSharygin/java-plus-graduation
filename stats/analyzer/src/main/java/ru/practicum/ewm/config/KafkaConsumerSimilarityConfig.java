package ru.practicum.ewm.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.util.Properties;

@Getter
@Setter
@ConfigurationProperties(prefix = "kafka.consumers.similarity")
@Configuration
@Slf4j
public class KafkaConsumerSimilarityConfig {

    private String uri;
    private String groupId;
    private String autoOffsetReset;
    private boolean enableAutoCommit;
    private Long consumeAttemptTimeoutMs;
    private String keyDeserializer;
    private String valueDeserializer;

    @Bean
    public KafkaConsumer<String, EventSimilarityAvro> kafkaHubsConsumer() {
        Properties config = new Properties();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, uri);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, enableAutoCommit);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializer);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializer);
        config.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 15000);
        config.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 5000);
        config.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10);
        config.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);

        return new KafkaConsumer<>(config);
    }
}
