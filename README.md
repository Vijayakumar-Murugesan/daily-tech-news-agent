# 🤖 Daily Top Tech News AI Agent

> A Java 21 + Spring Boot AI Agent that autonomously fetches, categorizes, ranks,
> and summarizes today's top tech news using RSS feeds + Multiple LLM providers
> (Groq, Google Gemini, or OpenAI).

---

## 📁 Project Structure

```
daily-tech-news-agent/
├── pom.xml
└── src/
    └── main/
        ├── java/com/techagent/
        │   ├── DailyTechNewsAgentApplication.java   ← Spring Boot entry point
        │   ├── controller/
        │   │   └── NewsController.java               ← REST API (POST /api/news/generate)
        │   ├── service/
        │   │   ├── NewsAgentService.java             ← 🧠 ORCHESTRATOR (the "agent brain")
        │   │   ├── RssFeedService.java               ← Step 1: Fetch 17 RSS feeds
        │   │   ├── NewsProcessorService.java         ← Step 2: Dedup + Categorize + Rank
        │   │   └── LlmSummarizerService.java         ← Step 3: Call LLM API (multi-provider + fallback)
        │   └── model/
        │       ├── NewsItem.java                     ← Core data model
        │       └── NewsResponse.java                 ← API response envelope
        └── resources/
            ├── application.properties
            └── static/
                └── index.html                        ← Single-page UI (HTML + CSS + JS)
```

---

## 🧠 How the AI Agent Works (4-Step Pipeline)

```
[USER CLICKS BUTTON]
        │
        ▼
[STEP 1] RssFeedService
  • Fetches 17 public RSS feeds (async)
  • Filters: only last 24 hours
  • Parses titles, descriptions, links, dates
        │
        ▼
[STEP 2] NewsProcessorService
  • Deduplication (Jaccard title similarity ≥ 60%)
  • Category detection (keyword matching: AI, Cloud, Cybersecurity, Java, DevOps, DB, Tools)
  • Relevance scoring (0-100: signals + recency + source authority)
  • Category diversity enforcement (max 2 per category)
  • Returns Top 10 ranked items
        │
        ▼
[STEP 3] LlmSummarizerService (Multi-Provider)
  • Provider options: Groq (recommended), Gemini, or OpenAI
  • Sends each item to configured LLM
  • Prompt: role + task + JSON output format
  • Parses: { summary, whyItMatters }
  • Graceful fallback: uses raw RSS description if API unavailable
        │
        ▼
[STEP 4] REST Response → Frontend UI
  • NewsResponse with all items + metadata + LLM usage flag
  • UI renders ranked cards with category, source, time, summary, insight, link
```

---

## 🤖 Supported LLM Providers

| Provider | Free Tier | Speed | Setup | Config |
|---|---|---|---|---|
| **Groq** ⭐ | 500+/day | ⚡ Fastest | 2 min (no card) | `llm.provider=groq` |
| Gemini 2.0 | 15/min | Fast | 2 min (no card) | `llm.provider=gemini` |
| OpenAI | $5+ | Fast | Requires card | `llm.provider=openai` |

> **Recommended:** Use Groq for development and demos. It's free, fast, and requires no credit card.

---

## ⚙️ Maven Dependencies

| Dependency | Purpose |
|---|---|
| `spring-boot-starter-web` | REST API |
| `rome:2.1.0` | RSS/Atom feed parsing |
| `httpclient5:5.3.1` | HTTP calls to LLM APIs |
| `jackson-databind` | JSON serialization |
| `lombok` | Optional boilerplate reduction |

---

## 🚀 Quick Start (5 minutes)

