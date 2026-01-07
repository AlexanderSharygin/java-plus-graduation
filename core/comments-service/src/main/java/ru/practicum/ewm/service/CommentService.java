package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.comments.CommentDto;
import ru.practicum.ewm.dto.comments.MergeCommentRequest;
import ru.practicum.ewm.dto.event.EventCommentDto;
import ru.practicum.ewm.dto.user.UserDto;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.exception.PublicationException;
import ru.practicum.ewm.feign_clients.EventClient;
import ru.practicum.ewm.feign_clients.UserClient;
import ru.practicum.ewm.mapper.comment.CommentMapper;
import ru.practicum.ewm.mapper.event.EventMapper;
import ru.practicum.ewm.mapper.user.UserMapper;
import ru.practicum.ewm.model.comments.Comment;
import ru.practicum.ewm.model.event.Event;
import ru.practicum.ewm.model.event.EventState;
import ru.practicum.ewm.model.user.User;
import ru.practicum.ewm.repository.CommentRepository;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final UserClient userClient;
    private final EventClient eventClient;

    public CommentDto createComment(MergeCommentRequest mergeCommentRequest, Long userId) {
        User user = UserMapper.toUserFromUserDto(userClient.getUserById(userId));
        Event event = EventMapper.fromEventDtoToEvent(eventClient.getEventById(mergeCommentRequest.getEventId()));
        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new PublicationException("Event must be published");
        }
        Comment comment = CommentMapper.fromCommentDtoToComment(mergeCommentRequest, event, user);
        Comment savedComment = commentRepository.save(comment);
        CommentDto response = CommentMapper.fromCommentToCommentDto(savedComment, event, user);
        log.info("Comment id={} was created by user id={}", response.getId(), response.getAuthor().getId());

        return response;
    }

    public void deleteCommentByIdAndAuthor(Long commentId, Long userId) {
        if (commentRepository.deleteCommentByIdAndAuthorId(commentId, userId) != 0) {
            log.info("Comment with id={} was deleted by user id={}", commentId, userId);
        } else {
            throw new NotFoundException(String.format("Comment with id=%d by author id=%d was not found", commentId, userId));
        }
    }

    public CommentDto updateCommentByIdAndAuthorId(Long commentId, Long userId, MergeCommentRequest request) {
        Comment oldComment = commentRepository.findByIdAndAuthorId(commentId, userId).orElseThrow(() ->
                new NotFoundException(String.format("Comment with id=%d by author id=%d was not found", commentId, userId)));
        if (!oldComment.getEventId().equals(request.getEventId())) {
            throw new DataIntegrityViolationException("Event Id not correct");
        }
        oldComment.setText(request.getText());
        Comment updatedComment = commentRepository.save(oldComment);
        User user = UserMapper.toUserFromUserDto(userClient.getUserById(updatedComment.getAuthorId()));
        Event event = EventMapper.fromEventDtoToEvent(eventClient.getEventById(updatedComment.getEventId()));
        CommentDto response = CommentMapper.fromCommentToCommentDto(updatedComment, event, user);
        log.info("Comment id={} was updated by user id={}", response.getId(), response.getAuthor().getId());

        return response;
    }

    public Collection<CommentDto> getAllCommentsByUser(Long userId, Integer from, Integer size) {
        log.info("Get all comments for user id={}", userId);
        List<Comment> comments = commentRepository.findAllByAuthorIdOrderByCreatedAtDesc(userId, createPageable(from, size)).stream().toList();
        HashMap<Long, Long> eventsComments = new HashMap<>();
        for (Comment comment : comments) {
            eventsComments.put(comment.getEventId(), comment.getId());
        }
        List<EventCommentDto> eventsDto = eventClient.getEventsDtoForComments(eventsComments.keySet().stream().toList());
        UserDto usersDto = userClient.getUserById(userId);

        return comments.stream().map(k -> CommentMapper.fromCommentToCommentDto(k,
                eventsDto.stream().filter(i -> i.getId() == k.getEventId()).findFirst().orElse(null),
                UserMapper.toUserFromUserDto(usersDto))).toList();
    }

    public Collection<CommentDto> getAllCommentsByEvent(Long eventId, Integer from, Integer size) {
        log.info("Get all comments for event id={}", eventId);
        List<Comment> comments = commentRepository.findAllByEventIdOrderByCreatedAtDesc(eventId, createPageable(from, size)).stream().toList();
        HashMap<Long, Long> usersComments = new HashMap<>();
        for (Comment comment : comments) {
            usersComments.put(comment.getAuthorId(), comment.getId());
        }
        List<UserDto> usersDto = userClient.getUsers(usersComments.keySet().stream().toList());
        Event event = EventMapper.fromEventDtoToEvent(eventClient.getEventById(eventId));

        return comments.stream().map(k -> CommentMapper.fromCommentToCommentDto(k, event,
                UserMapper.toUserFromUserDto(Objects.requireNonNull(usersDto.stream()
                        .filter(i -> i.getId() == k.getAuthorId())
                        .findFirst().orElse(null))))).toList();
    }

    public Collection<CommentDto> getAllCommentsByUserAndEvent(Long userId, Long eventId, Integer from, Integer size) {
        log.info("Get all comments for event id={} and user id={}", eventId, userId);
        User user = UserMapper.toUserFromUserDto(userClient.getUserById(userId));
        Event event = EventMapper.fromEventDtoToEvent(eventClient.getEventById(eventId));

        return commentRepository.findAllByAuthorIdAndEventIdOrderByCreatedAtDesc(userId, eventId, createPageable(from, size))
                .stream()
                .map(item -> CommentMapper.fromCommentToCommentDto(item, event, user))
                .toList();
    }

    public CommentDto getCommentById(Long commentId) {
        log.info("Get comment with id={}", commentId);
        Comment comment = commentRepository.findById(commentId).orElseThrow(() ->
                new NotFoundException(String.format("Comment with id=%d was not found", commentId)));
        User user = UserMapper.toUserFromUserDto(userClient.getUserById(comment.getAuthorId()));
        Event event = EventMapper.fromEventDtoToEvent(eventClient.getEventById(comment.getEventId()));
        return CommentMapper.fromCommentToCommentDto(comment, event, user);
    }

    private Pageable createPageable(Integer from, Integer size) {
        int pageNumber = from / size;
        return PageRequest.of(pageNumber, size);
    }
}