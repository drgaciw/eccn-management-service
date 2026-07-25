# Plan: Spring AI Integration with LLM Council for ECCN Management Service

## Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Spring AI version | **2.0.0 BOM** | Compatible with Spring Boot 4.0.7 |
| Architecture | **LLM Council (3-member deliberation)** | Multi-LLM cross-verification for high-stakes ECCN compliance |
| Council members | **GPT + Gemini + Claude** | Three independent providers |
| Chairman | **Anthropic Claude** | Synthesizes final response |
| Provider access | **Direct per-provider config** | Independent API keys, no OpenRouter dependency |
| Tool access | **Chairman only** | Chairman invokes @Tool services to ground the final synthesis |
| Capability scope | **All 10 ECCN capabilities** | 8 concrete services + 2 no-op stubs |
| Compliance mode | **Read-only AI** | AI analyzes/suggests; mutations go through REST endpoints with auth |
| API surface | **Single `/api/assist/council`** | POST endpoint, returns multi-stage deliberation response |

## Council Deliberation Flow (3 Stages)

```
Client (NL prompt)
    │
    ▼
AssistController  POST /api/assist/council
    │
    ▼
CouncilOrchestrator
    │
    ├─ Stage 1: "First Opinions" ──────────────────────
    │   ├── GPT (openAiClient)     → stage1Response
    │   ├── Gemini (geminiClient)  → stage1Response
    │   └── Claude (claudeClient)  → stage1Response
    │
    ├─ Stage 2: "Peer Review" ─────────────────────────
    │   ├── GPT reviews Gemini + Claude responses (anonymized)
    │   ├── Gemini reviews GPT + Claude responses (anonymized)
    │   └── Claude reviews GPT + Gemini responses (anonymized)
    │   └── Each produces: ranking + critique
    │
    └─ Stage 3: "Chairman Synthesis" ──────────────────
        └── Claude (chairman) receives:
            ├── All 3 original responses
            ├── All 6 peer reviews (rankings, critiques)
            └── ECCN tool access (fact-check against DB)
            → Produces: final response with disclaimer
```

Provider anonymization in Stage 2: each LLM sees other responses labeled "Member A", "Member B", "Member C" with provider identity removed.

## Architecture

```
src/main/java/com/aciworldwide/eccn_management_service/
├── council/
│   ├── CouncilOrchestrator.java          # 3-stage deliberation engine
│   ├── CouncilModels.java               # Council model configuration record
│   └── CouncilMember.java               # ChatClient wrapper per member
├── tools/
│   └── EccnTools.java                   # @Tool methods (chairman's toolbox)
├── config/
│   ├── ChatClientConfig.java            # 3 ChatClient beans + chairman
│   └── CouncilConfig.java               # Council member models config
├── controller/
│   └── AssistController.java            # POST /api/assist/council
```

## Relevant Files

| File | Action |
|------|--------|
| `pom.xml` | Add `spring-ai-bom:2.0.0` + 3 provider starters |
| `src/main/java/.../council/CouncilOrchestrator.java` | **New** — orchestrates 3-stage deliberation |
| `src/main/java/.../council/CouncilModels.java` | **New** — record: `List<String> memberModels, String chairmanModel` |
| `src/main/java/.../council/CouncilMember.java` | **New** — record: `String name, ChatClient client, String provider` |
| `src/main/java/.../tools/EccnTools.java` | **New** — `@Component` with 12 `@Tool` methods |
| `src/main/java/.../config/ChatClientConfig.java` | **New** — 3 ChatClient beans (openAiClient, geminiClient, claudeClient) |
| `src/main/java/.../config/CouncilConfig.java` | **New** — `CouncilModels` config properties bean |
| `src/main/java/.../controller/AssistController.java` | **New** — `POST /api/assist/council` |
| `src/main/resources/application-ai-openai.properties` | **New** — OpenAI API key + model config |
| `src/main/resources/application-ai-google.properties` | **New** — Google Gemini API key + model config |
| `src/main/resources/application-ai-anthropic.properties` | **New** — Anthropic Claude API key + model config |
| `src/main/java/.../service/AutomatedClassificationToolService.java` | **Edit** — remove AIModel stub, inject ChatClient |

