package com.techagent.service;

import com.techagent.model.NewsItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AGENT STEP 2: News Processor
 *
 * Handles:
 *   - Deduplication (by title similarity)
 *   - Smart category detection (keyword-based)
 *   - Relevance scoring & ranking
 *   - Selecting Top N items
 */
@Service
public class NewsProcessorService {

    private static final Logger log = LoggerFactory.getLogger(NewsProcessorService.class);

    @Value("${news.top.count:10}")
    private int topCount;

    // ---- Category keyword maps ----
    // Each category has keywords. A title/description match upgrades the category
    // and boosts the relevance score.

    private static final Map<String, List<String>> CATEGORY_KEYWORDS = new LinkedHashMap<>() {{
        put("AI", List.of(
            "ai ", "artificial intelligence", "machine learning", "llm", "gpt", "claude",
            "gemini", "openai", "neural network", "deep learning", "generative", "chatgpt",
            "copilot", "transformer", "model", "hugging face", "anthropic", "mistral",
            "langchain", "rag ", "vector", "embedding", "fine-tun", "agent"
        ));
        put("Cybersecurity", List.of(
            "hack", "vulnerabilit", "breach", "malware", "ransomware", "phishing",
            "exploit", "cve-", "zero-day", "security flaw", "data leak", "cyber",
            "threat", "patch tuesday", "attack", "stolen", "bypass", "backdoor"
        ));
        put("Cloud", List.of(
            "aws", "azure", "google cloud", "gcp", "kubernetes", "k8s", "serverless",
            "lambda", "ec2", "s3 ", "cloud native", "container", "docker", "terraform",
            "infrastructure as code", "microservice", "service mesh", "multi-cloud"
        ));
        put("Java", List.of(
            "java ", "jvm", "spring boot", "spring framework", "quarkus", "micronaut",
            "jakarta ee", "jdk ", "openjdk", "graalvm", "kotlin", "scala", "maven",
            "gradle", "jpa", "hibernate", "virtual threads", "project loom", "java 21",
            "java 22", "java 23", "java 24", "java 17"
        ));
        put("DevOps", List.of(
            "devops", "ci/cd", "pipeline", "jenkins", "github actions", "gitlab",
            "devsecops", "observability", "monitoring", "prometheus", "grafana",
            "deployment", "release", "platform engineering", "sre ", "site reliability",
            "incident", "opentelemetry", "gitops", "argocd", "helm"
        ));
        put("Database", List.of(
            "database", "postgresql", "mysql", "mongodb", "redis", "elasticsearch",
            "cassandra", "sql ", "nosql", "vector database", "data warehouse",
            "snowflake", "databricks", "dbt ", "etl ", "data lake", "pinecone",
            "chromadb", "sqlite", "cockroachdb"
        ));
        put("Developer Tools", List.of(
            "github", "vscode", "jetbrains", "ide ", "cli ", "sdk ", "api ",
            "open source", "developer", "programming", "code review", "debugging",
            "testing", "playwright", "selenium", "postman", "swagger", "openapi",
            "npm ", "package manager", "cursor ", "copilot"
        ));
    }};

    // High-signal words that boost a story's relevance score
    private static final List<String> HIGH_IMPORTANCE_SIGNALS = List.of(
        "critical", "major", "breach", "vulnerability", "launch", "release",
        "announces", "acqui", "funding", "open source", "record", "billion",
        "breaking", "urgent", "emergency", "milestone", "revolutionary"
    );

    /**
     * Full processing pipeline: dedup → categorize → score → rank → top-N
     */
    public List<NewsItem> process(List<NewsItem> rawItems) {
        log.info("Processing {} raw items...", rawItems.size());

        List<NewsItem> deduped = deduplicate(rawItems);
        log.info("  After deduplication: {}", deduped.size());

        List<NewsItem> categorized = detectCategories(deduped);
        List<NewsItem> scored = scoreAndRank(categorized);

        List<NewsItem> top = scored.stream()
            .limit(topCount)
            .collect(Collectors.toList());

        log.info("  Final top-{}: {}", topCount, top.size());
        return top;
    }

    /**
     * Remove duplicate or near-duplicate articles based on title similarity.
     * Two titles are "duplicates" if they share 60%+ of their words.
     */
    private List<NewsItem> deduplicate(List<NewsItem> items) {
        List<NewsItem> unique = new ArrayList<>();

        for (NewsItem candidate : items) {
            boolean isDuplicate = false;
            String candidateWords = normalizeTitle(candidate.getTitle());

            for (NewsItem existing : unique) {
                String existingWords = normalizeTitle(existing.getTitle());
                if (titleSimilarity(candidateWords, existingWords) >= 0.60) {
                    isDuplicate = true;
                    break;
                }
            }

            if (!isDuplicate) {
                unique.add(candidate);
            }
        }

        return unique;
    }

