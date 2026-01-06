package ru.practicum.ewm.mapper.event;

import lombok.NoArgsConstructor;
import ru.practicum.ewm.dto.catergory.EventCategoryDto;
import ru.practicum.ewm.dto.event.CreateNewEventDto;
import ru.practicum.ewm.dto.event.EventDto;
import ru.practicum.ewm.dto.event.EventShortDto;
import ru.practicum.ewm.dto.user.UserShortDto;
import ru.practicum.ewm.model.category.EventCategory;
import ru.practicum.ewm.model.event.Event;
import ru.practicum.ewm.model.event.EventState;
import ru.practicum.ewm.model.user.User;

import java.time.ZoneId;
import java.time.ZoneOffset;

import static java.time.LocalDateTime.now;
import static java.time.LocalDateTime.ofInstant;

@NoArgsConstructor
public class EventMapper {


    public static Event fromCreateNewEventDtoToEvent(CreateNewEventDto newEventDto, User owner, EventCategory category) {
        return new Event(null,
                newEventDto.getTitle(),
                newEventDto.getAnnotation(),
                newEventDto.getDescription(),
                category,
                now().toInstant(ZoneOffset.UTC),
                newEventDto.getEventDate().toInstant(ZoneOffset.UTC),
                owner,
                newEventDto.getLocation(),
                newEventDto.getPaid(),
                newEventDto.getParticipantLimit(),
                null,
                newEventDto.getRequestModeration(),
                EventState.PENDING);
    }

    public static Event fromEventDtoToEvent(EventDto eventDto) {
        User user = new User();
        user.setId(eventDto.getInitiator().getId());
        user.setName(eventDto.getInitiator().getName());
        EventCategory category = EventCategoryMapper.toCategoryFromCategoryDto(eventDto.getCategory());

        return new Event(eventDto.getId(),
                eventDto.getTitle(),
                eventDto.getAnnotation(),
                eventDto.getDescription(),
                category,
                now().toInstant(ZoneOffset.UTC),
                eventDto.getEventDate().toInstant(ZoneOffset.UTC),
                user,
                eventDto.getLocation(),
                eventDto.getPaid(),
                eventDto.getParticipantLimit(),
                eventDto.getPublishedOn().toInstant(ZoneOffset.UTC),
                eventDto.getRequestModeration(),
                eventDto.getState());
    }

    public static EventDto fromEventToEventDto(Event event, EventCategoryDto eventCategoryDto, UserShortDto owner,
                                               Long confirmedRequests, Integer views) {
        EventDto eventDto = new EventDto(event.getId(),
                event.getAnnotation(),
                eventCategoryDto,
                confirmedRequests,
                ofInstant(event.getCreatedOn(), ZoneId.of("UTC")),
                event.getDescription(),
                ofInstant(event.getEventDateTime(), ZoneId.of("UTC")),
                owner,
                event.getLocation(),
                event.getIsPaid(),
                event.getParticipantLimit(),
                null,
                event.getIsModerated(),
                event.getState(),
                event.getTitle(),
                views
        );
        if (event.getPublishedOn() != null) {
            eventDto.setPublishedOn(ofInstant(event.getPublishedOn(), ZoneId.of("UTC")));
        }
        return eventDto;
    }

    public static EventShortDto fromEventToEventShortDto(Event event, EventCategoryDto eventCategoryDto, UserShortDto owner,
                                                         Long confirmedRequests, Integer views) {
        return new EventShortDto(event.getId(),
                event.getAnnotation(),
                eventCategoryDto,
                confirmedRequests,
                ofInstant(event.getEventDateTime(), ZoneId.of("UTC")),
                owner,
                event.getIsPaid(),
                event.getTitle(),
                views);
    }
}
