package ru.practicum.ewm.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public enum KafkaTopic {
    ACTIONS("actions");

    private final String topicName;
}
