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
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.exception.PublicationException;
import ru.practicum.ewm.feign_clients.EventClient;
import ru.practicum.ewm.feign_clients.UserClient;
import ru.practicum.ewm.mapper.CommentMapper;
import ru.practicum.ewm.mapper.event.EventMapper;
import ru.practicum.ewm.mapper.user.UserMapper;
import ru.practicum.ewm.model.comments.Comment;
import ru.practicum.ewm.model.event.Event;
import ru.practicum.ewm.model.event.EventState;
import ru.practicum.ewm.model.user.User;
import ru.practicum.ewm.repository.CommentRepository;

import java.util.Collection;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final UserClient userClient;
    private final EventClient eventClient;

    public CommentDto createComment(MergeCommentRequest mergeCommentRequest, Long userId) {
        User user = UserMapper.toUserFromUserDto(userClient.getUserById(userId));
        Event event = EventMapper.fromEventDtoToEvent(eventClient.getEventById(mergeCommentRequest.getEventId()));
        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new PublicationException("Event must be published");
        }
        Comment comment = commentMapper.requestToComment(mergeCommentRequest, event, user);
        CommentDto response = commentMapper.commentToResponse(commentRepository.save(comment));
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
        if (!oldComment.getEvent().getId().equals(request.getEventId())) {
            throw new DataIntegrityViolationException("Event Id not correct");
        }
        commentMapper.updateComment(request, EventMapper.fromEventDtoToEvent(eventClient.getEventById(request.getEventId())),
                oldComment);
        CommentDto response = commentMapper.commentToResponse(commentRepository.save(oldComment));
        log.info("Comment id={} was updated by user id={}", response.getId(), response.getAuthor().getId());

        return response;
    }

    public Collection<CommentDto> getAllCommentsByUser(Long userId, Integer from, Integer size) {
        log.info("Get all comments for user id={}", userId);

        return commentRepository.findAllByAuthorIdOrderByCreatedAtDesc(userId, createPageable(from, size))
                .stream()
                .map(commentMapper::commentToResponse)
                .toList();
    }

    public Collection<CommentDto> getAllCommentsByEvent(Long eventId, Integer from, Integer size) {
        log.info("Get all comments for event id={}", eventId);

        return commentRepository.findAllByEventIdOrderByCreatedAtDesc(eventId, createPageable(from, size))
                .stream()
                .map(commentMapper::commentToResponse)
                .toList();
    }

    public Collection<CommentDto> getAllCommentsByUserAndEvent(Long userId, Long eventId, Integer from, Integer size) {
        log.info("Get all comments for event id={} and user id={}", eventId, userId);

        return commentRepository.findAllByAuthorIdAndEventIdOrderByCreatedAtDesc(userId, eventId, createPageable(from, size))
                .stream()
                .map(commentMapper::commentToResponse)
                .toList();
    }

    public CommentDto getCommentById(Long commentId) {
        log.info("Get comment with id={}", commentId);
        return commentMapper.commentToResponse(commentRepository.findById(commentId).orElseThrow(() ->
                new NotFoundException(String.format("Comment with id=%d was not found", commentId))));
    }

    private Pageable createPageable(Integer from, Integer size) {
        int pageNumber = from / size;
        return PageRequest.of(pageNumber, size);
    }
}