# StockPulse 🚀

### AI-Powered Inventory & Dynamic Pricing Recommendation System

StockPulse is an event-driven commerce intelligence system that monitors product inventory and demand signals to generate **pricing and reorder recommendations**.

The system combines **deterministic business rules with a real Gemini LLM**, while keeping humans in the loop before any consequential pricing or inventory action is applied.

---

## 🎯 Problem

E-commerce businesses continuously need to answer:

* When should the price of a product change?
* Should inventory be reordered?
* What should happen when stock becomes critically low?
* What should happen when demand suddenly spikes?

Traditional rule-based systems are predictable but limited, while relying completely on an LLM is risky and unreliable.

StockPulse combines both approaches:

> **Rules provide reliability. AI provides contextual reasoning. Humans provide final approval.**

---

# 🏗️ Architecture

```text
                         React Frontend
                              │
                         REST / Axios
                              │
                              ▼
                        Spring Boot API
                              │
                              ▼
                         Controllers
                              │
                              ▼
                        ProductService
                              │
                    Stock / Velocity Update
                              │
                       Trigger Detection
                              │
                              ▼
                        ProductEvent
                              │
                              ▼
                  @Async + @EventListener
                              │
                              ▼
                 RecommendationOrchestrator
                              │
                       Idempotency Check
                              │
                              ▼
                      StrategyResolver
                         /          \
                        /            \
                       ▼              ▼
              RuleBased Advisor    AI Advisor
                                      │
                                      ▼
                                 LLMGateway
                                      │
                                   WebClient
                                      │
                                      ▼
                              Gemini 3.6 Flash
                                      │
                                      ▼
                                  JSON Response
                                      │
                                      ▼
                               Java Validation
                                  /       \
                               Valid     Invalid
                                 │          │
                                 ▼          ▼
                                AI        Rules
                                 \          /
                                  \        /
                                   ▼      ▼
                              Recommendations
                                      │
                                   PENDING
                                      │
                                      ▼
                              Human Approval
                                /         \
                             Accept       Reject
                                │
                                ▼
                         Transactional Action
                                │
                                ▼
                          Product State Update
```

---

# 🤖 Agentic Loop

StockPulse follows an event-driven recommendation loop:

```text
OBSERVE
   ↓
Detect inventory / demand signal
   ↓
REASON
   ↓
AI or deterministic strategy
   ↓
RECOMMEND
   ↓
Create pricing + reorder suggestions
   ↓
CHECKPOINT
   ↓
Human approval
   ↓
BUSINESS ACTION
```

This allows the system to react to meaningful commerce events instead of continuously polling every product for changes.

---

# ⚡ Core Triggers

## 1. Inventory Low

When product inventory falls below its configured reorder threshold:

```text
Stock < Reorder Threshold
        ↓
INVENTORY_LOW
        ↓
Generate Recommendation
```

The system can recommend:

* Price adjustment
* Reorder quantity

---

## 2. Demand Spike

When product demand velocity exceeds the configured category-level threshold:

```text
Demand Velocity > Spike Threshold
        ↓
DEMAND_SPIKE
        ↓
Generate Recommendation
```

The AI receives different contextual information for this trigger because a demand spike represents a different merchandising problem from low inventory.

---

# 🧠 AI Architecture

The AI flow is:

```text
AiCommerceAdvisor
        ↓
Build contextual prompt
        ↓
LLMGateway
        ↓
Spring WebClient
        ↓
Gemini API
        ↓
JSON Response
        ↓
Parse response
        ↓
Validate result
        ↓
Commerce Recommendation
```

### Current AI Configuration

```text
Provider: Gemini
Model: Gemini 3.6 Flash
Communication: HTTP REST
Client: Spring WebClient
```

The API key is supplied through the environment rather than committed to the repository.

---

# 🛡️ AI Safety & Fallback

LLM output is **not blindly trusted**.

