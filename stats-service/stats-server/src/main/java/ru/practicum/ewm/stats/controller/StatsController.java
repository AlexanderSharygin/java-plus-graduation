package ru.practicum.ewm.stats.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.HitDto;
import ru.practicum.ewm.dto.StatsDto;
import ru.practicum.ewm.stats.service.StatsService;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Slf4j
@RestController
public class StatsController {

    private final StatsService statsService;

    @Autowired
    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    void addHit(@Valid @RequestBody HitDto endpointHitDto) {
        log.info("Поступил запрос POST /hit на создание hit {}", endpointHitDto);
        statsService.hit(endpointHitDto);
        log.info("Запрос POST /hit успешно обработан");
    }

    @GetMapping("/stats")
    @ResponseStatus(HttpStatus.OK)
    Collection<StatsDto> getStatistics(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
                                       @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end,
                                       @RequestParam(required = false) List<String> uris,
                                       @RequestParam(required = false, defaultValue = "false") boolean unique) {
        log.info(("Поступил запрос GET /stats на получение статистики: startDateTime = {}, endDateTime = {}, " +
                "uris = {},unique = {}"), start, end, uris, unique);
        List<StatsDto> viewStats = statsService.getStats(start, end, uris, unique);
        log.info("Запрос GET /stats успешно обработан {}", viewStats);
        return viewStats;
    }
}
