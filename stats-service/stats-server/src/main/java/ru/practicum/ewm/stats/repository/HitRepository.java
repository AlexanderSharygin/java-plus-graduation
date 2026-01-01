package ru.practicum.ewm.stats.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.practicum.ewm.dto.StatsDto;
import ru.practicum.ewm.stats.model.Hit;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface HitRepository extends JpaRepository<Hit, Integer> {

    @Query("SELECT new ru.practicum.ewm.dto.StatsDto(a.name, h.uri, COUNT (h.id)) " +
            "FROM Hit h JOIN App a ON a.id=h.app.id " +
            "WHERE h.timestamp BETWEEN :startDateTime AND :endDateTime " +
            "GROUP BY  h.uri, a.name " +
            "ORDER BY COUNT(h.id) DESC")
    List<StatsDto> findAllHits(LocalDateTime startDateTime, LocalDateTime endDateTime);

    @Query("SELECT new ru.practicum.ewm.dto.StatsDto(name, uri)" +
            "FROM (select distinct h.ip as ip, a.name as name, h.uri as uri from Hit h JOIN App a ON a.id=h.app.id " +
            "WHERE h.timestamp BETWEEN :startDateTime AND :endDateTime)")
    List<StatsDto> findAllUniqueHits(LocalDateTime startDateTime, LocalDateTime endDateTime);

    @Query("SELECT new ru.practicum.ewm.dto.StatsDto(a.name, h.uri, count (h.id))" +
            "FROM  Hit h JOIN App a ON a.id=h.app.id " +
            "WHERE (h.timestamp BETWEEN :startDateTime AND :endDateTime) AND (h.uri IN (:uris))" +
            "GROUP BY h.uri, a.name " +
            "ORDER BY COUNT(h.id) DESC")
    List<StatsDto> findHitsByUris(LocalDateTime startDateTime, LocalDateTime endDateTime, List<String> uris);


    @Query("SELECT new ru.practicum.ewm.dto.StatsDto(name, uri)" +
            "FROM (select distinct h.ip as ip, a.name as name, h.uri as uri from Hit h JOIN App a ON a.id=h.app.id " +
            "WHERE h.timestamp BETWEEN :startDateTime AND :endDateTime  AND (h.uri IN (:uris)))")
    List<StatsDto> findUniqueHitsByUris(LocalDateTime startDateTime, LocalDateTime endDateTime, List<String> uris);
}
