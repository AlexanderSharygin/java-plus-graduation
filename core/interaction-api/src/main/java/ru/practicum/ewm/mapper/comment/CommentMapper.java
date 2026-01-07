package ru.practicum.ewm.mapper.comment;

import ru.practicum.ewm.dto.comments.CommentDto;
import ru.practicum.ewm.dto.comments.MergeCommentRequest;
import ru.practicum.ewm.dto.event.EventCommentDto;
import ru.practicum.ewm.mapper.event.EventCategoryMapper;
import ru.practicum.ewm.mapper.event.EventMapper;
import ru.practicum.ewm.mapper.user.UserMapper;
import ru.practicum.ewm.model.comments.Comment;
import ru.practicum.ewm.model.event.Event;
import ru.practicum.ewm.model.user.User;

public class CommentMapper {
    public static Comment fromCommentDtoToComment(MergeCommentRequest commentDto, Event event, User owner) {
        return new Comment(-1L, event.getId(), owner.getId(), commentDto.getText(), commentDto.getCreatedAt());
    }

    public static CommentDto fromCommentToCommentDto(Comment comment, Event event, User owner) {
        return new CommentDto(comment.getId(),
                EventMapper.fromEventToEventCommentDto(event, EventCategoryMapper.toCategoryDtoFromCategory(event.getCategory())),
                UserMapper.fromUserToUserShortDto(owner),
                comment.getText(),
                comment.getCreatedAt());
    }

    public static CommentDto fromCommentToCommentDto(Comment comment, EventCommentDto eventCommentDto, User owner) {
        return new CommentDto(comment.getId(),
                eventCommentDto,
                UserMapper.fromUserToUserShortDto(owner),
                comment.getText(),
                comment.getCreatedAt());
    }
}
