package ru.practicum.ewm.service;

import feign.FeignException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.dto.request.RequestDto;
import ru.practicum.ewm.dto.request.RequestMapper;
import ru.practicum.ewm.dto.request.RequestStatusUpdateRequest;
import ru.practicum.ewm.dto.request.RequestStatusUpdateResponse;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.feign_clients.EventClient;
import ru.practicum.ewm.feign_clients.UserClient;
import ru.practicum.ewm.mapper.event.EventMapper;
import ru.practicum.ewm.mapper.user.UserMapper;
import ru.practicum.ewm.model.event.Event;
import ru.practicum.ewm.model.request.ParticipationRequest;
import ru.practicum.ewm.model.user.User;
import ru.practicum.ewm.repository.RequestRepository;

import java.time.LocalDateTime;
import java.util.*;

import static ru.practicum.ewm.model.request.RequestStatus.*;


@Service
public class RequestService {

    private final RequestRepository requestRepository;
    private final UserClient userClient;
    private final EventClient eventClient;

    @Autowired
    public RequestService(RequestRepository requestRepository, UserClient userClient, EventClient eventClient) {
        this.requestRepository = requestRepository;
        this.userClient = userClient;
        this.eventClient = eventClient;
    }

    public List<RequestDto> getEventRequests(Long userId, Long eventId) {
        checkUserIfExists(userId);
        Event event = checkEventIsPublished(eventId);
        if (!Objects.equals(event.getOwner().getId(), userId)) {
            throw new NotFoundException("User c id " + userId + " не хозяин для события " + eventId);
        }
        List<ParticipationRequest> result = requestRepository.findByEvent(event);

        return result.stream().map(RequestMapper::fromRequestTpRequestDto).toList();
    }

    public RequestStatusUpdateResponse updateRequest(Long userId, Long eventId, RequestStatusUpdateRequest request) {
        checkUserIfExists(userId);
        Event event = checkEventIsPublished(eventId);
        List<ParticipationRequest> requests = requestRepository.findAllByIdIn(request.getRequestIds());
        Set<RequestDto> confirmed = new HashSet<>();
        Set<RequestDto> rejected = new HashSet<>();
        RequestStatusUpdateResponse result = new RequestStatusUpdateResponse(confirmed, rejected);
        List<ParticipationRequest> pendingRequests = requests.stream()
                .filter(request1 -> request1.getStatus().equals(PENDING)).toList();

        if (pendingRequests.isEmpty()) {
            throw new ConflictException("Запрос не найден");
        }
        long confirmedRequestsCount = requestRepository.findAllByEventAndStatus(event, CONFIRMED).size();
        if (!event.getIsModerated() || event.getParticipantLimit() == 0) {
            requests.forEach(req -> req.setStatus(CONFIRMED));
            result.getConfirmedRequests().addAll(requests.stream()
                    .map(RequestMapper::fromRequestTpRequestDto)
                    .toList());
            requestRepository.saveAll(requests);

            return result;
        }
        if ((confirmedRequestsCount + request.getRequestIds().size()) > event.getParticipantLimit()) {
            throw new ConflictException("Для события с id " + eventId + " достигнут лимит участников");
        }

        if ((confirmedRequestsCount + request.getRequestIds().size()) == event.getParticipantLimit() &&
                request.getStatus().equals(CONFIRMED)) {
            requests.forEach(req -> req.setStatus(REJECTED));
            confirmed.addAll(requests.stream().map(RequestMapper::fromRequestTpRequestDto).toList());
            requestRepository.saveAll(requests);
            result.setConfirmedRequests(confirmed);

            List<ParticipationRequest> otherPendingRequests = requestRepository
                    .findAllByEventAndStatus(event, PENDING);
            otherPendingRequests.forEach(req -> req.setStatus(REJECTED));
            requestRepository.saveAll(otherPendingRequests);
            rejected.addAll(otherPendingRequests.stream().map(RequestMapper::fromRequestTpRequestDto).toList());
            result.setRejectedRequests(rejected);
            return result;
        }

        if (request.getStatus().equals(CONFIRMED)) {
            requests.forEach(req -> req.setStatus(CONFIRMED));
            confirmed.addAll(requests.stream().map(RequestMapper::fromRequestTpRequestDto).toList());
            requestRepository.saveAll(requests);
            result.setConfirmedRequests(confirmed);
        } else if (request.getStatus().equals(REJECTED)) {
            requests.forEach(req -> req.setStatus(REJECTED));
            rejected.addAll(requests.stream().map(RequestMapper::fromRequestTpRequestDto).toList());
            requestRepository.saveAll(requests);
            result.setRejectedRequests(rejected);
        }

        return result;
    }