    /**
     * Override the default RSS category with smarter keyword detection.
     * An article's title + description are scanned against all category keyword lists.
     * The category with the most keyword matches wins.
     */
    private List<NewsItem> detectCategories(List<NewsItem> items) {
        for (NewsItem item : items) {
            String text = (item.getTitle() + " " + item.getDescription()).toLowerCase();

            String bestCategory = item.getCategory(); // keep default as fallback
            int bestScore = 0;

            for (Map.Entry<String, List<String>> entry : CATEGORY_KEYWORDS.entrySet()) {
                int score = 0;
                for (String keyword : entry.getValue()) {
                    if (text.contains(keyword.toLowerCase())) {
                        score++;
                    }
                }
                if (score > bestScore) {
                    bestScore = score;
                    bestCategory = entry.getKey();
                }
            }

            item.setCategory(bestCategory);
        }

        return items;
    }

    /**
     * Score each item 0-100 based on:
     *   - High-importance signal words in title (+10 each, max 30)
     *   - Category keyword density (+5 per match, max 20)
     *   - Recency bonus: newer = higher score (up to 30 pts)
     *   - Source authority bonus (up to 20 pts)
     *
     * Then sort descending by score, then ensure category diversity.
     */
    private List<NewsItem> scoreAndRank(List<NewsItem> items) {
        for (NewsItem item : items) {
            int score = 50; // base score
            String titleLower = item.getTitle().toLowerCase();
            String fullText = (titleLower + " " + item.getDescription()).toLowerCase();

            // 1. High-importance signals in title
            int signalBonus = 0;
            for (String signal : HIGH_IMPORTANCE_SIGNALS) {
                if (titleLower.contains(signal)) {
                    signalBonus += 10;
                }
            }
            score += Math.min(signalBonus, 30);

            // 2. Category keyword density
            List<String> keywords = CATEGORY_KEYWORDS.getOrDefault(item.getCategory(), List.of());
            int kwBonus = 0;
            for (String kw : keywords) {
                if (fullText.contains(kw)) kwBonus += 5;
            }
            score += Math.min(kwBonus, 20);

            // 3. Recency: items from last 6 hours get +20, 6-12h get +10, else 0
            if (item.getPublishedAt() != null) {
                long minutesOld = java.time.Duration
                    .between(item.getPublishedAt(), java.time.LocalDateTime.now())
                    .toMinutes();
                if (minutesOld <= 360) score += 20;
                else if (minutesOld <= 720) score += 10;
            }

            // 4. Trusted source bonus
            score += getSourceBonus(item.getSource());

            item.setRelevanceScore(Math.min(score, 100));
        }

        // Sort by score descending
        items.sort(Comparator.comparingInt(NewsItem::getRelevanceScore).reversed());

        // Ensure diversity: at most 2 items per category in top results
        return ensureCategoryDiversity(items);
    }

    /**
     * Reorder results to ensure no single category dominates.
     * Max 2 items per category are taken first, then remaining slots filled.
     */
    private List<NewsItem> ensureCategoryDiversity(List<NewsItem> ranked) {
        Map<String, Integer> categoryCount = new HashMap<>();
        List<NewsItem> diverse = new ArrayList<>();
        List<NewsItem> overflow = new ArrayList<>();

        for (NewsItem item : ranked) {
            int count = categoryCount.getOrDefault(item.getCategory(), 0);
            if (count < 2) {
                diverse.add(item);
                categoryCount.put(item.getCategory(), count + 1);
            } else {
                overflow.add(item);
            }
        }

        // Fill remaining slots from overflow (still sorted by score)
        diverse.addAll(overflow);
        return diverse;
    }

    private int getSourceBonus(String source) {
        if (source == null) return 0;
        return switch (source) {
            case "The Hacker News", "Krebs on Security" -> 15;
            case "TechCrunch", "AWS Blog", "GitHub Blog", "OpenAI Blog" -> 12;
            case "InfoQ Java", "InfoQ DevOps", "Baeldung" -> 10;
            case "Stack Overflow Blog", "The New Stack" -> 8;
            default -> 5;
        };
    }

    private String normalizeTitle(String title) {
        return title.toLowerCase()
            .replaceAll("[^a-z0-9 ]", "")
            .trim();
    }

    private double titleSimilarity(String a, String b) {
        Set<String> wordsA = new HashSet<>(Arrays.asList(a.split("\\s+")));
        Set<String> wordsB = new HashSet<>(Arrays.asList(b.split("\\s+")));
        // Remove noise words
        Set<String> noise = Set.of("the", "a", "an", "in", "on", "at", "to", "for",
                                   "of", "and", "or", "is", "are", "was", "were",
                                   "with", "from", "how", "what", "why");
        wordsA.removeAll(noise);
        wordsB.removeAll(noise);

        if (wordsA.isEmpty() || wordsB.isEmpty()) return 0.0;

        Set<String> intersection = new HashSet<>(wordsA);
        intersection.retainAll(wordsB);

        Set<String> union = new HashSet<>(wordsA);
        union.addAll(wordsB);

        return (double) intersection.size() / union.size(); // Jaccard similarity
    }

    public int getTopCount() {
        return topCount;
    }

    /**
     * Count items per category for the response metadata.
     */
    public Map<String, Integer> getCategoryCounts(List<NewsItem> items) {
        Map<String, Integer> counts = new TreeMap<>();
        for (NewsItem item : items) {
            counts.merge(item.getCategory(), 1, Integer::sum);
        }
        return counts;
    }
}