The system validates the response in Java after receiving it from Gemini.

If the LLM:

* Fails to respond
* Returns malformed JSON
* Produces an invalid recommendation
* Fails validation

the system falls back directly to:

```text
RuleBasedCommerceAdvisor
```

```text
                 Gemini
                    │
             ┌──────┴──────┐
             │             │
           Valid         Failure
             │             │
             ▼             ▼
        AI Result       Rule-Based
```

This provides graceful degradation when the external AI service is unavailable.

---

# 🔄 Strategy Pattern

Recommendation generation is abstracted through:

```java
CommerceAdvisor
```

Current implementations:

```text
CommerceAdvisor
├── RuleBasedCommerceAdvisor
└── AiCommerceAdvisor
```

The orchestrator does not need to know which implementation is being used.

It simply requests a recommendation from the active advisor.

---

# 🔀 Runtime Strategy Switching

StockPulse supports switching between AI and rule-based strategies while the application is running.

```text
AI → RULE → AI
```

`StrategyResolver` maintains the registered strategies and resolves the active strategy for every recommendation execution.

The admin API allows runtime switching:

```text
POST /api/admin/strategy
```

Example:

```json
{
  "strategy": "RULE"
}
```

No application restart is required.

---

# 🔁 Idempotency

Multiple events can occur within a short period.

StockPulse checks for existing `PENDING` recommendations before creating new ones.

This prevents repeated events from unnecessarily generating duplicate pending recommendations.

Conceptually:

```text
Product + Trigger + PENDING
             ↓
Already exists?
      /             \
    YES              NO
     ↓                ↓
  Skip             Generate
```

Pricing and reorder recommendations are handled independently.

---

# 👤 Human-in-the-Loop

AI recommendations do not directly modify business state.

Instead:

```text
AI / Rules
    ↓
Recommendation
    ↓
PENDING
    ↓
Human Review
   /      \
Accept    Reject
   ↓
Business Action
```

This provides a human checkpoint before applying potentially consequential pricing or inventory changes.

---

# 💾 Data Model

Core entities include:

### Product

Stores product information such as:

* SKU
* Name
* Category
* Price
* Stock
* Reorder threshold
* Demand velocity
* Cost price
* Supplier

### PricingSuggestion

Stores proposed pricing changes and their approval status.

### ReorderSuggestion

Stores proposed inventory replenishment and its approval status.

### Suggestion Status

```text
PENDING
ACCEPTED
REJECTED
```

### Product State

```text
ACTIVE
PRICE_REVIEW_PENDING
OUT_OF_STOCK
```

---

# 🧩 Technology Stack

## Backend

* Java 17
* Spring Boot 3.2.3
* Spring Data JPA
* Hibernate
* Spring WebFlux WebClient
* Spring Events
* Spring `@Async`
* Maven

## Database

* H2
* JPA/Hibernate

## AI

* Google Gemini
* Gemini 3.6 Flash
* Direct REST API integration
* WebClient

## Frontend

* React
* TypeScript
* Vite
* Tailwind CSS
* Axios

## Testing

* JUnit
* Mockito
* Maven Test

---

# 📂 Backend Architecture

```text
Controller
    ↓
Service
    ↓
Repository
```

Recommendation processing:

```text
ProductService
    ↓
ProductEvent
    ↓
AgenticLoopListener
    ↓
RecommendationOrchestrator
    ↓
StrategyResolver
    ↓
CommerceAdvisor
```

AI:

```text
AiCommerceAdvisor
    ↓
LLMGateway
    ↓
Gemini
```

---

# 🖥️ Frontend

The React dashboard allows users to:

* View products
* Simulate sales
* Monitor inventory
* View pricing recommendations
* View reorder recommendations
* Accept/reject suggestions
* Switch recommendation strategies

The frontend currently uses polling to detect newly generated recommendations.

Future real-time delivery could use **Server-Sent Events (SSE)** or WebSockets.

