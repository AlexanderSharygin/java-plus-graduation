package ru.practicum.ewm.main.category.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.main.category.dto.EventCategoryDto;
import ru.practicum.ewm.main.category.dto.EventCategoryMapper;
import ru.practicum.ewm.main.category.model.EventCategory;
import ru.practicum.ewm.main.category.repository.EventCategoryRepository;
import ru.practicum.ewm.main.event.model.Event;
import ru.practicum.ewm.main.event.repository.EventRepository;
import ru.practicum.ewm.main.exception.model.ConflictException;
import ru.practicum.ewm.main.exception.model.NotFoundException;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EventCategoryService {

    private final EventCategoryRepository categoryRepository;
    private final EventRepository eventRepository;

    public List<EventCategoryDto> getAll(Pageable pageable) {
        return categoryRepository.findAll(pageable).stream()
                .map(EventCategoryMapper::toCategoryDtoFromCategory).toList();
    }

    public EventCategoryDto getById(long catId) {
        EventCategory category = getCategoryIfExist(catId);
        return EventCategoryMapper.toCategoryDtoFromCategory(category);
    }

    public EventCategoryDto update(long catId, EventCategoryDto eventCategoryDto) {
        EventCategory categoryToUpdate = getCategoryIfExist(catId);
        Optional<EventCategory> categoryWithSameName = categoryRepository.findByName(eventCategoryDto.getName());
        if (categoryWithSameName.isPresent() && !categoryWithSameName.get().getId().equals(categoryToUpdate.getId())) {
            throw new ConflictException("Категория " + eventCategoryDto.getName() + " уже существует!");
        }
        categoryToUpdate.setName(eventCategoryDto.getName());
        EventCategory updatedCategory = categoryRepository.save(categoryToUpdate);

        return EventCategoryMapper.toCategoryDtoFromCategory(updatedCategory);
    }

    public EventCategoryDto create(EventCategoryDto eventCategoryDto) {
        Optional<EventCategory> categoryWithSameName = categoryRepository.findByName(eventCategoryDto.getName());
        if (categoryWithSameName.isPresent()) {
            throw new ConflictException("Категория " + eventCategoryDto.getName() + " уже существует!");
        }

        EventCategory category = categoryRepository.save(EventCategoryMapper.toCategoryFromCategoryDto(eventCategoryDto));
        return EventCategoryMapper.toCategoryDtoFromCategory(category);
    }

    public void delete(long catId) {
        EventCategory category = getCategoryIfExist(catId);
        List<Event> eventsList = eventRepository.findAllByCategory(category);
        if (!eventsList.isEmpty()) {
            throw new ConflictException("У категории есть события. Удаление невозможно!");
        }
        categoryRepository.delete(category);
    }

    private EventCategory getCategoryIfExist(long catId) {
        return categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Категория с id " + catId + " не существует!"));
    }
}
