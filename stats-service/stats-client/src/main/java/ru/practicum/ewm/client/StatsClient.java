package ru.practicum.ewm.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.MaxAttemptsRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.dto.HitDto;

import java.util.List;
import java.util.Map;

@Service
public class StatsClient extends BaseClient {

    private static final String SERVICE_NAME = "STATS-SERVER";
    private final DiscoveryClient discoveryClient;
    private final RetryTemplate retryTemplate;

    @Autowired
    public StatsClient(RestTemplateBuilder builder, DiscoveryClient discoveryClient) {
        super(builder.setConnectTimeout(java.time.Duration.ofSeconds(10))
                .setReadTimeout(java.time.Duration.ofSeconds(10))
                .build());

        this.discoveryClient = discoveryClient;

        RetryTemplate template = new RetryTemplate();
        FixedBackOffPolicy backOff = new FixedBackOffPolicy();
        backOff.setBackOffPeriod(3000L);
        template.setBackOffPolicy(backOff);

        MaxAttemptsRetryPolicy retryPolicy = new MaxAttemptsRetryPolicy(3);
        template.setRetryPolicy(retryPolicy);

        this.retryTemplate = template;
    }

    private String getBaseUrl() {
        ServiceInstance instance = retryTemplate.execute(ctx ->
                discoveryClient.getInstances(SERVICE_NAME).getFirst());
        return "http://" + instance.getHost() + ":" + instance.getPort();
    }

    public ResponseEntity<Object> getStats(String startDateTime, String endDateTime, List<String> uris, boolean unique) {
        String baseUrl = getBaseUrl();
        Map<String, Object> parameters = Map.of(
                "start", startDateTime,
                "end", endDateTime,
                "uris", uris,
                "unique", unique
        );


        return get(baseUrl + "/stats?start={start}&end={end}&uris={uris}&unique={unique}", parameters);
    }

    public ResponseEntity<Object> create(HitDto hitDto) {
        String baseUrl = getBaseUrl();
        return post(baseUrl + "/hit", hitDto);
    }
}

