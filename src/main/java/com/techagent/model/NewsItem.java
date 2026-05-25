package com.techagent.model;

import java.time.LocalDateTime;

/**
 * Represents a single tech news item fetched from RSS feeds.
 * This is the core data model that flows through the entire agent pipeline.
 */
public class NewsItem {

    private String title;
    private String description;       // Raw RSS description (fallback summary)
    private String link;
    private String source;            // e.g., "TechCrunch", "The Hacker News"
    private String category;          // e.g., "AI", "Cloud", "Cybersecurity"
    private LocalDateTime publishedAt;
    private String summary;           // AI-generated summary
    private String whyItMatters;      // AI-generated "why it matters" insight
    private int relevanceScore;       // Used for ranking (0-100)

    // ---- Constructors ----

    public NewsItem() {}

    public NewsItem(String title, String description, String link,
                    String source, LocalDateTime publishedAt) {
        this.title = title;
        this.description = description;
        this.link = link;
        this.source = source;
        this.publishedAt = publishedAt;
    }

    // ---- Getters & Setters ----

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getWhyItMatters() { return whyItMatters; }
    public void setWhyItMatters(String whyItMatters) { this.whyItMatters = whyItMatters; }

    public int getRelevanceScore() { return relevanceScore; }
    public void setRelevanceScore(int relevanceScore) { this.relevanceScore = relevanceScore; }

    @Override
    public String toString() {
        return "NewsItem{title='" + title + "', category='" + category +
               "', source='" + source + "', score=" + relevanceScore + "}";
    }
}
