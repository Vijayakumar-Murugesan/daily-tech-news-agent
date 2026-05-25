package com.techagent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.techagent.model.NewsItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * AGENT STEP 3: LLM Summarizer (Multi-Provider Support)
 *
 * Supports multiple LLM providers:
 *   - Groq (RECOMMENDED: 500+ free requests/day, very fast)
 *   - Google Gemini 2.0 Flash
 *   - OpenAI GPT
 *
 * Sends each news item to the configured LLM for:
 *   1. A concise 2-sentence summary
 *   2. A "why it matters" insight for developers/tech professionals
 *
 * Falls back gracefully to the raw RSS description if:
 *   - No API key is configured
 *   - The API call fails or times out
 *   - The response is malformed
 *
 * =========================================================
 *  GROQ - RECOMMENDED (FREE tier 500+/day)
 *  Get your FREE key: https://console.groq.com
 * =========================================================
 */
@Service
public class LlmSummarizerService {

    private static final Logger log = LoggerFactory.getLogger(LlmSummarizerService.class);

    @Value("${llm.provider:groq}")
    private String provider;

    @Value("${llm.api.key:}")
    private String apiKey;

    @Value("${llm.model:llama-3.1-8b-instant}")
    private String model;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build();

    /**
     * Enriches all news items with AI-generated summaries.
     * Returns true if LLM was actually used, false if fallback was applied.
     */
    public boolean summarizeAll(List<NewsItem> items) {
        if (!isApiKeyConfigured()) {
            log.warn("No LLM API key configured — using fallback summaries.");
            log.warn("Get a FREE Groq key at: https://console.groq.com");
            applyFallbackToAll(items);
            return false;
        }

        log.info("LLM API key found ({}) — using AI summaries", provider);
        boolean anyLlmUsed = false;

        for (NewsItem item : items) {
            try {
                summarizeWithLLM(item);
                anyLlmUsed = true;
                Thread.sleep(300); // rate limiting
            } catch (Exception e) {
                log.warn("LLM call failed for '{}': {} — using fallback",
                    item.getTitle(), e.getMessage());
                applyFallback(item);
            }
        }

        return anyLlmUsed;
    }

    /**
     * Calls the configured LLM API (Groq, Gemini, or OpenAI).
     */
    private void summarizeWithLLM(NewsItem item) throws Exception {
        String prompt = buildPrompt(item);

        if ("groq".equalsIgnoreCase(provider)) {
            summarizeWithGroq(item, prompt);
        } else if ("gemini".equalsIgnoreCase(provider)) {
            summarizeWithGemini(item, prompt);
        } else if ("openai".equalsIgnoreCase(provider)) {
            summarizeWithOpenAI(item, prompt);
        } else {
            throw new RuntimeException("Unknown LLM provider: " + provider);
        }
    }

    /**
     * Calls the Groq API (RECOMMENDED - fast & free).
     */
    private void summarizeWithGroq(NewsItem item, String prompt) throws Exception {
        // ── Build Groq request body ──────────────────────────────────────
        ObjectNode messageContent = objectMapper.createObjectNode()
            .put("role", "user")
            .put("content", prompt);

        String requestBody = objectMapper.writeValueAsString(
            objectMapper.createObjectNode()
                .put("model", model)
                .put("max_tokens", 300)
                .put("temperature", 0.3)
                .set("messages", objectMapper.createArrayNode().add(messageContent))
        );

        // ── URL & Request ───────────────────────────────────────────
        String url = "https://api.groq.com/openai/v1/chat/completions";

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            String body = response.body();
            log.error("[DEBUG] Groq Error Response: {}", body);
            log.error("[DEBUG] Groq Request URL: {}", url);
            throw new RuntimeException("Groq API HTTP " + response.statusCode());
        }

        // ── Parse Groq response ──────────────────────────────────────────
        // Groq format: choices[0].message.content
        JsonNode responseJson = objectMapper.readTree(response.body());
        String content = responseJson
            .path("choices").get(0)
            .path("message")
            .path("content").asText();

