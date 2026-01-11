package ru.practicum.ewm.event.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.category.repository.EventCategoryRepository;
import ru.practicum.ewm.client.StatsClient;
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
import java.time.format.DateTimeFormatter;
import java.util.*;

import static java.time.LocalDateTime.now;
import static java.time.format.DateTimeFormatter.*;
import static ru.practicum.ewm.model.event.AdminEventAction.REJECT_EVENT;
import static ru.practicum.ewm.model.request.RequestStatus.CONFIRMED;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventCategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final LocationRepository locationRepository;
    private final RequestClient requestClient;
    private final UserClient userClient;
    private final StatsClient statsClient;


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
                UserMapper.fromUserToUserShortDto(owner), 0L, 0);
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

        return getEventDtoFromEvent(updatedEvent);
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
        Integer views = getEventsViews(event.getId()) + 1;
        EventDto eventDto = getEventDtoFromEvent(event);
        eventDto.setViews(views);

        return eventDto;
    }

    public List<EventCommentDto> getEventsById(List<Long> eventIds) {

        return eventRepository.findAllByIdIn(eventIds).stream()
                .map(k -> EventMapper.fromEventToEventCommentDto(k, EventCategoryMapper.toCategoryDtoFromCategory(k.getCategory())))
                .toList();
    }

    public Integer getEventsViews(Long eventId) {
        List<String> uris = List.of("/events/" + eventId);
        List<HashMap<Object, Object>> stats = getStats(uris);
        if (stats != null && !stats.isEmpty()) {
            return (Integer) stats.getFirst().get("hits");
        } else {
            return 0;
        }
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

        return getEventDtoFromEvent(event);
    }

    public List<EventShortDto> getAllShort(String text, List<Long> categories, Boolean paid,
                                           LocalDateTime rangeStart, LocalDateTime rangeEnd, boolean onlyAvailable,
                                           String sort, int from, int size) {
        Page<Event> events;
        Pageable paging;
        if (sort == null) {
            paging = PageRequest.of(from, size);
        } else {
            if (sort.equals("VIEWS") || sort.equals("EVENT_DATE") || sort.isBlank()) {
                if (sort.equals("EVENT_DATE")) {
                    paging = PageRequest.of((from) % size, size, Sort.by("eventDateTime")
                            .descending());
                } else {
                    paging = PageRequest.of(from, size);
                }
            } else {
                throw new ConflictException("Неверная сортировка. Используй VIEW or EVENT_DATE");
            }
        }
        if (onlyAvailable) {
            if (rangeStart == null || rangeEnd == null) {
                events = eventRepository.findAllAvailablePublishedEventsByCategoryAndStateAfterDate(text,
                        now().toInstant(ZoneOffset.UTC), categories, paging, EventState.PUBLISHED,
                        CONFIRMED, paid);
            } else {
                events = eventRepository.findAllAvailablePublishedEventsByCategoryAndStateBetweenDates(text,
                        rangeStart.toInstant(ZoneOffset.UTC), rangeEnd.toInstant(ZoneOffset.UTC), categories, paging,
                        EventState.PUBLISHED, CONFIRMED, paid);
            }
        } else {
            if (rangeStart == null || rangeEnd == null) {
                events = eventRepository.findAllEventsWithStatusAfterDate(text, now().toInstant(ZoneOffset.UTC),
                        categories, EventState.PUBLISHED, paging, paid);
            } else {
                events = eventRepository.findAllEventsWithStatusBetweenDates(text,
                        rangeStart.toInstant(ZoneOffset.UTC), rangeEnd.toInstant(ZoneOffset.UTC), categories,
                        EventState.PUBLISHED, paging, paid);
            }
        }

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
        return getEventDtoFromEvent(event);
    }

    public Event getEventIfExist(long eventId) {
        return eventRepository
                .findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id " + eventId + " не существует!"));
    }

    public Map<Long, Integer> getEventsViewsMap(List<Long> eventsIds) {
        List<String> uris = new ArrayList<>();
        for (Long eventId : eventsIds) {
            uris.add("/events/" + eventId);
        }
        List<HashMap<Object, Object>> stats = getStats(uris);
        Map<Long, Integer> eventViewsMap = new HashMap<>();
        if (stats != null && !stats.isEmpty()) {
            for (var map : stats) {
                String uri = (String) map.get("uri");
                String[] urisAsArr = uri.split("/");
                Long id = Long.parseLong(urisAsArr[urisAsArr.length - 1]);
                eventViewsMap.put(id, (Integer) map.get("hits"));
            }
        }
        for (Long id : eventsIds) {
            if (!eventViewsMap.containsKey(id)) {
                eventViewsMap.put(id, 0);
            }
        }

        return eventViewsMap;
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
        Integer views = getEventsViews(event.getId());
        User owner = UserMapper.toUserFromUserDto(userClient.getUserById(event.getOwnerId()));

        return EventMapper.fromEventToEventDto(event,
                EventCategoryMapper.toCategoryDtoFromCategory(event.getCategory()),
                UserMapper.fromUserToUserShortDto(owner),
                confirmedRequests,
                views);
    }

    private List<HashMap<Object, Object>> getStats(List<String> uris) {
        return (List<HashMap<Object, Object>>) statsClient.getStats("2000-01-01 00:00:00",
                now().format(ofPattern("yyyy-MM-dd HH:mm:ss")),
                uris, false).getBody();
    }

    private List<Long> getEventsIdFromEventsList(List<Event> events) {
        return events.stream().map(Event::getId).toList();
    }

    private List<EventDto> getEventsFulls(List<Event> events) {
        List<Long> eventIds = getEventsIdFromEventsList(events);
        Map<Long, Long> confirmedRequestsCountForEvents = getConfirmedRequestsCountForEvents(events);
        Map<Long, Integer> viewsMap = getEventsViewsMap(new ArrayList<>(eventIds));
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
        Map<Long, Integer> viewsMap = getEventsViewsMap(new ArrayList<>(eventIds));
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
                viewsMap.get(event.getId()))).toList();
    }
}