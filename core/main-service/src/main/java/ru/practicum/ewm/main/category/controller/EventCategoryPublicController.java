package ru.practicum.ewm.main.category.controller;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.main.category.dto.EventCategoryDto;
import ru.practicum.ewm.main.category.service.EventCategoryService;

import java.util.List;

@RestController
@RequestMapping(path = "/categories")
public class EventCategoryPublicController {

    private final EventCategoryService categoryService;

    @Autowired
    public EventCategoryPublicController(EventCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<EventCategoryDto> getCategories(@PositiveOrZero @RequestParam(value = "from", defaultValue = "0") int from,
                                                @Positive @RequestParam(value = "size", defaultValue = "10") int size) {
        return categoryService.getAll(PageRequest.of(from, size));
    }

    @GetMapping("/{catId}")
    public EventCategoryDto getCategory(@PathVariable long catId) {
        return categoryService.getById(catId);
    }
}