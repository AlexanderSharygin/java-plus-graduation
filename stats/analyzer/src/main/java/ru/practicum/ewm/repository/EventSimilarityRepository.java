package ru.practicum.ewm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.ewm.models.EventSimilarity;


import java.util.List;

public interface EventSimilarityRepository extends JpaRepository<EventSimilarity, Long> {

    List<EventSimilarity> findByEventAOrEventB(long eventId, long eventId1);

    List<EventSimilarity> findByEventAAndEventB(Long eventA, Long eventB);
}