## Tasks

### 1. POM: Add Spring AI Dependencies

**File**: `pom.xml`

In `<dependencyManagement>`:
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-bom</artifactId>
    <version>2.0.0</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

In `<dependencies>`:
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-openai</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-google-genai</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-anthropic</artifactId>
</dependency>
```

**Verify**: `./mvnw -q -DskipTests compile` resolves all 3 starters.

### 2. ChatClient Configuration (3 Beams)

**File**: `src/main/java/.../config/ChatClientConfig.java`

Creates three `ChatClient` beans using Spring AI auto-configured `ChatClient.Builder` bound to each provider:

```java
@Configuration
public class ChatClientConfig {

    @Bean
    @Qualifier("openAiClient")
    public ChatClient openAiClient(ChatClient.Builder builder) {
        return builder.build();
    }

    @Bean
    @Qualifier("geminiClient")
    public ChatClient geminiClient(ChatClient.Builder builder) {
        return builder.build();
    }

    @Bean
    @Qualifier("claudeClient")
    public ChatClient claudeClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
```

Each bean is auto-configured by its respective provider starter when the corresponding `spring.ai.*` properties are present. Provider selection: the starter detects its properties and auto-creates the `ChatModel`; the `ChatClient.Builder` injects the right model automatically.

**Note**: In Spring AI 2.0.0, when multiple ChatModel beans exist (one per provider), each `ChatClient.Builder` is qualified to a specific model. The config above relies on Spring AI's `@ConditionalOnProperty` per provider. If auto-wiring ambiguity occurs, use `ChatClient.builder(chatModel)` with explicit `@Qualifier("openAiChatModel")` etc.

### 3. Council Configuration

**File**: `src/main/java/.../config/CouncilConfig.java`

```java
@Configuration
@ConfigurationProperties(prefix = "eccn.council")
public class CouncilConfig {

    private List<String> members = List.of(
        "openai/gpt-4.1",
        "google/gemini-2.5-pro",
        "anthropic/claude-sonnet-4.5"
    );

    private String chairman = "anthropic/claude-sonnet-4.5";

    // getters, setters
}
```

**File**: `src/main/resources/application.properties` — add:
```properties
# LLM Council
eccn.council.members=openai/gpt-4.1,google/gemini-2.5-pro,anthropic/claude-sonnet-4.5
eccn.council.chairman=anthropic/claude-sonnet-4.5
```

### 4. Provider Configuration Templates

Three new profile-specific properties files:

**`application-ai-openai.properties`**:
```properties
spring.ai.openai.api-key=${OPENAI_API_KEY}
spring.ai.openai.chat.options.model=gpt-4.1
spring.ai.openai.chat.options.temperature=0.0
```

**`application-ai-google.properties`**:
```properties
spring.ai.google.genai.api-key=${GOOGLE_API_KEY}
spring.ai.google.genai.chat.options.model=gemini-2.5-pro
spring.ai.google.genai.chat.options.temperature=0.0
```

**`application-ai-anthropic.properties`**:
```properties
spring.ai.anthropic.api-key=${ANTHROPIC_API_KEY}
spring.ai.anthropic.chat.options.model=claude-sonnet-4.5
spring.ai.anthropic.chat.options.temperature=0.0
```

### 5. CouncilOrchestrator — 3-Stage Deliberation

**File**: `src/main/java/.../council/CouncilOrchestrator.java`

```java
@Component
public class CouncilOrchestrator {

    private final List<CouncilMember> council;
    private final CouncilMember chairman;
    private final EccnTools tools;

    // Constructor injection of 3 ChatClients, chairman qualifier, EccnTools

