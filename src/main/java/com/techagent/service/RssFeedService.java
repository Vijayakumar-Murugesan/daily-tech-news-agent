package com.techagent.service;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.techagent.model.NewsItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * AGENT STEP 1: RSS Feed Fetcher
 *
 * Fetches tech news from public RSS feeds.
 * Each feed is tagged with a default category if the article's category
 * cannot be auto-detected later.
 */
@Service
public class RssFeedService {

    private static final Logger log = LoggerFactory.getLogger(RssFeedService.class);

    @Value("${news.hours.lookback:24}")
    private int hoursLookback;

    /**
     * Public RSS feeds grouped by default category.
     * These are all free, no-auth, public RSS endpoints.
     */
    private static final List<FeedSource> FEED_SOURCES = List.of(
        // AI / Machine Learning
        new FeedSource("https://techcrunch.com/category/artificial-intelligence/feed/", "TechCrunch", "AI"),
        new FeedSource("https://feeds.feedburner.com/nvidiablog", "NVIDIA Blog", "AI"),
        new FeedSource("https://openai.com/blog/rss/", "OpenAI Blog", "AI"),

        // Cloud & Infrastructure
        new FeedSource("https://aws.amazon.com/blogs/aws/feed/", "AWS Blog", "Cloud"),
        new FeedSource("https://cloud.google.com/feeds/gcp-release-notes.xml", "Google Cloud", "Cloud"),

        // Cybersecurity
        new FeedSource("https://feeds.feedburner.com/TheHackersNews", "The Hacker News", "Cybersecurity"),
        new FeedSource("https://krebsonsecurity.com/feed/", "Krebs on Security", "Cybersecurity"),
        new FeedSource("https://www.darkreading.com/rss.xml", "Dark Reading", "Cybersecurity"),

        // Java / JVM
        new FeedSource("https://www.infoq.com/java/rss/", "InfoQ Java", "Java"),
        new FeedSource("https://feeds.feedburner.com/baeldung", "Baeldung", "Java"),

        // DevOps
        new FeedSource("https://www.infoq.com/devops/rss/", "InfoQ DevOps", "DevOps"),
        new FeedSource("https://thenewstack.io/feed/", "The New Stack", "DevOps"),

        // Databases
        new FeedSource("https://www.infoq.com/data-engineering/rss/", "InfoQ Data", "Database"),

        // Developer Tools & General Tech
        new FeedSource("https://stackoverflow.blog/feed/", "Stack Overflow Blog", "Developer Tools"),
        new FeedSource("https://github.blog/feed/", "GitHub Blog", "Developer Tools"),
        new FeedSource("https://feeds.arstechnica.com/arstechnica/technology-lab", "Ars Technica", "General"),
        new FeedSource("https://www.wired.com/feed/rss", "Wired", "General")
    );

    /**
     * Fetches all configured RSS feeds and returns raw news items.
     * Items are filtered to only include those published within the lookback window.
     */
    public List<NewsItem> fetchAllFeeds() {
        List<NewsItem> allItems = new ArrayList<>();
        LocalDateTime cutoff = LocalDateTime.now().minusHours(hoursLookback);

        log.info("Starting RSS fetch from {} feeds (last {} hours)...", FEED_SOURCES.size(), hoursLookback);

        for (FeedSource source : FEED_SOURCES) {
            try {
                List<NewsItem> items = fetchFeed(source, cutoff);
                allItems.addAll(items);
                log.info("  ✓ {} — {} items", source.sourceName(), items.size());
            } catch (Exception e) {
                log.warn("  ✗ {} — failed: {}", source.sourceName(), e.getMessage());
            }
        }

        log.info("Total items fetched across all feeds: {}", allItems.size());
        return allItems;
    }

    /**
     * Fetches a single RSS feed and parses its entries.
     */
    private List<NewsItem> fetchFeed(FeedSource source, LocalDateTime cutoff) throws Exception {
        List<NewsItem> items = new ArrayList<>();

        URL feedUrl = new URL(source.url());
        SyndFeedInput input = new SyndFeedInput();
        // 10 second timeout to avoid hanging
        var connection = feedUrl.openConnection();
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(10_000);
        connection.setRequestProperty("User-Agent", "DailyTechNewsAgent/1.0");

        SyndFeed feed = input.build(new XmlReader(connection.getInputStream()));

        for (SyndEntry entry : feed.getEntries()) {
            LocalDateTime publishedAt = getPublishDate(entry);

            // Skip items older than the lookback window
            if (publishedAt != null && publishedAt.isBefore(cutoff)) {
                continue;
            }

            // Clean up HTML tags from description
            String rawDesc = "";
            if (entry.getDescription() != null) {
                rawDesc = entry.getDescription().getValue();
                rawDesc = rawDesc.replaceAll("<[^>]+>", "").trim();
                if (rawDesc.length() > 500) {
                    rawDesc = rawDesc.substring(0, 500) + "...";
                }
            }

            NewsItem item = new NewsItem();
            item.setTitle(entry.getTitle() != null ? entry.getTitle().trim() : "No Title");
            item.setDescription(rawDesc);
            item.setLink(entry.getLink());
            item.setSource(source.sourceName());
            item.setCategory(source.defaultCategory());
            item.setPublishedAt(publishedAt != null ? publishedAt : LocalDateTime.now());

            items.add(item);
        }

        return items;
    }

    private LocalDateTime getPublishDate(SyndEntry entry) {
        Date date = entry.getPublishedDate() != null ? entry.getPublishedDate() : entry.getUpdatedDate();
        if (date == null) return null;
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    /**
     * Simple record to hold feed metadata.
     */
    private record FeedSource(String url, String sourceName, String defaultCategory) {}
}
