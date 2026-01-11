package ru.practicum.ewm.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "kafka.topics")
@Configuration
@Slf4j
public class KafkaTopicConfig {

    private List<String> actions;
    private List<String> similarity;
}
