package com.techagent.controller;

import com.techagent.model.NewsResponse;
import com.techagent.service.NewsAgentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API Controller
 *
 * Endpoints:
 *   GET  /api/news/generate  — Run the full agent pipeline
 *   GET  /api/health         — Simple health check
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // allow frontend to call from any origin
public class NewsController {

    private static final Logger log = LoggerFactory.getLogger(NewsController.class);

    private final NewsAgentService newsAgentService;

    public NewsController(NewsAgentService newsAgentService) {
        this.newsAgentService = newsAgentService;
    }

    /**
     * Main endpoint — triggers the full AI Agent pipeline.
     * Called by the "Generate Daily Tech News" button in the UI.
     */
    @GetMapping("/news/generate")
    public ResponseEntity<NewsResponse> generateNews() {
        log.info("POST /api/news/generate — starting agent pipeline");
        NewsResponse response = newsAgentService.runAgent();
        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "Daily Top Tech News AI Agent",
            "version", "1.0.0"
        ));
    }
}
