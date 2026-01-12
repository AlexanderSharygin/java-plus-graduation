package ru.practicum.ewm.compilation.service;

import ru.practicum.ewm.AnalyzerClient;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.compilation.repository.CompilationRepository;
import ru.practicum.ewm.dto.compilation.CompilationDto;
import ru.practicum.ewm.dto.compilation.CompilationRequestDto;
import ru.practicum.ewm.dto.event.EventShortDto;
import ru.practicum.ewm.dto.user.UserDto;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.event.service.EventService;
import ru.practicum.ewm.exception.BadRequestException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.feign_clients.UserClient;
import ru.practicum.ewm.mapper.compilation.CompilationMapper;
import ru.practicum.ewm.mapper.event.EventCategoryMapper;
import ru.practicum.ewm.mapper.event.EventMapper;
import ru.practicum.ewm.mapper.user.UserMapper;
import ru.practicum.ewm.model.compilation.Compilation;
import ru.practicum.ewm.model.event.Event;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final EventService eventService;
    private final AnalyzerClient analyzerClient;
    private final UserClient userClient;

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

    private Set<EventShortDto> getEventsShorts(Set<Event> events) {
        List<Long> eventIds = events.stream().map(Event::getId).toList();
        Map<Long, Long> confirmedRequestsCountForEvents = eventService
                .getConfirmedRequestsCountForEvents(new ArrayList<>(events));
        Map<Long, Double> viewsMap = analyzerClient.getInteractionsCount(new ArrayList<>(eventIds));

        Map<Long, Long> eventsOwnersId = new HashMap<>();
        for (var event : events) {
            eventsOwnersId.put(event.getId(), event.getOwnerId());
        }
        List<UserDto> owners = userClient.getUsers(eventsOwnersId.values().stream().toList());

        return events.stream()
                .map(event -> EventMapper.fromEventToEventShortDto(event,
                        EventCategoryMapper.toCategoryDtoFromCategory(event.getCategory()),
                        UserMapper.toUserShortDtoFromUserDto(Objects.requireNonNull(owners.stream()
                                .filter(k -> k.getId() == event.getOwnerId())
                                .findFirst().orElse(null))),
                        confirmedRequestsCountForEvents.getOrDefault(event.getId(), 0L),
                        viewsMap.get(event.getId()))).collect(Collectors.toSet());
    }
}