    public CouncilResponse deliberate(String prompt) {
        // Stage 1: collect first opinions from all members
        List<MemberOpinion> stage1 = collectFirstOpinions(prompt);

        // Stage 2: peer review (anonymized)
        List<PeerReview> stage2 = collectPeerReviews(stage1);

        // Stage 3: chairman synthesis with tool access
        String finalResponse = chairmanSynthesis(prompt, stage1, stage2);

        return new CouncilResponse(stage1, stage2, finalResponse);
    }
}
```

**Stage 1 details**: For each council member, call `chatClient.prompt().user(prompt).call().content()`. No tools at this stage. Store provider-identity-stripped responses with metadata.

**Stage 2 details**: For each member, construct a review prompt containing the OTHER two members' responses (anonymized as "Member A", "Member B"). Ask: "Review these two responses. Rank them by accuracy and insight. Explain your ranking." Collect rankings and critiques.

**Stage 3 details**: Only the chairman (Claude). Prompt includes: original question, all 3 Stage 1 responses, all peer reviews from Stage 2. Chairman has `.tools(eccnTools)` for fact-checking. Chairman's system prompt includes compliance disclaimer.

**CouncilMember record**:
```java
public record CouncilMember(String name, ChatClient client, String provider) {}
```

**Response DTO**:
```java
public record CouncilResponse(
    List<MemberOpinion> stage1Opinions,
    List<PeerReview> stage2Reviews,
    String finalResponse,
    String disclaimer
) {}

public record MemberOpinion(String memberLabel, String provider, String content) {}
public record PeerReview(String reviewerLabel, String ranking, String critique) {}
```

### 6. EccnTools — @Tool-Annotated Methods

**File**: `src/main/java/.../tools/EccnTools.java`

`@Component` with 8 concrete tool methods + 2 no-op stubs:

| Capability | Tool Method | Delegates To | Args → Returns |
|------------|-----------|-------------|----------------|
| Record Mgmt | `searchEccnRecords` | EccnService.searchEccns() | String query → List&lt;Eccn&gt; summary |
| Record Mgmt | `getEccnRecord` | EccnService.findById() | String eccnCode → Eccn detail |
| Product Class. | `searchProducts` | ProductService.searchProductsByName() | String name → List&lt;Product&gt; summary |
| Product Class. | `getProductsByStatus` | ProductService.getProductsByStatus() | String status → List&lt;Product&gt; |
| Crypto Class. | `classifyCrypto` | CryptoClassificationService.classifyCryptography() | int keyLength, String algorithm → String classification |
| Glossary | `searchGlossary` | GlossaryService.searchByTermPart() | String term → List&lt;GlossaryEntry&gt; |
| Compliance Docs | `searchDocuments` | DocumentRecordService.searchDocuments() | String query → List&lt;DocumentRecord&gt; summary |
| Risk Assessment | `getRiskAssessments` | RiskAssessmentService.getAssessmentsByModule() | String moduleName → List&lt;RiskAssessment&gt; |
| Risk Assessment | `getHighRiskAssessments` | RiskAssessmentService.getHighRiskAssessments() | none → List&lt;RiskAssessment&gt; |
| Export Control | `getExportControls` | ExportControlService.getExportControlsByModule() | String moduleName → List&lt;ExportControl&gt; |
| Classification WF | `getWorkflowStatus` | EccnClassificationWorkflowService (read-only: iterate in-memory requests) | String releaseVersion → status string |
| Auto Class. | `suggestEccn` | AutomatedClassificationToolService.suggestECCN() | String moduleName → Map&lt;String, Double&gt; suggestions |
| Enterprise Int. | `validateExternalEccn` | **no-op stub** | String eccnCode → "Enterprise integration validation is not yet available" |
| Integration | `getIntegrationStatus` | **no-op stub** | String system → "Integration status check is not yet available" |

Every tool method uses `@Tool(description = "...")` from `org.springframework.ai.tool.annotation.Tool`. The description field is critical — it's what the LLM reads to decide which tool to call.

**No-op stubs** for unimplemented capabilities:
```java
@Tool(description = "Validate ECCN code against external databases. NOT YET IMPLEMENTED.")
public String validateExternalEccn(String eccnCode) {
    return "External ECCN validation is not yet available. " +
           "Use standard ECCN format validation instead.";
}
```

### 7. AssistController

**File**: `src/main/java/.../controller/AssistController.java`

```java
@RestController
@RequestMapping("/api/assist")
@Tag(name = "AI Assistant", description = "LLM Council deliberation endpoint")
public class AssistController {