    public List<RequestDto> getByUserId(Long userId) {
        List<ParticipationRequest> result = requestRepository.
                findAllByRequester(UserMapper.toUserFromUserDto(userClient.getUserById(userId)));

        return result.stream().map(RequestMapper::fromRequestTpRequestDto).toList();

    }

    public RequestDto create(Long userId, Long eventId) {
        User user = checkUserIfExists(userId);
        Event event = checkEventIsPublished(eventId);

        if (Objects.equals(user.getId(), event.getOwner().getId())) {
            throw new ConflictException("User c id " + userId + " не хозяин для события " + eventId);
        }
        if (event.getPublishedOn() == null) {
            throw new ConflictException("Событие с id " + eventId + " еще не опубликовано");
        }

        long confirmedRequestsCount = requestRepository.findAllByEventAndStatus(event, CONFIRMED).size();
        if (confirmedRequestsCount == event.getParticipantLimit() && event.getParticipantLimit() > 0) {
            throw new ConflictException(
                    "Лимит участников для события " + event.getId() + " превышен");
        }
        if (!requestRepository.findAllByEventAndRequester(event, user).isEmpty()) {
            throw new ConflictException("Запрос на участие в событии " + event.getId() +
                    " уже существует для пользователя с id " + user.getId());
        }
        ParticipationRequest request = new ParticipationRequest(null, LocalDateTime.now(), event, user, PENDING);
        if (!event.getIsModerated()) {
            request.setStatus(CONFIRMED);
        }
        if (event.getParticipantLimit() == 0) {
            request.setStatus(CONFIRMED);
        }
        ParticipationRequest result = requestRepository.save(request);

        return RequestMapper.fromRequestTpRequestDto(result);
    }

    public RequestDto cancelRequestByUser(Long userId, Long requestId) {
        User user = checkUserIfExists(userId);
        ParticipationRequest request = requestRepository
                .findById(requestId)
                .orElseThrow(() -> new NotFoundException("Запрос с id=" + requestId + " не существует!"));
        if (!Objects.equals(request.getRequester().getId(), user.getId())) {
            throw new ConflictException("User c id " + userId + " не хозяин для запроса " + requestId);
        }
        request.setStatus(CANCELED);
        ParticipationRequest result = requestRepository.save(request);

        return RequestMapper.fromRequestTpRequestDto(result);
    }

    public List<RequestDto> getConfirmedRequestsForEvent(List<Event> events) {
        List<ParticipationRequest> requests = requestRepository.findAllByEventInAndStatus(new ArrayList<>(events),
                CONFIRMED);

        return requests.stream().map(RequestMapper::fromRequestTpRequestDto).toList();
    }

    private Event checkEventIsPublished(Long eventId) {
        try {
            return EventMapper.fromEventDtoToEvent(eventClient.getEventById(eventId));
        } catch (FeignException.NotFound e) {
            throw new ConflictException("Событие с id" + eventId + " ещё не опубликовано");
        }
    }

    private User checkUserIfExists(Long userId) {
        try {
            return UserMapper.toUserFromUserDto(userClient.getUserById(userId));
        } catch (FeignException.NotFound e) {
            throw new NotFoundException("User c id" + userId + " не существует!");
        }
    }
}