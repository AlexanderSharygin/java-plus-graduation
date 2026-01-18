package ru.practicum.ewm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.ConfigurableApplicationContext;
import ru.practicum.ewm.handler.SimilarityCalculator;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableDiscoveryClient
public class Aggregator {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(Aggregator.class, args);
        SimilarityCalculator aggregator = context.getBean(SimilarityCalculator.class);
        Runtime.getRuntime().addShutdownHook(new Thread(aggregator::stop));
        aggregator.run();
    }
}