    private final CouncilOrchestrator orchestrator;

    @PostMapping("/council")
    public ResponseEntity<CouncilResponse> deliberate(@RequestBody String prompt) {
        CouncilResponse response = orchestrator.deliberate(prompt);
        return ResponseEntity.ok(response);
    }
}
```

**Security**: Covered by `SecurityConfig.anyRequest().authenticated()`. No additional matcher needed.

### 8. Refactor AutomatedClassificationToolService

**File**: `src/main/java/.../service/AutomatedClassificationToolService.java`

- Remove `AIModel` interface (lines ~357-360)
- Remove `aiModel` field and `integrateAIModel()` method
- Inject `ChatClient` via constructor
- `attemptAIClassification()` calls `chatClient.prompt().user(getClassificationPrompt(...)).call().content()` with structured output parsing
- `suggestECCN()` calls `chatClient.prompt().user(getSuggestionPrompt(...)).call().content()` and parses confidence scores

The ChatClient injected here is the chairman's client (Claude), consistent with the chairman-only tool access rule.

### 9. Compliance Guardrails

- System prompt on chairman: "ECCN compliance assistant. Decision support only. Human compliance approval required."
- Every `CouncilResponse.finalResponse` includes disclaimer field text
- All `@Tool` methods are read-only (queries + suggestions only)
- No tool creates/updates/deletes records
- Audit trail: log every council session with correlation ID, prompt, stage durations, tool invocation count, council size

## Risk Assessment

| Risk | Level | Mitigation |
|------|-------|------------|
| AI provides incorrect classification | MEDIUM | 3-way cross-verification + chairman synthesis + human approval required |
| Council cost (3x API calls) | MEDIUM | 3 members is baseline; configurable to reduce to 2 or increase to 4 |
| Google GenAI starter incompatible with 2.0.0 | LOW | Verified: `spring-ai-starter-model-google-genai` exists in 2.0.0 BOM |
| Anthropic starter incompatible with 2.0.0 | LOW | Verified: `spring-ai-starter-model-anthropic` exists in 2.0.0 BOM |
| Multiple ChatModel beans ambiguity | LOW | Spring AI auto-qualifies by provider; fallback: explicit `ChatClient.create(model)` |
| Council serial latency (3 sequential calls) | MEDIUM | Stage 1 calls fire in parallel (VirtualThreads enabled); Stage 2 parallel |

## Provider Property Mapping (verified against Spring AI 2.0.0)

| Provider | Starter Artifact | Property Prefix | API Key Property |
|----------|-----------------|-----------------|-----------------|
| OpenAI | `spring-ai-starter-model-openai` | `spring.ai.openai` | `spring.ai.openai.api-key` |
| Google Gemini | `spring-ai-starter-model-google-genai` | `spring.ai.google.genai` | `spring.ai.google.genai.api-key` |
| Anthropic Claude | `spring-ai-starter-model-anthropic` | `spring.ai.anthropic` | `spring.ai.anthropic.api-key` |

## Validation Checklist

- [ ] `spring-ai-bom:2.0.0` added to pom.xml dependency management
- [ ] Three provider starters added (openai, google-genai, anthropic)
- [ ] `ChatClientConfig` creates 3 ChatClient beans with qualifiers
- [ ] `CouncilConfig` loads `eccn.council.*` properties
- [ ] `CouncilOrchestrator` implements 3-stage deliberation
- [ ] `EccnTools` `@Component` with 12 `@Tool` methods + 2 no-op stubs
- [ ] `AssistController` with `POST /api/assist/council`
- [ ] Three `application-ai-*.properties` files created
- [ ] `AutomatedClassificationToolService` refactored: AIModel removed, ChatClient injected
- [ ] `./mvnw -q -DskipTests compile` passes with all Spring AI deps resolved
- [ ] `./mvnw test` — all 71 tests pass (AI config auto-config skips when provider properties absent)
- [ ] SecurityConfig unchanged (anyRequest().authenticated() covers /api/assist/**)
