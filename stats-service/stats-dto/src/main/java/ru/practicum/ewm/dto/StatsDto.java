package ru.practicum.ewm.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StatsDto {
    private String app;
    private String uri;
    private Long hits;

    public StatsDto(String app, String uri, Long hits) {
        this.app = app;
        this.uri = uri;
        this.hits = hits;
    }

    public StatsDto(String app, String uri) {
        this.app = app;
        this.uri = uri;
        this.hits = 1L;
    }
}