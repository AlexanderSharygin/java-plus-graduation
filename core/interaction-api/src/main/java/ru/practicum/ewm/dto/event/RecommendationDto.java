package ru.practicum.ewm.dto.event;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Builder
@Getter
public class RecommendationDto {

    private Long eventId;
    private Double score;
}