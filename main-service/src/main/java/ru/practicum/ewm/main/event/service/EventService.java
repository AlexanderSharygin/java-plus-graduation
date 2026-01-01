package ru.practicum.ewm.main.event.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.client.StatsClient;
import ru.practicum.ewm.main.category.dto.EventCategoryMapper;
import ru.practicum.ewm.main.category.model.EventCategory;
import ru.practicum.ewm.main.category.repository.EventCategoryRepository;
import ru.practicum.ewm.main.event.dto.*;
import ru.practicum.ewm.main.event.mapper.EventMapper;
import ru.practicum.ewm.main.event.model.AdminEventAction;
import ru.practicum.ewm.main.event.model.Event;
import ru.practicum.ewm.main.event.model.EventState;
import ru.practicum.ewm.main.event.model.Location;
import ru.practicum.ewm.main.event.repository.EventRepository;
import ru.practicum.ewm.main.event.repository.LocationRepository;
import ru.practicum.ewm.main.exception.model.BadRequestException;
import ru.practicum.ewm.main.exception.model.ConflictException;
import ru.practicum.ewm.main.exception.model.NotFoundException;
import ru.practicum.ewm.main.request.model.ParticipationRequest;
import ru.practicum.ewm.main.request.repository.RequestRepository;
import ru.practicum.ewm.main.user.dto.UserMapper;
import ru.practicum.ewm.main.user.model.User;
import ru.practicum.ewm.main.user.service.UserService;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static java.time.LocalDateTime.now;
import static ru.practicum.ewm.main.request.model.RequestStatus.CONFIRMED;

@Service
public class EventService {

    private final EventCategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final LocationRepository locationRepository;
    private final RequestRepository requestRepository;
    private final UserService userService;
    private final StatsClient statsClient;

    @Autowired
    public EventService(EventCategoryRepository categoryRepository, EventRepository eventRepository,
                        LocationRepository locationRepository, RequestRepository requestRepository,
                        UserService userService, StatsClient statsClient) {
        this.categoryRepository = categoryRepository;
        this.eventRepository = eventRepository;
        this.locationRepository = locationRepository;
        this.requestRepository = requestRepository;
        this.userService = userService;
        this.statsClient = statsClient;
    }

    public EventDto create(CreateNewEventDto eventDto, Long userId) {
        User owner = userService.getUserIfExist(userId);
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
        if (event.getPublishedOn() != null && updateEventDto.getStateAction().equals(AdminEventAction.REJECT_EVENT)) {
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
        userService.getUserIfExist(userId);

        if (!Objects.equals(event.getOwner().getId(), userId)) {
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
        User user = userService.getUserIfExist(userId);
        List<Event> events = eventRepository.findAllByOwner(user, paging).stream().toList();

        return getEventsShorts(events);
    }

    public EventDto getEventByUserId(Long userId, Long eventId) {
        Event event = getEventIfExist(eventId);
        userService.getUserIfExist(userId);
        if (!Objects.equals(event.getOwner().getId(), userId)) {
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
        List<ParticipationRequest> requests = requestRepository.findAllByEventInAndStatus(new ArrayList<>(events),
                CONFIRMED);
        Set<Long> requestsIds = new HashSet<>();
        for (var request : requests) {
            requestsIds.add(request.getEvent().getId());
        }
        Map<Long, Long> confirmedRequestsCountForEvents = new HashMap<>();
        for (var id : requestsIds) {
            int count = (int) requests.stream()
                    .filter(k -> Objects.equals(k.getEvent().getId(), id)).count();
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
        long confirmedRequests = requestRepository.findAllByEventInAndStatus(List.of(event), CONFIRMED).size();
        Integer views = getEventsViews(event.getId());

        return EventMapper.fromEventToEventDto(event,
                EventCategoryMapper.toCategoryDtoFromCategory(event.getCategory()),
                UserMapper.fromUserToUserShortDto(event.getOwner()),
                confirmedRequests,
                views);
    }

    private List<HashMap<Object, Object>> getStats(List<String> uris) {
        return (List<HashMap<Object, Object>>) statsClient.getStats("2000-01-01 00:00:00",
                now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                uris, false).getBody();
    }

    private List<Long> getEventsIdFromEventsList(List<Event> events) {
        return events.stream().map(Event::getId).toList();
    }

    private List<EventDto> getEventsFulls(List<Event> events) {
        List<Long> eventIds = getEventsIdFromEventsList(events);
        Map<Long, Long> confirmedRequestsCountForEvents = getConfirmedRequestsCountForEvents(events);
        Map<Long, Integer> viewsMap = getEventsViewsMap(new ArrayList<>(eventIds));

        return events.stream().map(event -> EventMapper.fromEventToEventDto(event,
                EventCategoryMapper.toCategoryDtoFromCategory(event.getCategory()),
                UserMapper.fromUserToUserShortDto(event.getOwner()),
                confirmedRequestsCountForEvents.getOrDefault(event.getId(), 0L),
                viewsMap.get(event.getId()))).toList();
    }

    private List<EventShortDto> getEventsShorts(List<Event> events) {
        List<Long> eventIds = getEventsIdFromEventsList(events);
        Map<Long, Long> confirmedRequestsCountForEvents = getConfirmedRequestsCountForEvents(events);
        Map<Long, Integer> viewsMap = getEventsViewsMap(new ArrayList<>(eventIds));

        return events.stream().map(event -> EventMapper.fromEventToEventShortDto(event,
                EventCategoryMapper.toCategoryDtoFromCategory(event.getCategory()),
                UserMapper.fromUserToUserShortDto(event.getOwner()),
                confirmedRequestsCountForEvents.getOrDefault(event.getId(), 0L),
                viewsMap.get(event.getId()))).toList();
    }
}