package ru.practicum.ewm.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import ru.practicum.ewm.dto.catergory.EventCategoryDto;
import ru.practicum.ewm.dto.user.UserShortDto;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class EventCommentDto {

    private long id;
    @NotBlank
    @Size(min = 20, max = 2000)
    private String annotation;
    private EventCategoryDto category;
    private LocalDateTime eventDate;
    @NotBlank
    @Size(min = 3, max = 120)
    private String title;
}