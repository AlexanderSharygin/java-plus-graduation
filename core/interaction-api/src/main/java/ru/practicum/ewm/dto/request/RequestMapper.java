package ru.practicum.ewm.dto.request;

import lombok.NoArgsConstructor;
import ru.practicum.ewm.model.request.ParticipationRequest;


@NoArgsConstructor
public class RequestMapper {

    public static RequestDto fromRequestTpRequestDto(ParticipationRequest participationrequest) {
        return new RequestDto(
                participationrequest.getCreatedOn(),
                participationrequest.getEvent().getId(),
                participationrequest.getId(),
                participationrequest.getRequester().getId(),
                participationrequest.getStatus());
    }
}