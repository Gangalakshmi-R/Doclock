package com.doclock.backend.controller;

import com.doclock.backend.service.SemanticSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SemanticSearchController {

    private final SemanticSearchService semanticSearchService;


    @GetMapping
    public List<Map<String, Object>> search(
            @RequestParam String question,
            @RequestParam(defaultValue = "3")
            int limit) {

        return semanticSearchService.search(
                question,
                limit
        );
    }
}