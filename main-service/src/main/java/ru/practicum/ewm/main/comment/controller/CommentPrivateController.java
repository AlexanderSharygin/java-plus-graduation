package ru.practicum.ewm.main.comment.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.main.comment.dto.CommentDto;
import ru.practicum.ewm.main.comment.dto.MergeCommentRequest;
import ru.practicum.ewm.main.comment.service.CommentService;

import java.util.Collection;

@RestController
@RequestMapping("/users/{userId}/comments")
@AllArgsConstructor
public class CommentPrivateController {
    private final CommentService commentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto createComment(@Valid @RequestBody MergeCommentRequest request, @PathVariable Long userId) {
        return commentService.createComment(request, userId);
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable Long commentId, @PathVariable Long userId) {
        commentService.deleteCommentByIdAndAuthor(commentId, userId);
    }

    @PatchMapping("/{commentId}")
    @ResponseStatus(HttpStatus.OK)
    public CommentDto updateComment(@Valid @RequestBody MergeCommentRequest request,
                                    @PathVariable Long userId,
                                    @PathVariable Long commentId) {
        return commentService.updateCommentByIdAndAuthorId(commentId, userId, request);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Collection<CommentDto> getAllCommentsByUser(@PathVariable Long userId,
                                                       @RequestParam(defaultValue = "0") Integer from,
                                                       @RequestParam(defaultValue = "10") Integer size) {
        return commentService.getAllCommentsByUser(userId, from, size);
    }

    @GetMapping("events/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public Collection<CommentDto> getAllCommentsByEvent(@PathVariable Long eventId,
                                                        @PathVariable Long userId,
                                                        @RequestParam(defaultValue = "0") Integer from,
                                                        @RequestParam(defaultValue = "10") Integer size) {
        return commentService.getAllCommentsByUserAndEvent(userId, eventId, from, size);
    }
}