### Prerequisites
- Java 21+ installed
- Maven 3.8+ (or use IntelliJ's bundled Maven)
- Free API key (see next section)

### Step 1: Get a Free LLM API Key

**Option A: Groq (RECOMMENDED)** ✅
```bash
1. Go to https://console.groq.com
2. Sign up (no credit card needed)
3. Create API key in dashboard
4. Copy the key
```

**Option B: Google Gemini**
```bash
1. Go to https://aistudio.google.com
2. Sign in with Google
3. Click "Get API Key" → "Create API Key"
```

**Option C: OpenAI**
```bash
1. Go to https://platform.openai.com/api/keys
2. Add payment method
3. Create API key
```

### Step 2: Configure API Key
Edit `src/main/resources/application.properties`:

**For Groq (recommended):**
```properties
llm.provider=groq
llm.api.key=gsk_YOUR_GROQ_KEY_HERE
llm.model=mixtral-8x7b-32768
```

**For Gemini:**
```properties
llm.provider=gemini
llm.api.key=YOUR_GEMINI_KEY_HERE
llm.model=models/gemini-2.0-flash
```

**For OpenAI:**
```properties
llm.provider=openai
llm.api.key=sk-YOUR_OPENAI_KEY_HERE
llm.model=gpt-4-mini
```

### Step 3: Run in IntelliJ
1. Open `DailyTechNewsAgentApplication.java`
2. Click ▶ green Run button (or Shift+F10)
3. Wait for: `Started DailyTechNewsAgentApplication in X seconds`
4. Open browser: **http://localhost:8080**

### Step 4: Test!
Click **"⚡ Generate Daily Tech News"** button and watch the agent work.

---

## 🌐 Available Groq Models

Use any of these with Groq:
```properties
llm.model=mixtral-8x7b-32768        # Most popular (default)
llm.model=llama-3.1-8b-instant      # Lightweight
llm.model=llama-3.1-70b-versatile   # More powerful
```

---

## 📡 RSS Feeds Included (17 sources)

| Source | Category |
|---|---|
| TechCrunch | AI |
| NVIDIA Blog | AI |
| OpenAI Blog | AI |
| AWS Blog | Cloud |
| Google Cloud Release Notes | Cloud |
| The Hacker News | Cybersecurity |
| Krebs on Security | Cybersecurity |
| Dark Reading | Cybersecurity |
| InfoQ Java | Java |
| Baeldung | Java |
| InfoQ DevOps | DevOps |
| The New Stack | DevOps |
| InfoQ Data | Database |
| Stack Overflow Blog | Developer Tools |
| GitHub Blog | Developer Tools |
| Ars Technica | General Tech |
| Wired | General Tech |

---

## 🎤 Demo Explanation Script (20–30 minutes)

### Introduction (5 min)
> "Today we're building an AI Agent — not just calling an API, but an autonomous
> pipeline that uses multiple tools, makes decisions, and produces structured output.
>
> Think of it like a human researcher:
> 1. **Gather** → Fetch data from multiple sources
> 2. **Process** → Filter duplicates, categorize, rank by relevance
> 3. **Synthesize** → Use AI to write clear summaries
> 4. **Present** → Return structured, user-ready results
>
> That's what our agent does, but 100x faster."

### Architecture Walkthrough (5 min)
Point to `NewsAgentService.java`:
> "This is the orchestrator — the central 'brain'. It coordinates 3 specialized services.
> Each does ONE thing well. This is the **agent pattern**: coordination + experts.
>
> Unlike traditional code where you control flow, an agent delegates work and
> composes results intelligently."

Walk through the 4 steps, show each service file.

### The Prompt Engineering Section (5 min) ← MOST IMPORTANT
Open `LlmSummarizerService.java`, show `buildPrompt()`:
> "THIS is where the magic happens. This prompt is the complete specification.
> Notice the pattern:
>
> 1. **Role assignment** — 'You are an expert tech journalist'
> 2. **Structured input** — title, source, category, description injected
> 3. **Strict output format** — we demand JSON so we can parse it reliably
> 4. **Guardrails** — 'under 150 chars', 'no hype words', clear rules
>
> Prompt engineering is a **skill**. Better prompts = better outputs.
> Bad prompts = hallucinations and unparseable responses."

### Multi-Provider Pattern (3 min)
> "Notice: we support Groq, Gemini, AND OpenAI from the same code.
> The `summarizeWithLLM()` method dispatches to provider-specific implementations.
>
> This is good **software architecture**: abstractions over specifics.
> In production, you can switch providers without rewriting business logic."

### Live Demo (5 min)
1. Click Generate
2. Show the loading logs: "Fetching 17 feeds..."
3. Show processing: "Deduplication → Ranking..."
4. Show LLM calls: "Summarizing with [Groq/Gemini/OpenAI]..."
5. Show final cards with AI summaries, category badges, time-to-fetch

### Fallback Graceful Degradation (2 min)
Remove/invalidate API key → regenerate:
> "Production systems must handle LLM unavailability. Watch: agent still works,
> just uses RSS descriptions instead of AI summaries. No crashes,
> no angry users. This is **production-grade resilience**."

### Code Quality Points (5 min)
- `record FeedSource(...)` — Java records (immutable data)
- `Comparator.comparingInt(...).reversed()` — fluent Java streams
- Jaccard similarity for deduplication — real algorithm, not naive string match
- `@Value("${...}")` — externalized config (12-factor apps)
- Provider abstraction — clean DI pattern

---

## 🔮 Future Improvements

### Near-term (Easy wins)
1. **Caching** — Cache results for 30 min to avoid hammering RSS feeds
2. **Async fetching** — `CompletableFuture` for parallel feed fetch (10x faster)
3. **Scheduled generation** — `@Scheduled` to auto-run at 8am daily
4. **Persistence** — Store history with Spring Data JPA + PostgreSQL

### Advanced AI Features
5. **Multi-agent architecture** — Specialized agents for fetch, rank, summarize, QA
6. **Semantic deduplication** — Embedding vectors instead of keyword Jaccard
7. **User preferences** — Let users pick categories; LLM weights by interests
8. **Trend detection** — Identify topics appearing across sources
9. **Confidence scoring** — Ask LLM to rate its own summary quality
10. **RAG integration** — Retrieve historical articles for context

### Production Hardening
11. **Rate limiting** — Token bucket on endpoints
12. **Circuit breaker** — Resilience4j for LLM failures
13. **Observability** — Micrometer metrics + distributed tracing
14. **Docker** — Containerized deployment
15. **Tests** — Unit + integration tests with WireMock

---

## 🔑 Core Teaching Points

| Concept | Location in Code | Why It Matters |
|---|---|---|
| AI Agent Pattern | `NewsAgentService.java` | Orchestration + coordination |
| Prompt Engineering | `LlmSummarizerService.buildPrompt()` | Output quality = input quality |
| Structured LLM Output | `parseLlmResponse()` | JSON parsing makes results reliable |
| Graceful Degradation | Fallback in `LlmSummarizerService` | Production resilience |
| Multi-tool agents | `RssFeedService.java` | Agents use tools to gather info |
| Ranking & filtering | `NewsProcessorService.java` | Relevance = algorithm + heuristics |
| Java 17+ features | Records, text blocks, switch expr. | Modern Java is elegant |
| Provider abstraction | `summarizeWithGroq/Gemini/OpenAI` | Good design: swappable backends |

---

## 📊 API Endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/news/generate` | Run full agent pipeline, return top 10 news |
| `GET` | `/` | Serve UI (index.html) |

---

## 🛠️ Troubleshooting

### "API key not found"
```
→ Open application.properties
→ Set llm.api.key=your-actual-key (not placeholder)
→ Rebuild (Ctrl+Shift+F9)
```

### "HTTP 429 - Quota Exceeded"
```
→ Free tier limit reached
→ Either: wait a bit, or switch to different provider
→ Or: get paid tier
```

### "HTTP 404 - Not Found"
```
→ Check llm.provider matches your config
→ Check API endpoint URL is correct
→ Check model name is correct for that provider
```

### Agent runs but no LLM summaries
```
→ Check logs: "Gemini call failed… using fallback"
→ This is NORMAL — fallback mode is working
→ Verify API key is valid at provider dashboard
```

---

## 🎓 Learning Resources

- **Prompt Engineering**: https://platform.openai.com/docs/guides/prompt-engineering
- **Groq API Docs**: https://console.groq.com/docs
- **Spring Boot**: https://spring.io/projects/spring-boot
- **RSS Parsing (Rome)**: https://rometools.github.io/rome/

---

*Built for Introduction to LLM & AI Agents workshop.*
