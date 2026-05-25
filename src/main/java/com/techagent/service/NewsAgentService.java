package com.techagent.service;

import com.techagent.model.NewsItem;
import com.techagent.model.NewsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AGENT ORCHESTRATOR
 *
 * This is the brain of the AI Agent — it coordinates all steps
 * in a clear, readable pipeline:
 *
 *   Step 1: Fetch RSS feeds        → RssFeedService
 *   Step 2: Process & rank         → NewsProcessorService
 *   Step 3: AI summarization       → LlmSummarizerService
 *   Step 4: Package response       → NewsResponse
 *
 * ===================================================
 *  TEACHING POINT: An "AI Agent" is just:
 *    - A goal (get me today's top tech news)
 *    - Tools (RSS fetcher, LLM API)
 *    - Logic (filter, rank, summarize)
 *    - Output (structured response)
 * ===================================================
 */
@Service
public class NewsAgentService {

    private static final Logger log = LoggerFactory.getLogger(NewsAgentService.class);

    private final RssFeedService rssFeedService;
    private final NewsProcessorService newsProcessorService;
    private final LlmSummarizerService llmSummarizerService;

    public NewsAgentService(RssFeedService rssFeedService,
                            NewsProcessorService newsProcessorService,
                            LlmSummarizerService llmSummarizerService) {
        this.rssFeedService = rssFeedService;
        this.newsProcessorService = newsProcessorService;
        this.llmSummarizerService = llmSummarizerService;
    }

    /**
     * Run the full agent pipeline and return a structured response.
     */
    public NewsResponse runAgent() {
        long startTime = System.currentTimeMillis();
        log.info("=== AI Agent Started: Daily Tech News Pipeline ===");

        NewsResponse response = new NewsResponse();

        try {
            // ── STEP 1: Fetch raw news from RSS feeds ──────────────────────
            log.info("[STEP 1] Fetching RSS feeds...");
            List<NewsItem> rawItems = rssFeedService.fetchAllFeeds();
            response.setTotalFetched(rawItems.size());

            if (rawItems.isEmpty()) {
                response.setAgentStatus("NO_DATA");
                response.setMessage("No news articles found. RSS feeds may be unavailable.");
                response.setNewsItems(List.of());
                return response;
            }

            // ── STEP 2: Deduplicate, categorize, score, rank ────────────────
            log.info("[STEP 2] Processing {} items: dedup → categorize → rank...", rawItems.size());
            List<NewsItem> topItems = newsProcessorService.process(rawItems);
            response.setTotalAfterFilter(topItems.size());
            response.setCategoryCounts(newsProcessorService.getCategoryCounts(topItems));

            // ── STEP 3: AI Summarization ────────────────────────────────────
            log.info("[STEP 3] Summarizing {} items with LLM...", topItems.size());
            boolean llmUsed = llmSummarizerService.summarizeAll(topItems);
            response.setLlmUsed(llmUsed);

            // ── STEP 4: Package the final response ──────────────────────────
            response.setNewsItems(topItems);
            response.setAgentStatus(llmUsed ? "SUCCESS" : "FALLBACK_MODE");
            response.setMessage(buildStatusMessage(llmUsed, rawItems.size(), topItems.size()));

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("=== AI Agent Complete in {}ms — {} items, LLM={} ===",
                elapsed, topItems.size(), llmUsed);

        } catch (Exception e) {
            log.error("Agent pipeline failed: {}", e.getMessage(), e);
            response.setAgentStatus("ERROR");
            response.setMessage("Agent encountered an error: " + e.getMessage());
            response.setNewsItems(List.of());
        }

        return response;
    }

    private String buildStatusMessage(boolean llmUsed, int fetched, int top) {
        String mode = llmUsed ? "AI-powered summaries" : "RSS fallback summaries (no API key)";
        return String.format(
            "Scanned %d articles → selected top %d → generated with %s",
            fetched, top, mode
        );
    }
}
