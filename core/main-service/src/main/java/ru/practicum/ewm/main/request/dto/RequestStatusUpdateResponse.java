package ru.practicum.ewm.main.request.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class RequestStatusUpdateResponse {
    private Set<RequestDto> confirmedRequests;
    private Set<RequestDto> rejectedRequests;
}