        parseLlmResponse(content, item);
        log.info("  ✓ Groq summarized: '{}'",
            item.getTitle().substring(0, Math.min(60, item.getTitle().length())));
    }

    /**
     * Calls the Gemini 2.0 Flash REST API.
     */
    private void summarizeWithGemini(NewsItem item, String prompt) throws Exception {
        // ── Build Gemini request body ──────────────────────────────────────
        String requestBody = objectMapper.writeValueAsString(
            objectMapper.createObjectNode()
                .set("contents", objectMapper.createArrayNode()
                    .add(objectMapper.createObjectNode()
                        .set("parts", objectMapper.createArrayNode()
                            .add(objectMapper.createObjectNode()
                                .put("text", prompt)))))
        );

        // ── URL: model + API key as query param ───────────────────────────
        String url = "https://generativelanguage.googleapis.com/v1/"
            + model + ":generateContent?key=" + apiKey;

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            String body = response.body();
            log.error("[DEBUG] Gemini Error Response: {}", body);
            throw new RuntimeException("Gemini API HTTP " + response.statusCode());
        }

        // ── Parse Gemini response ──────────────────────────────────────────
        JsonNode responseJson = objectMapper.readTree(response.body());
        String content = responseJson
            .path("candidates").get(0)
            .path("content")
            .path("parts").get(0)
            .path("text").asText();

        parseLlmResponse(content, item);
        log.info("  ✓ Gemini summarized: '{}'",
            item.getTitle().substring(0, Math.min(60, item.getTitle().length())));
    }

    /**
     * Calls the OpenAI API.
     */
    private void summarizeWithOpenAI(NewsItem item, String prompt) throws Exception {
        String requestBody = objectMapper.writeValueAsString(
            objectMapper.createObjectNode()
                .put("model", model)
                .put("temperature", 0.3)
                .put("max_tokens", 300)
                .set("messages", objectMapper.createArrayNode()
                    .add(objectMapper.createObjectNode()
                        .put("role", "user")
                        .put("content", prompt)))
        );

        String url = "https://api.openai.com/v1/chat/completions";

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            String body = response.body();
            log.error("[DEBUG] OpenAI Error Response: {}", body);
            throw new RuntimeException("OpenAI API HTTP " + response.statusCode());
        }

        JsonNode responseJson = objectMapper.readTree(response.body());
        String content = responseJson
            .path("choices").get(0)
            .path("message")
            .path("content").asText();

        parseLlmResponse(content, item);
        log.info("  ✓ OpenAI summarized: '{}'",
            item.getTitle().substring(0, Math.min(60, item.getTitle().length())));
    }

    /**
     * The prompt engineering heart of the agent.
     *
     * KEY PRINCIPLES DEMONSTRATED HERE:
     *   1. Role assignment  — "You are an expert tech journalist"
     *   2. Structured input — inject title, source, category, description
     *   3. Output format    — strict JSON so we can parse reliably
     *   4. Constraints      — length, tone, audience
     */
    private String buildPrompt(NewsItem item) {
        return """
            You are an expert tech journalist writing for senior software engineers and architects.

            Analyze this tech news article and respond ONLY with a valid JSON object.
            No markdown, no code blocks, no extra text — pure JSON only.

            Article Title: %s
            Article Source: %s
            Article Category: %s
            Article Description: %s

            Respond with this exact JSON structure:
            {
              "summary": "2-sentence factual summary of what happened or was announced",
              "whyItMatters": "1-2 sentences explaining the practical impact for developers or tech professionals"
            }

            Rules:
            - summary: be factual and specific, mention key names/numbers if present
            - whyItMatters: focus on real-world developer/architect impact
            - Keep each field under 150 characters
            - Do NOT use jargon or hype words
            - Do NOT start with "This article..." or "The article..."
            """.formatted(
                item.getTitle(),
                item.getSource(),
                item.getCategory(),
                item.getDescription() == null || item.getDescription().isEmpty()
                    ? "No description available" : item.getDescription()
            );
    }

    /**
     * Parse the JSON response from Gemini and populate the NewsItem.
     */
    private void parseLlmResponse(String content, NewsItem item) {
        try {
            // Strip any accidental markdown fences Gemini might add
            String cleaned = content.trim()
                .replaceAll("(?s)^```json\\s*", "")
                .replaceAll("(?s)^```\\s*", "")
                .replaceAll("(?s)```\\s*$", "")
                .trim();

            JsonNode json = objectMapper.readTree(cleaned);
            String summary = json.path("summary").asText();
            String whyItMatters = json.path("whyItMatters").asText();

            item.setSummary(!summary.isEmpty() ? summary : item.getDescription());
            item.setWhyItMatters(!whyItMatters.isEmpty()
                ? whyItMatters
                : "Stay updated with the latest in " + item.getCategory() + ".");

        } catch (Exception e) {
            log.warn("Failed to parse Gemini JSON — fallback. Raw: {}",
                content.substring(0, Math.min(120, content.length())));
            applyFallback(item);
        }
    }

    private void applyFallback(NewsItem item) {
        String desc = item.getDescription();
        if (desc == null || desc.isBlank()) desc = "No description available from source.";
        item.setSummary(desc);
        item.setWhyItMatters("Visit the original article for full details. Category: " + item.getCategory());
    }

    private void applyFallbackToAll(List<NewsItem> items) {
        items.forEach(this::applyFallback);
    }

    private boolean isApiKeyConfigured() {
        return apiKey != null
            && !apiKey.isBlank()
            && !apiKey.equals("your-groq-api-key-here")
            && !apiKey.equals("your-gemini-api-key-here")
            && !apiKey.equals("sk-your-openai-key-here");
    }
}
