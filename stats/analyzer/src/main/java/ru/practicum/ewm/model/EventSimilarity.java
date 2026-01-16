package ru.practicum.ewm.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventSimilarity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "event_a")
    private Long eventA;

    @NotNull
    @Column(name = "event_b")
    private Long eventB;

    @PositiveOrZero
    @Column(name = "score")
    private Double score;  // Оставляем Double

    @Column(name = "event_time")
    private Instant eventTime;  // Оставляем Instant
}
