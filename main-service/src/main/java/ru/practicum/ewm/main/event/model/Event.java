package ru.practicum.ewm.main.event.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import ru.practicum.ewm.main.category.model.EventCategory;
import ru.practicum.ewm.main.user.model.User;

import java.time.Instant;

@Entity
@Table(name = "events")
@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Size(min = 1, max = 120)
    private String title;
    @Size(min = 20, max = 2000)
    private String annotation;
    @Size(min = 20, max = 7000)
    private String description;
    @ManyToOne(fetch = FetchType.EAGER)
    @JsonIgnore
    @JoinColumn(name = "category_id")
    private EventCategory category;
    private Instant createdOn;
    private Instant eventDateTime;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;
    private Boolean isPaid;
    private Long participantLimit;
    private Instant publishedOn;
    private Boolean isModerated;
    @Enumerated(EnumType.STRING)
    private EventState state;
}
