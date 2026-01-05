package ru.practicum.ewm.compilation.mapper;

import lombok.NoArgsConstructor;
import ru.practicum.ewm.dto.compilation.CompilationDto;
import ru.practicum.ewm.dto.compilation.CompilationRequestDto;
import ru.practicum.ewm.dto.event.EventShortDto;
import ru.practicum.ewm.model.compilation.Compilation;
import ru.practicum.ewm.model.event.Event;

import java.util.Set;

@NoArgsConstructor
public class CompilationMapper {

    public static CompilationDto toDtoFromCompilation(Compilation compilation, Set<EventShortDto> eventDto) {

        return new CompilationDto(eventDto, compilation.getId(), compilation.getPinned(), compilation.getTitle());
    }

    public static Compilation toCompilationFromDto(CompilationRequestDto compilationDto, Set<Event> eventsList) {

        return new Compilation(null, eventsList, compilationDto.getPinned(), compilationDto.getTitle());
    }
}
