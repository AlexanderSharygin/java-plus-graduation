package ru.practicum.ewm.stats.service;

import ru.practicum.ewm.dto.HitDto;
import ru.practicum.ewm.dto.StatsDto;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsService {
    void hit(HitDto hitDto);

    List<StatsDto> getStats(LocalDateTime startRange, LocalDateTime endRange, List<String> uris, boolean unique);
}
