package ru.practicum.ewm.stats.mapper;

import lombok.NoArgsConstructor;
import ru.practicum.ewm.dto.HitDto;
import ru.practicum.ewm.stats.model.Hit;

@NoArgsConstructor
public class HitMapper {

    public static Hit toHit(HitDto hitDto) {
        return new Hit(-1L,
                hitDto.getIp(),
                hitDto.getUri(),
                hitDto.getTimestamp(),
                null
        );
    }
}