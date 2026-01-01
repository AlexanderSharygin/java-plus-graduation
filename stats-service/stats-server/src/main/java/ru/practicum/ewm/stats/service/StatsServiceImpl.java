package ru.practicum.ewm.stats.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.dto.HitDto;
import ru.practicum.ewm.dto.StatsDto;
import ru.practicum.ewm.exception.model.BadRequestException;
import ru.practicum.ewm.stats.mapper.HitMapper;
import ru.practicum.ewm.stats.model.App;
import ru.practicum.ewm.stats.model.Hit;
import ru.practicum.ewm.stats.repository.AppRepository;
import ru.practicum.ewm.stats.repository.HitRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
@Slf4j
public class StatsServiceImpl implements StatsService {
    private final HitRepository hitRepository;
    private final AppRepository appRepository;

    @Override
    public List<StatsDto> getStats(LocalDateTime startRange, LocalDateTime endRange, List<String> uris, boolean unique) {

        if (startRange == null || endRange == null || startRange.isAfter(endRange) || startRange.equals(endRange)) {
            throw new BadRequestException("Неверный диапазон дат для выгрузки статистики");
        }
        List<StatsDto> stats;
        if (uris == null || uris.isEmpty()) {
            if (!unique) {
                stats = hitRepository.findAllHits(startRange, endRange);
            } else {
                stats = hitRepository.findAllUniqueHits(startRange, endRange);
                stats.forEach(k -> k.setHits(1L));
            }
        } else {
            if (!unique) {
                stats = hitRepository.findHitsByUris(startRange, endRange, uris);
            } else {
                stats = hitRepository.findUniqueHitsByUris(startRange, endRange, uris);
            }
        }

        return stats;
    }

    @Override
    public void hit(HitDto hitDto) {
        Hit hit = HitMapper.toHit(hitDto);
        hit.setTimestamp(LocalDateTime.now());
        Optional<App> existedApp = appRepository.findByName(hitDto.getApp());
        if (existedApp.isPresent()) {
            hit.setApp(existedApp.get());
        } else {
            App app = new App();
            app.setName(hitDto.getApp());
            hit.setApp(appRepository.save(app));
            log.info("В таблицу APP добавлено новое приложение - {}", app.getName());
        }
        hitRepository.save(hit);
        log.info("В таблицу HIT добавлено обращение к {} с IP {} от источника {}", hit.getUri(), hit.getIp(),
                hit.getApp().getName());
    }
}
