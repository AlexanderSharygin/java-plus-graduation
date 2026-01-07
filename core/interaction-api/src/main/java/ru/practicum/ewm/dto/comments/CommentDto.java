package ru.practicum.ewm.dto.comments;

import lombok.*;
import ru.practicum.ewm.dto.event.EventCommentDto;
import ru.practicum.ewm.dto.event.EventShortDto;
import ru.practicum.ewm.dto.user.UserShortDto;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentDto {
    private Long id;
    private EventCommentDto event;
    private UserShortDto author;
    private String text;
    private LocalDateTime createdAt;
}
