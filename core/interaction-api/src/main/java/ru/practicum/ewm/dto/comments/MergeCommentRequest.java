package ru.practicum.ewm.dto.comments;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergeCommentRequest {

    @NotNull
    private Long eventId;

    @NotBlank
    private String text;

    @Builder.Default
    LocalDateTime createdAt = LocalDateTime.now();
}