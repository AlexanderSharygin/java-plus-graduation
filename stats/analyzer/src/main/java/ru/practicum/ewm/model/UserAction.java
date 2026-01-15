package ru.practicum.ewm.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.checkerframework.checker.units.qual.A;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAction {
    @Id
    private Long id;
    @NotNull
    private Long userId;
    @NotNull
    private Long eventId;
    @Enumerated(EnumType.STRING)
    private ActionType actionType;
    private Instant actionTime;
}