---

# 🚀 Demo Flow

## Demo 1 — Inventory Low

```text
Select low-stock product
        ↓
Simulate Sale
        ↓
Stock decreases
        ↓
INVENTORY_LOW triggered
        ↓
Async recommendation generation
        ↓
AI recommendation appears
        ↓
Accept pricing suggestion
        ↓
Product price updates
```

---

## Demo 2 — Demand Spike

```text
Select high-demand product
        ↓
Simulate multiple sales
        ↓
Demand velocity increases
        ↓
DEMAND_SPIKE triggered
        ↓
AI receives demand-spike context
        ↓
Pricing + reorder suggestions
```

---

## Demo 3 — Runtime Strategy Switching

```text
AI
 ↓
Switch to RULE
 ↓
Generate recommendation
 ↓
Switch back to AI
 ↓
Generate recommendation
```

All without restarting the application.

---

# 🧪 Testing

The project includes tests covering important architectural behavior:

### StrategyResolverTest

Verifies runtime switching between recommendation strategies.

### RecommendationOrchestratorTest

Tests recommendation generation and idempotency behavior.

### AiCommerceAdvisorTest

Tests AI response handling and deterministic fallback behavior.

Run the test suite with:

```bash
mvn clean test
```

---

# ⚙️ Configuration

The application expects the LLM API key through an environment variable.

Example:

```text
LLM_API_KEY=<your-api-key>
```

The secret should **never be committed to Git**.

Configuration follows the pattern:

```properties
llm.provider=gemini
llm.model=gemini-3.6-flash
llm.api-key=${LLM_API_KEY:}
llm.base-url=https://generativelanguage.googleapis.com
```

---

# 🛠️ Running Locally

## Backend

```bash
cd backend
mvn spring-boot:run
```

Set the LLM API key before starting the backend.

## Frontend

```bash
cd frontend
npm install
npm run dev
```

Then open the frontend URL provided by Vite.

---

# 🔐 Security Note

Do not commit:

```text
.env
API keys
credentials
application-local.properties
```

Use environment variables for secrets.

---

# 📈 Future Improvements

The architecture is designed to support future strategies and infrastructure.

Potential improvements include:

* `CompetitorAwareCommerceAdvisor`
* Margin-aware pricing
* Competitor price integration
* PostgreSQL for production persistence
* Kafka/RabbitMQ for durable event processing
* SSE/WebSockets for real-time recommendations
* Retry and exponential backoff for LLM failures
* Distributed strategy configuration
* Authentication and authorization
* Production observability and monitoring

---

# 🏆 Key Design Principles

StockPulse is built around five principles:

### 1. Separation of Concerns

Product management, event handling, orchestration, recommendation policy and AI communication are separated.

### 2. Strategy Pattern

Recommendation algorithms are interchangeable.

### 3. Event-Driven Processing

Product events trigger recommendations asynchronously.

### 4. AI with Deterministic Guardrails

Gemini provides contextual reasoning, while Java validates the result.

### 5. Human-in-the-Loop

AI recommends; a human decides whether to apply the recommendation.

---

# 📜 Architecture Decisions

Major architectural decisions are documented in [`ADR.md`](ADR.md), including:

* Commerce logic separation
* Unified AI call
* Runtime strategy switching
* AI validation and fallback
* Event-driven asynchronous processing
* Idempotency and human approval
* Extensibility and deliberate scope

---

# 👨‍💻 Project Philosophy

StockPulse is not designed around the idea that **"AI should control everything."**

Instead:

```text
              AI
               ↓
        Contextual Reasoning
               ↓
        Deterministic Validation
               ↓
        Human Approval
               ↓
        Business Action
```

The goal is to combine the flexibility of LLM-based reasoning with the reliability, safety and predictability required by commerce systems.

---

## ⭐ StockPulse

**Observe → Reason → Recommend → Human Checkpoint → Act**
