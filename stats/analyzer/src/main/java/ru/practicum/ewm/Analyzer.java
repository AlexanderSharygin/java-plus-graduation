package ru.practicum.ewm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.ConfigurableApplicationContext;
import ru.practicum.ewm.service.AnalyzerRunner;


@SpringBootApplication
@ConfigurationPropertiesScan
@EnableDiscoveryClient
public class Analyzer {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(Analyzer.class, args);

        AnalyzerRunner kafkaConsumer = context.getBean(AnalyzerRunner.class);
        Runtime.getRuntime().addShutdownHook(new Thread(kafkaConsumer::stop));
        kafkaConsumer.run();
    }
}