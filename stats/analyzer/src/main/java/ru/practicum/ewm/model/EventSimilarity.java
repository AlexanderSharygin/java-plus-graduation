package ru.practicum.ewm.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventSimilarity {
    @Id
    private Long id;
    @NotNull
    private Long eventA;
    @NotNull
    private Long eventB;
    @PositiveOrZero
    private Double score;
    private Instant eventTime;
}
