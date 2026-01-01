package ru.practicum.ewm.stats.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@ToString
@Table(name = "hit")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Hit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "ip", nullable = false, length = 15)
    String ip;

    @Column(name = "uri", nullable = false)
    String uri;

    @Column(name = "DATE_TIME")
    LocalDateTime timestamp;

    @ManyToOne
    private App app;
}