package ru.practicum.ewm.main.category.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.main.category.dto.EventCategoryDto;
import ru.practicum.ewm.main.category.service.EventCategoryService;


@RestController
@RequestMapping(path = "/admin/categories")
public class EventCategoryAdminController {
    private final EventCategoryService categoryService;

    @Autowired
    public EventCategoryAdminController(EventCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PatchMapping("/{catId}")
    public EventCategoryDto update(@RequestBody @Valid EventCategoryDto eventCategoryDto,
                                   @PathVariable Long catId) {
        return categoryService.update(catId, eventCategoryDto);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventCategoryDto create(@RequestBody @Valid EventCategoryDto eventCategoryDto) {
        return categoryService.create(eventCategoryDto);
    }

    @DeleteMapping("/{catId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long catId) {
        categoryService.delete(catId);
    }
}

