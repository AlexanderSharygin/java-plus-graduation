package ru.practicum.ewm.dto.request;

import lombok.NoArgsConstructor;
import ru.practicum.ewm.model.request.ParticipationRequest;


@NoArgsConstructor
public class RequestMapper {

    public static RequestDto fromRequestTpRequestDto(ParticipationRequest participationrequest) {
        return new RequestDto(
                participationrequest.getCreatedOn(),
                participationrequest.getEventId(),
                participationrequest.getId(),
                participationrequest.getRequesterId(),
                participationrequest.getStatus());
    }
}