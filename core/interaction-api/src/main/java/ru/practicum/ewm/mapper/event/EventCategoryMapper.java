package ru.practicum.ewm.mapper.event;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.model.category.EventCategory;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EventCategoryMapper {

    public static ru.practicum.ewm.dto.catergory.EventCategoryDto toCategoryDtoFromCategory(EventCategory category) {
        return new ru.practicum.ewm.dto.catergory.EventCategoryDto(category.getId(), category.getName());
    }

    public static EventCategory toCategoryFromCategoryDto(ru.practicum.ewm.dto.catergory.EventCategoryDto eventCategoryDto) {
        return new EventCategory(-1L, eventCategoryDto.getName());
    }
}
