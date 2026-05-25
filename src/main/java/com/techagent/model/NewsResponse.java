package com.techagent.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * The full API response returned to the frontend.
 * Contains the ranked news items + metadata about this run.
 */
public class NewsResponse {

    private List<NewsItem> newsItems;
    private int totalFetched;
    private int totalAfterFilter;
    private LocalDateTime generatedAt;
    private String agentStatus;            // "SUCCESS", "PARTIAL", "FALLBACK_MODE"
    private boolean llmUsed;               // Was the LLM API actually called?
    private Map<String, Integer> categoryCounts;  // e.g. {AI: 3, Cloud: 2, ...}
    private String message;

    // ---- Constructors ----

    public NewsResponse() {
        this.generatedAt = LocalDateTime.now();
    }

    // ---- Getters & Setters ----

    public List<NewsItem> getNewsItems() { return newsItems; }
    public void setNewsItems(List<NewsItem> newsItems) { this.newsItems = newsItems; }

    public int getTotalFetched() { return totalFetched; }
    public void setTotalFetched(int totalFetched) { this.totalFetched = totalFetched; }

    public int getTotalAfterFilter() { return totalAfterFilter; }
    public void setTotalAfterFilter(int totalAfterFilter) { this.totalAfterFilter = totalAfterFilter; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

    public String getAgentStatus() { return agentStatus; }
    public void setAgentStatus(String agentStatus) { this.agentStatus = agentStatus; }

    public boolean isLlmUsed() { return llmUsed; }
    public void setLlmUsed(boolean llmUsed) { this.llmUsed = llmUsed; }

    public Map<String, Integer> getCategoryCounts() { return categoryCounts; }
    public void setCategoryCounts(Map<String, Integer> categoryCounts) {
        this.categoryCounts = categoryCounts;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
