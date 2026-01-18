package ru.practicum.ewm.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.models.EventSimilarity;
import ru.practicum.ewm.models.UserAction;
import ru.practicum.ewm.models.UserActionType;
import ru.practicum.ewm.repository.EventSimilarityRepository;
import ru.practicum.ewm.repository.UserActionRepository;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsumerHandler {
    private final EventSimilarityRepository eventSimilarityRepository;
    private final UserActionRepository userActionRepository;

    public void handleUserAction(UserActionAvro userActionAvro) {
        UserAction userAction = UserAction.builder()
                .userId(userActionAvro.getUserId())
                .eventId(userActionAvro.getEventId())
                .actionType(UserActionType.valueOf(userActionAvro.getActionType().name()))
                .timestamp(userActionAvro.getTimestamp().atZone(ZoneId.of("UTC")).toLocalDateTime())
                .build();
        List<UserAction> actions = userActionRepository.findByUserIdAndEventId(userActionAvro.getUserId(), userActionAvro.getEventId());
        if (actions.isEmpty()) {
            userActionRepository.save(userAction);
        } else {
            UserAction oldAction = actions.getFirst();
            if (getScore(oldAction.getActionType()) < getScore(userAction.getActionType())) {
                userActionRepository.delete(oldAction);
                userActionRepository.save(userAction);
            }
        }
    }

    public void handleEventsSimilarity(EventSimilarityAvro eventSimilarityAvro) {
        EventSimilarity eventSimilarity = EventSimilarity.builder()
                .eventA(eventSimilarityAvro.getEventA())
                .eventB(eventSimilarityAvro.getEventB())
                .score(eventSimilarityAvro.getScore())
                .timestamp(eventSimilarityAvro.getTimestamp().atZone(ZoneId.of("UTC")).toLocalDateTime())
                .build();
        List<EventSimilarity> similarities = eventSimilarityRepository.findByEventAAndEventB(eventSimilarity.getEventA(), eventSimilarity.getEventB());
        if (similarities.isEmpty()) {
            eventSimilarityRepository.save(eventSimilarity);
        } else {
            EventSimilarity oldSimilarity = similarities.getFirst();
            oldSimilarity.setScore(eventSimilarity.getScore());
            oldSimilarity.setTimestamp(eventSimilarity.getTimestamp());
            eventSimilarityRepository.save(oldSimilarity);
        }
    }

    private double getScore(UserActionType type) {
        return switch (type) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };
    }
}