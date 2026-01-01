package ru.practicum.ewm.main.compilation.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.client.StatsClient;
import ru.practicum.ewm.main.category.dto.EventCategoryMapper;
import ru.practicum.ewm.main.compilation.dto.CompilationDto;
import ru.practicum.ewm.main.compilation.dto.CompilationRequestDto;
import ru.practicum.ewm.main.compilation.mapper.CompilationMapper;
import ru.practicum.ewm.main.compilation.model.Compilation;
import ru.practicum.ewm.main.compilation.repository.CompilationRepository;
import ru.practicum.ewm.main.event.dto.EventShortDto;
import ru.practicum.ewm.main.event.mapper.EventMapper;
import ru.practicum.ewm.main.event.model.Event;
import ru.practicum.ewm.main.event.repository.EventRepository;
import ru.practicum.ewm.main.event.service.EventService;
import ru.practicum.ewm.main.exception.model.BadRequestException;
import ru.practicum.ewm.main.exception.model.NotFoundException;
import ru.practicum.ewm.main.user.dto.UserMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final EventService eventService;

    private final StatsClient statClient;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    public CompilationService(CompilationRepository compilationRepository, EventRepository eventRepository,
                              EventService eventService, StatsClient statClient) {
        this.compilationRepository = compilationRepository;
        this.eventRepository = eventRepository;
        this.eventService = eventService;
        this.statClient = statClient;
    }

    public List<CompilationDto> getAll(boolean pinned, Pageable pageable) {
        List<Compilation> compilations = compilationRepository
                .getAllByPinned(pinned, pageable).stream().toList();
        List<CompilationDto> result = new ArrayList<>();
        for (Compilation compilation : compilations) {
            Set<EventShortDto> items = new HashSet<>();
            if (compilation.getEvents() != null && !compilation.getEvents().isEmpty()) {
                Set<Event> eventSet = compilation.getEvents();
                items = getEventsShorts(eventSet);
            }
            result.add(CompilationMapper.toDtoFromCompilation(compilation, items));
        }

        return result;
    }

    public CompilationDto getById(long id) {
        Compilation compilation = compilationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Подборка с id " + id + " не найдена в БД"));

        Set<EventShortDto> items = new HashSet<>();

        if (compilation.getEvents() != null && !compilation.getEvents().isEmpty()) {
            Set<Event> eventSet = compilation.getEvents();
            items = getEventsShorts(eventSet);
        }

        return CompilationMapper.toDtoFromCompilation(compilation, items);
    }

    public CompilationDto create(CompilationRequestDto compilationDto) {
        Set<Long> eventIds = new HashSet<>();
        if (compilationDto.getEvents() != null) {
            eventIds.addAll(compilationDto.getEvents());
        }
        if (compilationDto.getTitle() == null || compilationDto.getTitle().isEmpty() || compilationDto.getTitle().isBlank()) {
            throw new BadRequestException("Title не может быть пустым");
        }
        Set<Event> eventSet = new HashSet<>();
        Set<EventShortDto> items = new HashSet<>();
        if (!eventIds.isEmpty()) {
            eventSet = new HashSet<>(eventRepository.findAllById(eventIds));
            if (eventIds.size() == eventSet.size()) {
                items = getEventsShorts(eventSet);
            } else {
                throw new NotFoundException("Некоторые события не найдены");
            }
        }

        Compilation compilationToSave = CompilationMapper.toCompilationFromDto(compilationDto, eventSet);
        if (compilationDto.getPinned() == null) {
            compilationToSave.setPinned(false);
        }
        Compilation compilation = compilationRepository.save(compilationToSave);

        return CompilationMapper.toDtoFromCompilation(compilation, items);
    }

    public void delete(long compilationId) {
        Compilation compilation = compilationRepository.findById(compilationId)
                .orElseThrow(() -> new NotFoundException("Подборка с id " + compilationId + " не существует!"));
        compilationRepository.delete(compilation);
    }

    public CompilationDto updateCompilation(long compilationId, CompilationRequestDto updateCompilationRequest) {
        Compilation existedCompilation = compilationRepository.findById(compilationId)
                .orElseThrow(() -> new NotFoundException("Подборка с id " + compilationId + " не существует!"));
        Set<Event> eventSet;
        if (updateCompilationRequest.getEvents() != null && !updateCompilationRequest.getEvents().isEmpty()) {
            eventSet = new HashSet<>(eventRepository.findAllById(updateCompilationRequest.getEvents()));
            if (updateCompilationRequest.getEvents().size() == eventSet.size()) {
                existedCompilation.setEvents(eventSet);
            } else {
                throw new NotFoundException("Некоторые события не найдены");
            }
        }
        eventSet = existedCompilation.getEvents();
        if (updateCompilationRequest.getTitle() != null && !updateCompilationRequest.getTitle().isBlank()) {
            existedCompilation.setTitle(updateCompilationRequest.getTitle());
        }
        if (updateCompilationRequest.getPinned() != null) {
            existedCompilation.setPinned(updateCompilationRequest.getPinned());
        }
        Set<EventShortDto> eventShortDtos = getEventsShorts(eventSet);
        Compilation resultCompilation = compilationRepository.save(existedCompilation);

        return CompilationMapper.toDtoFromCompilation(resultCompilation, eventShortDtos);
    }

    public Map<Long, Integer> getEventsViewsMap(List<Long> eventsIds) {
        List<String> uris = new ArrayList<>();
        for (Long eventId : eventsIds) {
            uris.add("/events/" + eventId);
        }
        List<HashMap<Object, Object>> stats = (List<HashMap<Object, Object>>) statClient.getStats(
                "2000-01-01 00:00:00", LocalDateTime.now().format(formatter), uris, false).getBody();
        Map<Long, Integer> eventViewsMap = new HashMap<>();
        if (stats != null && !stats.isEmpty()) {
            stats.forEach(map -> {
                String uri = (String) map.get("uri");
                String[] urisAsArr = uri.split("/");
                Long id = Long.parseLong(urisAsArr[urisAsArr.length - 1]);
                eventViewsMap.put(id, (Integer) map.get("hits"));
            });
        }
        for (Long id : eventsIds) {
            if (!eventViewsMap.containsKey(id)) {
                eventViewsMap.put(id, 0);
            }
        }

        return eventViewsMap;
    }

    private Set<EventShortDto> getEventsShorts(Set<Event> events) {
        List<Long> eventIds = events.stream().map(Event::getId).toList();
        Map<Long, Long> confirmedRequestsCountForEvents = eventService
                .getConfirmedRequestsCountForEvents(new ArrayList<>(events));
        Map<Long, Integer> viewsMap = getEventsViewsMap(new ArrayList<>(eventIds));

        return events.stream()
                .map(event -> EventMapper.fromEventToEventShortDto(event,
                        EventCategoryMapper.toCategoryDtoFromCategory(event.getCategory()),
                        UserMapper.fromUserToUserShortDto(event.getOwner()),
                        confirmedRequestsCountForEvents.getOrDefault(event.getId(), 0L),
                        viewsMap.get(event.getId()))).collect(Collectors.toSet());
    }
}
