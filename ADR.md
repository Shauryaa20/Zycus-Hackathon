# Architecture Decision Records (ADRs)

## ADR-001: Where commerce logic lives

**Context:** The application evaluates product inventory and demand signals to generate pricing and reorder suggestions. This logic must not turn the `ProductService` or `ProductController` into "god objects" handling persistence, events, HTTP, and complex business/AI policy.
**Options:**
- A. Embed in `ProductService`.
- B. Dedicated `CommerceAdvisor` interface with implementations, orchestrated by `RecommendationOrchestrator`.
**Decision:** **Option B**. We introduced a dedicated `CommerceAdvisor` interface. `ProductService` handles state and events, and an async listener delegates to `RecommendationOrchestrator`, which resolves the strategy and invokes the policy.
**Tradeoffs:** This introduces slightly more indirection but clearly separates the *mechanism* of reacting to events from the *policy* of how to price.

## ADR-002: Unified AI call vs separate pricing/reorder calls

**Context:** We need both pricing and reorder suggestions. Should we make one LLM call or two?
**Options:**
- A. One unified call returning both.
- B. Two separate calls.
**Decision:** **Option A**. The LLM is prompted for a unified JSON block containing both pricing and reorder properties.
**Tradeoffs:** A unified call is faster, reduces API costs (one request vs two), and allows the LLM to contextually balance the two (e.g., if it recommends a clearance price, it might recommend zero reorders). The tradeoff is that if the JSON parsing fails for one part, both suggestions fail. We mitigated this by wrapping the call in a try/catch block with a deterministic fallback to rules.

## ADR-003: Runtime strategy switching

**Context:** We need to switch between rule-based and AI-based strategies at runtime without code changes or restarts. A startup-only bean selection is not sufficient.
**Options:**
- A. Use Spring `@ConditionalOnProperty` (requires restart).
- B. Implement a dynamic `StrategyResolver`.
**Decision:** **Option B**. We implemented a `StrategyResolver` component containing a `ConcurrentHashMap` registry of all `CommerceAdvisor` beans. The `RecommendationOrchestrator` fetches the active strategy on *every* run. We exposed an `AdminController` (`POST /api/admin/strategy`) to update the configuration at runtime.
**Tradeoffs:** This provides true runtime switching and allows hot-swapping strategies without redeploying. Adding Sprint 2's `CompetitorAwareCommerceAdvisor` simply requires registering it in the resolver map.

## ADR-004: LLM failure handling and deterministic fallback

**Context:** LLMs can time out, return malformed JSON, or suggest absurd values (e.g., price = $0). An agentic loop cannot afford to silently drop suggestions.
**Options:**
- A. Fail silently.
- B. Bubble up exception to the orchestrator.
- C. Explicit validation boundary with deterministic fallback.
**Decision:** **Option C**. `AiCommerceAdvisor` parses the JSON and passes it through an explicit `validateResult` boundary. It ensures `recommendedPrice > 0` (and is within 0.2x to 2.0x of the current price) and `recommendedQuantity >= 1`. If validation fails, or if a network/parsing error occurs, the advisor catches the exception, logs it, and immediately calls `RuleBasedCommerceAdvisor`.
**Tradeoffs:** The system degrades gracefully to deterministic rules, guaranteeing that the loop always produces a suggestion.

## ADR-005: Agentic event-driven trigger and decoupling

**Context:** The system needs to generate suggestions when a checkout or stock update occurs, without blocking the HTTP request.
**Options:**
- A. Synchronous method calls in the controller.
- B. Scheduled polling (cron job).
- C. Async Event Listeners.
**Decision:** **Option C**. The `ProductService` publishes `ProductEvent`s. The `AgenticLoopListener` is annotated with `@Async` and `@EventListener` to process the event on a separate thread, delegating to `RecommendationOrchestrator`. 
**Tradeoffs:** It provides excellent decoupling and immediate reaction. The orchestrator checks for existing `PENDING` suggestions based on `productId`, `triggerReason`, and `suggestionType` to ensure idempotency so the merchandiser isn't bombarded with duplicate suggestions. This loop represents true agentic behavior: OBSERVE (inventory/demand signal) -> REASON (rule/AI strategy) -> ACT (create recommendation) -> CHECKPOINT (human approval) -> BUSINESS ACTION (update price/stock).

## ADR-006: Extensibility and deliberate exclusions

**Context:** Sprint 2 introduces margins, cost prices, and competitor strategies.
**Decision:** We added `costPrice` and `supplierId` as placeholder fields in the `Product` entity. We intentionally deferred complex security, authentication, payments, and a competitor scraping API to prioritize a robust agentic loop and clean backend boundaries. 
**Tradeoffs:** A future `CompetitorAwareCommerceAdvisor` will not require changes to controllers, listeners, orchestrators, or the UI. It will merely implement `CommerceAdvisor` and be registered in the `StrategyResolver`.
