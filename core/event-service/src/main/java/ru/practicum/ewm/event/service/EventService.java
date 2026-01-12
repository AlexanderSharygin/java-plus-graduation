package ru.practicum.ewm.event.service;

import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.category.repository.EventCategoryRepository;
import ru.practicum.ewm.client.AnalyzerClient;
import ru.practicum.ewm.dto.event.*;
import ru.practicum.ewm.dto.request.RequestDto;
import ru.practicum.ewm.dto.user.UserDto;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.event.repository.LocationRepository;
import ru.practicum.ewm.exception.BadRequestException;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.feign_clients.RequestClient;
import ru.practicum.ewm.feign_clients.UserClient;
import ru.practicum.ewm.grpc.stats.recommendations.RecommendedEventProto;
import ru.practicum.ewm.mapper.event.EventCategoryMapper;
import ru.practicum.ewm.mapper.event.EventMapper;
import ru.practicum.ewm.mapper.user.UserMapper;
import ru.practicum.ewm.model.category.EventCategory;
import ru.practicum.ewm.model.event.Event;
import ru.practicum.ewm.model.event.EventState;
import ru.practicum.ewm.model.event.Location;
import ru.practicum.ewm.model.user.User;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static java.time.LocalDateTime.now;
import static ru.practicum.ewm.model.event.AdminEventAction.REJECT_EVENT;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventCategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final LocationRepository locationRepository;
    private final RequestClient requestClient;
    private final UserClient userClient;
    private final AnalyzerClient analyzerClient;


    public EventDto create(CreateNewEventDto eventDto, Long userId) {
        User owner = UserMapper.toUserFromUserDto(userClient.getUserById(userId));
        EventCategory category = categoryRepository.findById(eventDto.getCategory())
                .orElseThrow(() -> new NotFoundException(
                        "Категория с id " + eventDto.getCategory() + "не существует!"));
        if (eventDto.getEventDate() != null &&
                eventDto.getEventDate().isBefore(now())) {
            throw new BadRequestException("Неверный eventStarDate: " + eventDto.getEventDate());
        }

        Event event = EventMapper.fromCreateNewEventDtoToEvent(eventDto, owner, category);
        if (event.getLocation().getLat() != null && event.getLocation().getLon() != null) {
            event.setLocation(saveLocation(event.getLocation()));
        } else {
            event.setLocation(saveLocation(new Location(-1L, 0.0, 0.0)));
        }
        if (event.getIsPaid() == null) {
            event.setIsPaid(false);
        }
        if (event.getParticipantLimit() == null) {
            event.setParticipantLimit(0L);
        }
        if (event.getIsModerated() == null) {
            event.setIsModerated(true);
        }
        Event result = eventRepository.save(event);

        return EventMapper.fromEventToEventDto(result, EventCategoryMapper.toCategoryDtoFromCategory(category),
                UserMapper.fromUserToUserShortDto(owner), 0L, 0d);
    }

    public EventDto updateByAdmin(Long eventId, UpdateEventAdminDto updateEventDto) {
        Event event = getEventIfExist(eventId);
        if (!event.getState().equals(EventState.PENDING)) {
            throw new ConflictException("Только событие в статусе pending может быть опубликовано");
        }
        if (event.getPublishedOn() != null && updateEventDto.getStateAction().equals(REJECT_EVENT)) {
            throw new ConflictException("Неверный статус события");
        }
        if (updateEventDto.getEventDate() != null && updateEventDto.getEventDate().isBefore(now())) {
            throw new BadRequestException("Неверный eventStarDate: " + updateEventDto.getEventDate());
        }

        if (updateEventDto.getCategory() != null) {
            EventCategory category = categoryRepository
                    .findById(updateEventDto.getCategory())
                    .orElseThrow(() -> new NotFoundException(
                            "Категория с id " + updateEventDto.getCategory() + "не существует"));
            event.setCategory(category);
        }
        if (updateEventDto.getAnnotation() != null) {
            event.setAnnotation(updateEventDto.getAnnotation());
        }
        if (updateEventDto.getDescription() != null) {
            event.setDescription(updateEventDto.getDescription());
        }
        updateEvent(event, updateEventDto.getEventDate(), updateEventDto.getLocation(), updateEventDto.getPaid(),
                updateEventDto.getParticipantLimit(), updateEventDto.getRequestModeration());
        if (updateEventDto.getStateAction() != null) {
            switch (updateEventDto.getStateAction()) {
                case PUBLISH_EVENT:
                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(now().toInstant(ZoneOffset.UTC));
                    break;
                case REJECT_EVENT:
                    event.setState(EventState.CANCELED);
                    break;
                default:
                    throw new ConflictException("Неверный статус события");
            }
        }
        if (updateEventDto.getTitle() != null) {
            event.setTitle(updateEventDto.getTitle());
        }
        Location location = event.getLocation();
        Location savedLOcation = saveLocation(location);
        event.setLocation(savedLOcation);
        Event updatedEvent = eventRepository.save(event);
        Map<Long, Double> ratingMap = analyzerClient.getInteractionsCount(List.of(eventId));
        EventDto eventDto = getEventDtoFromEvent(updatedEvent);
        eventDto.setRating(ratingMap.get(eventId));
        return eventDto;
    }


    public List<EventDto> getAll(List<Long> users, List<String> states, List<Long> categories,
                                 LocalDateTime rangeStart, LocalDateTime rangeEnd, Pageable pageable) {
        List<EventState> eventStates = new ArrayList<>();
        if (states != null) {
            for (String state : states) {
                eventStates.add(EventState.valueOf(state));
            }
        } else {
            eventStates = null;
        }

        if (states != null) {
            for (String state : states) {
                if (!state.equals(EventState.PUBLISHED.toString()) && !state.equals(EventState.CANCELED.toString()) &&
                        !state.equals(EventState.PENDING.toString())) {
                    throw new BadRequestException("Неверный статус события");
                }
            }
        }
        List<Long> categoriesIds;
        if (categories == null || categories.isEmpty()) {
            categoriesIds = categoryRepository.findAll().stream().map(EventCategory::getId).toList();
        } else {
            categoriesIds = categories;
        }
        Page<Event> events;
        if (rangeStart == null || rangeEnd == null) {
            events = eventRepository.findAllEventsAfterDateForUsersByStateAndCategories(users, eventStates,
                    categoriesIds, now().toInstant(ZoneOffset.UTC), pageable);

        } else {
            events = eventRepository.findAllEventsBetweenDatesForUsersByStateAndCategories(users, eventStates,
                    categoriesIds, rangeStart.toInstant(ZoneOffset.UTC), rangeEnd.toInstant(ZoneOffset.UTC), pageable);
        }

        return getEventsFulls(events.stream().toList());
    }

    public EventDto getById(Long eventId) {
        Event event = getEventIfExist(eventId);
        if (event.getPublishedOn() == null) {
            throw new NotFoundException("Событие с id" + eventId + " ещё не опубликовано");
        }
        Map<Long, Double> ratingMap = analyzerClient.getInteractionsCount(List.of(eventId));
        EventDto eventDto = getEventDtoFromEvent(event);
        eventDto.setRating(ratingMap.get(eventId));

        return eventDto;
    }

    public List<EventCommentDto> getEventsById(List<Long> eventIds) {

        return eventRepository.findAllByIdIn(eventIds).stream()
                .map(k -> EventMapper.fromEventToEventCommentDto(k, EventCategoryMapper.toCategoryDtoFromCategory(k.getCategory())))
                .toList();
    }

    public EventDto updateByUser(UpdateEventUserRequest eventDto, Long userId, Long eventId) {
        Event event = getEventIfExist(eventId);
        userClient.getUserById(userId);

        if (!Objects.equals(event.getOwnerId(), userId)) {
            throw new NotFoundException("User с id " + userId + " не хозяин для события " + eventId);
        }

        if (!event.getState().equals(EventState.PENDING) && !event.getState().equals(EventState.CANCELED)) {
            throw new ConflictException("Можно изменить только события в статусе pending или canceled");
        }

        if (eventDto.getEventDate() != null && eventDto.getEventDate().isBefore(now())) {
            throw new BadRequestException("Неверный eventStarDate: " + eventDto.getEventDate());
        }
        if (eventDto.getCategory() != null) {
            EventCategory category = categoryRepository
                    .findById(eventDto.getCategory())
                    .orElseThrow(() -> new NotFoundException(
                            "Категория с id " + eventDto.getCategory() + "не существует в БД"));
            event.setCategory(category);
        }
        if (eventDto.getAnnotation() != null && !event.getAnnotation().isBlank()) {
            event.setAnnotation(eventDto.getAnnotation());
        }

        if (eventDto.getDescription() != null && !eventDto.getDescription().isBlank()) {
            event.setDescription(eventDto.getDescription());
        }
        updateEvent(event, eventDto.getEventDate(), eventDto.getLocation(), eventDto.getPaid(),
                eventDto.getParticipantLimit(), eventDto.getRequestModeration());
        if (eventDto.getStateAction() != null) {
            switch (eventDto.getStateAction()) {
                case CANCEL_REVIEW:
                    event.setState(EventState.CANCELED);
                    break;
                case SEND_TO_REVIEW:
                    event.setState(EventState.PENDING);
                    break;
                default:
                    throw new ConflictException("Неверный статус события");
            }
        }
        if (eventDto.getTitle() != null && !eventDto.getTitle().isBlank()) {
            event.setTitle(eventDto.getTitle());
        }
        Map<Long, Double> ratingMap = analyzerClient.getInteractionsCount(List.of(eventId));
        EventDto result = getEventDtoFromEvent(event);
        result.setRating(ratingMap.get(eventId));

        return result;
    }

    public List<EventShortDto> getAllShort(String text, List<Long> categories, Boolean paid,
                                           LocalDateTime rangeStart, LocalDateTime rangeEnd, boolean onlyAvailable,
                                           String sort, int from, int size) {
        LocalDateTime start = (rangeStart == null) ? LocalDateTime.now() : rangeStart;
        LocalDateTime end = (rangeEnd == null) ? LocalDateTime.now().plusYears(10) : rangeEnd;

        if (start.isAfter(end))
            throw new BadRequestException("Дата окончания, должна быть больше даты старта.");
        List<Event> events = eventRepository.findEventsPublic(text, categories, paid, start, end,
                EventState.PUBLISHED, onlyAvailable, PageRequest.of(from / size, size)
        );

        return getEventsShorts(events.stream().toList());
    }

    public List<EventShortDto> getByUserId(Long userId, Pageable paging) {
        User user = UserMapper.toUserFromUserDto(userClient.getUserById(userId));
        List<Event> events = eventRepository.findAllByOwnerId(user.getId(), paging).stream().toList();
        events.forEach(event -> event.setOwnerId(user.getId()));

        return getEventsShorts(events);
    }

    public EventDto getEventByUserId(Long userId, Long eventId) {
        Event event = getEventIfExist(eventId);
        userClient.getUserById(userId);
        if (!Objects.equals(event.getOwnerId(), userId)) {
            throw new NotFoundException("User с id " + userId + " не хозяин события " + eventId);
        }
        Map<Long, Double> ratingMap = analyzerClient.getInteractionsCount(List.of(eventId));
        EventDto eventDto = getEventDtoFromEvent(event);
        eventDto.setRating(ratingMap.get(eventId));

        return eventDto;
    }

    public Event getEventIfExist(long eventId) {
        return eventRepository
                .findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id " + eventId + " не существует!"));
    }

    public Map<Long, Long> getConfirmedRequestsCountForEvents(List<Event> events) {
        List<RequestDto> requests = requestClient.getConfirmedRequestsForEvent(new ArrayList<>(events));
        Set<Long> requestsIds = new HashSet<>();
        for (var request : requests) {
            requestsIds.add(request.getEvent());
        }
        Map<Long, Long> confirmedRequestsCountForEvents = new HashMap<>();
        for (var id : requestsIds) {
            int count = (int) requests.stream()
                    .filter(k -> Objects.equals(k.getEvent(), id)).count();
            confirmedRequestsCountForEvents.put(id, (long) count);
        }

        return confirmedRequestsCountForEvents;
    }

    private Location saveLocation(Location location) {
        Optional<Location> existedLocation = locationRepository
                .findByLatAndLon(location.getLat(), location.getLon());
        if (existedLocation.isEmpty()) {
            locationRepository.save(location);
        }

        return locationRepository.findByLatAndLon(location.getLat(), location.getLon())
                .orElse(new Location());
    }

    private void updateEvent(Event event, LocalDateTime eventDate, Location location, Boolean paid, Long
            participantLimit, Boolean requestModeration) {
        if (eventDate != null) {
            event.setEventDateTime(eventDate.toInstant(ZoneOffset.UTC));
        }
        if (location != null) {
            event.setLocation(location);
        }
        if (paid != null) {
            event.setIsPaid(paid);
        }
        if (participantLimit != null) {
            event.setParticipantLimit(participantLimit);
        }
        if (requestModeration != null) {
            event.setIsModerated(requestModeration);
        }
    }

    private EventDto getEventDtoFromEvent(Event event) {
        long confirmedRequests = requestClient.getConfirmedRequestsForEvent(List.of(event)).size();

        User owner = UserMapper.toUserFromUserDto(userClient.getUserById(event.getOwnerId()));

        return EventMapper.fromEventToEventDto(event,
                EventCategoryMapper.toCategoryDtoFromCategory(event.getCategory()),
                UserMapper.fromUserToUserShortDto(owner),
                confirmedRequests,
                0d);
    }

    private List<Long> getEventsIdFromEventsList(List<Event> events) {
        return events.stream().map(Event::getId).toList();
    }

    private List<EventDto> getEventsFulls(List<Event> events) {
        List<Long> eventIds = getEventsIdFromEventsList(events);
        Map<Long, Long> confirmedRequestsCountForEvents = getConfirmedRequestsCountForEvents(events);
        Map<Long, Double> viewsMap = analyzerClient.getInteractionsCount(new ArrayList<>(eventIds));
        Map<Long, Long> eventsOwnersId = new HashMap<>();
        for (var event : events) {
            eventsOwnersId.put(event.getId(), event.getOwnerId());
        }
        List<UserDto> owners = userClient.getUsers(eventsOwnersId.values().stream().toList());

        return events.stream().map(event -> EventMapper.fromEventToEventDto(event,
                EventCategoryMapper.toCategoryDtoFromCategory(event.getCategory()),
                UserMapper.toUserShortDtoFromUserDto(Objects.requireNonNull(owners.stream()
                        .filter(k -> k.getId() == event.getOwnerId())
                        .findFirst().orElse(null))),
                confirmedRequestsCountForEvents.getOrDefault(event.getId(), 0L),
                viewsMap.get(event.getId()))).toList();
    }

    private List<EventShortDto> getEventsShorts(List<Event> events) {
        List<Long> eventIds = getEventsIdFromEventsList(events);
        Map<Long, Long> confirmedRequestsCountForEvents = getConfirmedRequestsCountForEvents(events);
        Map<Long, Double> ratingMap = analyzerClient.getInteractionsCount(new ArrayList<>(eventIds));
        Map<Long, Long> eventsOwnersId = new HashMap<>();
        for (var event : events) {
            eventsOwnersId.put(event.getId(), event.getOwnerId());
        }
        List<UserDto> owners = userClient.getUsers(eventsOwnersId.values().stream().toList());

        return events.stream().map(event -> EventMapper.fromEventToEventShortDto(event,
                EventCategoryMapper.toCategoryDtoFromCategory(event.getCategory()),
                UserMapper.toUserShortDtoFromUserDto(Objects.requireNonNull(owners.stream()
                        .filter(k -> k.getId() == event.getOwnerId())
                        .findFirst().orElseThrow())),
                confirmedRequestsCountForEvents.getOrDefault(event.getId(), 0L),
                ratingMap.get(event.getId()))).toList();
    }

    public void likeEvent(Long eventId, Long userId) {
        Event event = getEventIfExist(eventId);
        List<RequestDto> requests = requestClient.getConfirmedRequestsForEvent(List.of(event))
                .stream().filter(k -> k.getRequester() == userId).toList();
        if (requests.isEmpty()) {
            throw new ValidationException("Event not found");
        }


    }

    public List<EventDto> getRecommendations(Long userId, long maxResults) {
        List<Long> ids = analyzerClient.getRecommendationsForUser(userId, maxResults).stream()
                .sorted((a, b) -> (int) (a.getScore() - b.getScore()))
                .map(RecommendedEventProto::getEventId).toList();
        List<Event> events = eventRepository.findAllById(ids);
        List<Long> eventIds = getEventsIdFromEventsList(events);
        Map<Long, Long> eventsOwnersId = new HashMap<>();
        for (var event : events) {
            eventsOwnersId.put(event.getId(), event.getOwnerId());
        }
        List<UserDto> owners = userClient.getUsers(eventsOwnersId.values().stream().toList());
        Map<Long, Long> confirmedRequestsCountForEvents = getConfirmedRequestsCountForEvents(events);
        Map<Long, Double> viewsMap = analyzerClient.getInteractionsCount(new ArrayList<>(eventIds));

        return events.stream().map(event -> EventMapper.fromEventToEventDto(event,
                EventCategoryMapper.toCategoryDtoFromCategory(event.getCategory()),
                UserMapper.toUserShortDtoFromUserDto(Objects.requireNonNull(owners.stream()
                        .filter(k -> k.getId() == event.getOwnerId())
                        .findFirst().orElse(null))),
                confirmedRequestsCountForEvents.getOrDefault(event.getId(), 0L),
                viewsMap.get(event.getId()))).toList();
    }
}