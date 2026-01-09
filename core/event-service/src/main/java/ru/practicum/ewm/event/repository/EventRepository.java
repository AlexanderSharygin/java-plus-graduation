package ru.practicum.ewm.event.repository;

import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.ewm.model.category.EventCategory;
import ru.practicum.ewm.model.event.Event;
import ru.practicum.ewm.model.event.EventState;
import ru.practicum.ewm.model.request.RequestStatus;
import ru.practicum.ewm.model.user.User;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@NotNull
public interface EventRepository extends JpaRepository<Event, Long> {

    Page<Event> findAllByOwnerId(Long ownerId, Pageable pageable);

    List<Event> findAllByCategory(EventCategory category);

    List<Event> findAllByIdIn(List<Long> ids);

    @Query("SELECT e FROM Event e " +
            "WHERE e.ownerId is not null or e.ownerId IN :usersIds " +
            "AND e.state is not null or e.state = :states " +
            "AND e.category.id is not null or e.category.id in :categoriesIds " +
            "AND e.eventDateTime > :dateTime")
    Page<Event> findAllEventsAfterDateForUsersByStateAndCategories(List<Long> usersIds, List<EventState> states,
                                                                   List<Long> categoriesIds,
                                                                   Instant dateTime,
                                                                   Pageable pageable);

    @Query("SELECT e FROM Event e " +
            "WHERE e.ownerId IN :usersIds " +
            "AND e.state IN :states " +
            "AND e.category.id IN :categoriesIds " +
            "AND e.eventDateTime BETWEEN :startDateTime AND :endDateTime")
    Page<Event> findAllEventsBetweenDatesForUsersByStateAndCategories(List<Long> usersIds, List<EventState> states,
                                                                      List<Long> categoriesIds,
                                                                      Instant startDateTime,
                                                                      Instant endDateTime, Pageable pageable);

    @Query(" SELECT e " +
            "FROM Event e " +
            "JOIN ParticipationRequest r ON e.id = r.eventId " +
            "WHERE (upper(e.annotation) LIKE UPPER(CONCAT('%', :text, '%')) " +
            "OR UPPER(e.description) LIKE UPPER(CONCAT('%', :text, '%')) " +
            "OR :text is null) " +
            "AND (e.category.id IN :categoriesIds)" +
            "AND e.eventDateTime   >= :startDateTime " +
            "AND e.state = :eventState " +
            "AND r.status = :requestState " +
            "AND e.isPaid = :isPaid " +
            "GROUP BY e.id, e.annotation, e.category, e.createdOn, e.description, e.eventDateTime," +
            " e.ownerId, e.location, e.isPaid, e.participantLimit, e.publishedOn, e.isModerated," +
            " e.state, e.title " +
            "HAVING COUNT(r.status) < e.participantLimit ")
    Page<Event> findAllAvailablePublishedEventsByCategoryAndStateAfterDate(String text, Instant startDateTime,
                                                                           List<Long> categoriesIds, Pageable pageable,
                                                                           EventState eventState,
                                                                           RequestStatus requestState,
                                                                           boolean isPaid);

    @Query(" SELECT e " +
            "FROM Event e " +
            "JOIN ParticipationRequest r ON e.id = r.eventId " +
            "WHERE (UPPER(e.annotation) LIKE UPPER(CONCAT('%', :text, '%')) " +
            "OR UPPER(e.description)  LIKE UPPER(concat('%', :text, '%')) " +
            "OR :text is null) " +
            "AND (e.category.id IN :categoriesIds)" +
            "AND e.eventDateTime BETWEEN :startDateTime AND :endDateTime " +
            "AND e.state = :eventState " +
            "AND r.status = :requestState " +
            "AND e.isPaid = :isPaid " +
            "GROUP BY e.id, e.annotation, e.category, e.createdOn, e.description, e.eventDateTime," +
            " e.ownerId, e.location, e.isPaid, e.participantLimit, e.publishedOn, e.isModerated," +
            " e.state, e.title " +
            "HAVING COUNT(r.status) < e.participantLimit ")
    Page<Event> findAllAvailablePublishedEventsByCategoryAndStateBetweenDates(String text, Instant startDateTime,
                                                                              Instant endDateTime,
                                                                              List<Long> categoriesIds,
                                                                              Pageable pageable, EventState eventState,
                                                                              RequestStatus requestState,
                                                                              boolean isPaid);

    @Query(" SELECT e FROM Event e " +
            "WHERE (UPPER(e.annotation) LIKE UPPER(CONCAT('%', :text, '%')) " +
            "OR UPPER(e.description) LIKE UPPER(CONCAT('%', :text, '%')) " +
            "OR :text is null) " +
            "AND (e.category.id IN :categoriesIds)" +
            "AND e.isPaid is not null or e.isPaid = :isPaid " +
            "AND e.eventDateTime >= :startDateTime " +
            "AND e.state = :state")
    Page<Event> findAllEventsWithStatusAfterDate(String text, Instant startDateTime,
                                                 List<Long> categoriesIds, EventState state,
                                                 Pageable pageable, Boolean isPaid);

    @Query(" SELECT e FROM Event e " +
            "WHERE (UPPER(e.annotation) LIKE UPPER(CONCAT('%', :text, '%')) " +
            "OR UPPER(e.description) LIKE UPPER(CONCAT('%', :text, '%')) " +
            "OR :text is null) " +
            "AND (e.category.id IN :categoriesIds)" +
            "AND e.eventDateTime BETWEEN :startDateTime AND :endDateTime " +
            "AND e.isPaid is not null or e.isPaid = :isPaid " +
            "AND e.state = :state")
    Page<Event> findAllEventsWithStatusBetweenDates(String text, Instant startDateTime, Instant endDateTime,
                                                    List<Long> categoriesIds, EventState state,
                                                    Pageable pageable, boolean isPaid);

}

