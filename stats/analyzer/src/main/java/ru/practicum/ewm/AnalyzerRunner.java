package ru.practicum.ewm;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.processor.EventSimilarityProcessor;
import ru.practicum.ewm.processor.UsersActionsProcessor;

@Component
@RequiredArgsConstructor
public class AnalyzerRunner implements CommandLineRunner {

    private final EventSimilarityProcessor eventSimilarityProcessor;
    private final UsersActionsProcessor usersActionsProcessor;

    @Override
    public void run(String... args) throws Exception {
        try {
            Thread hubEventsThread = new Thread(eventSimilarityProcessor);
            hubEventsThread.setName("EventSimilarityHandlerThread");
            hubEventsThread.start();
            usersActionsProcessor.run();
        } catch (Exception e) {
            throw new RuntimeException("Не удалось запустить обработчики", e);
        }
    }
}
