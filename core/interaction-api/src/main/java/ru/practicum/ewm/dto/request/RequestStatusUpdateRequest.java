package ru.practicum.ewm.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import ru.practicum.ewm.model.request.RequestStatus;

import java.util.List;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class RequestStatusUpdateRequest {
    private List<Long> requestIds;
    private RequestStatus status;
}
