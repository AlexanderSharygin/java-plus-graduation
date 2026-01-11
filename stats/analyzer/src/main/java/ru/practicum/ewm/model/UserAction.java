package ru.practicum.ewm.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.checkerframework.checker.units.qual.A;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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
