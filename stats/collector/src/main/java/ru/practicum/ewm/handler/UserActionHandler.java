package ru.practicum.ewm.handler;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.config.KafkaConfig;
import ru.practicum.ewm.config.KafkaTopic;
import ru.practicum.ewm.grpc.stats.action.ActionTypeProto;
import ru.practicum.ewm.grpc.stats.action.UserActionProto;
import ru.practicum.ewm.producer.KafkaEventProducer;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UserActionHandler {

    protected final KafkaEventProducer producer;
    private final KafkaConfig kafkaConfig;

    public void collectUserAction(UserActionProto request) {
        String topic = kafkaConfig.getTopic(KafkaTopic.ACTIONS.getTopicName());
        UserActionAvro userActionAvro = mapProtoToUserActionAvro(request);
        ProducerRecord<String, UserActionAvro> userActionRecord = new ProducerRecord<>(topic, userActionAvro);
        producer.send(userActionAvro, topic);
    }

    public static UserActionAvro mapProtoToUserActionAvro(UserActionProto userActionProto) {
        UserActionAvro userActionAvro = new UserActionAvro();
        userActionAvro.setUserId(userActionProto.getUserId());
        userActionAvro.setEventId(userActionProto.getEventId());
        Instant instant = Instant.ofEpochSecond(userActionProto.getTimestamp().getSeconds(), userActionProto.getTimestamp().getNanos());
        userActionAvro.setTimestamp(instant);
        userActionAvro.setActionType(mapProtoToActionTypeAvro(userActionProto.getActionType()));
        return userActionAvro;
    }

    public static ActionTypeAvro mapProtoToActionTypeAvro(ActionTypeProto typeProto) {
        return switch (typeProto) {
            case ActionTypeProto.ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ActionTypeProto.ACTION_LIKE -> ActionTypeAvro.LIKE;
            case ActionTypeProto.ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            default -> null;
        };
    }
}
