package ru.practicum.ewm.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "similarities")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EventSimilarity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_a")
    private Long eventA;

    @Column(name = "event_b")
    private Long eventB;

    @Column(name = "score")
    private Double score;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;
}
