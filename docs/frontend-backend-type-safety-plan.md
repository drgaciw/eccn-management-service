# Plan — Mapping the Angular Frontend to the Spring Backend, With Type Safety

**Date:** 2026-07-25
**Backend:** `eccn-management-service` — Spring Boot 4.0.7, Java 21, 10 controllers / 57 endpoints
**Frontend:** `eccn-management-ui-service-angular/frontend` — Angular 20.3.26, standalone + zoneless

> **Revision note.** An earlier draft of this plan framed the problem as type drift and led with
> OpenAPI codegen. Fuller investigation showed that framing was wrong: the dominant problem is
> that **most of the API the frontend calls does not exist yet**. Codegen is still the right
> destination, but it is not the first move — see §3.

---

## 1. The actual problem

The frontend was built against the **target API described in the PRD**. The backend implements a
subset. `CLAUDE.md` already records this: *"Postman includes target multi-service/auth flows that
are not fully implemented locally."*

Of roughly **37 backend-facing HTTP calls** in the Angular services, about **6 resolve**.

| Frontend service | Calls | Resolve | Notes |
|---|---|---|---|
| `eccn-data.service.ts` | 11 | **0** | Wrong base path *and* an entire `/classifications` sub-resource that does not exist |
| `export-control.service.ts` | 14 | 3 | `/controlled-countries` vs backend `/country-restrictions`; `/stats` vs `/dashboard` |
| `product.service.ts` | 12 | 3 | Missing `/categories`, `/stats`, `/bulk-import`, `/{id}/versions` |

### 1.1 The backend has the logic, not the surface

**67 public service methods have no HTTP surface at all:**

| Service | Public methods | Controller |
|---|---|---|
| `AutomatedClassificationToolService` | 28 | none |
| `DocumentRecordService` | 15 | none |
| `EccnClassificationWorkflowService` | 14 | none |
| `RiskAssessmentService` | 10 | none |
| `ExternalEccnValidationService` | 0 (stub) | none |
| `IntegrationService` | 0 (stub) | none |

This is the same pattern as `ExportControlService`, which had a fully implemented service and no
controller until one was restored during the lineage port. **The gap is mostly thin controllers
over existing logic, not unbuilt features.** That materially lowers the cost of closing it.

### 1.2 Type-level drift (real, but secondary)

| Frontend declares | Backend reality |
|---|---|
| `EccnListResponse { content, totalElements, totalPages, size, number }` | `ResponseEntity<List<Eccn>>` — bare array |
| `EccnClassification.status?: 'Active' \| 'Inactive' \| 'Deprecated'` | `Eccn` has no `status` field |
| specs assert `items`, `'ACTIVE'`, `ClassificationRequest.eccnCode` | third shape, matching neither |

### 1.3 Why nothing caught it

Per the Angular documentation:

> The generic type used with HttpClient request methods is a type assertion about the data
> returned by the server. HttpClient itself does not verify that the actual return data matches
> this asserted type.
> — https://angular.dev/guide/http/making-requests

`http.get<EccnListResponse>(...)` against an endpoint returning 404 still compiles green. There is
no codegen, no contract test, and no lint gate (`eslint.config.js` does not exist).

---

## 2. Decisions

Taken as owner of the mapping, grounded in the codebase and framework guidance.

### D1 — `eccn-data.service.ts` base path → `/api/v1/eccn`, and collapse `/classifications`

Repointing alone does not help; the sub-resource is missing entirely. Two parts:

- **Use `/api/v1/eccn`**, the explicitly versioned path, not `/api/eccn`. URL versioning is
  unambiguous and does not depend on `spring.mvc.apiversion.default`, which today the frontend
  relies on by accident.
- **Collapse `/classifications` into the collection root.** `/api/v1/eccn/classifications` is
  redundant — in this domain the ECCN record *is* the classification. `GET /classifications`
  becomes `GET /api/v1/eccn`. This is a frontend-side change and yields cleaner REST.

Genuinely new operations still needed: `POST /classify`, `GET /validate/{code}`,
`GET /categories`, `GET /{id}/history`. All map onto `EccnService`,
`AutomatedClassificationToolService` and `EccnClassificationWorkflowService`, which exist.

### D2 — Introduce an explicit `PageResponse<T>` DTO; do not serialize Spring's `Page`

The frontend already assumes a page envelope, and ECCN datasets grow — so paginate. But **do not
return `org.springframework.data.domain.Page` directly from a controller.** Its JSON form is an
implementation detail that has changed across Spring versions; Spring introduced `PagedModel`
precisely because serializing `Page` was an unstable public contract.

Define a project DTO:

```java
public record PageResponse<T>(
    List<T> content, long totalElements, int totalPages, int size, int number) {}
```

This matches the frontend's existing `EccnListResponse` field-for-field, documents cleanly in
OpenAPI, and decouples the wire format from Spring internals. Note there is currently **zero**
`Pageable`/`Page` usage in the backend, so this is additive work across list endpoints.

### D3 — Do **not** add `status` to `Eccn`. Expose workflow status instead.

The concept exists (`Product.status`, `EccnClassificationWorkflowService` tracks status), so the
temptation is to add a field. Resisted deliberately: under the project's business guardrails,
classification status is compliance-significant — `CLASSIFIED` status requires named human
approval. A bare mutable `status` on the record would let the UI imply a classification state
that never passed an approval gate.

Correct mapping: surface status through the classification-workflow endpoints, as workflow state
with its transitions, not as a free field on the ECCN document. **This one needs a named
compliance owner's sign-off before implementation** — it is a domain decision, not a typing one.

### D4 — Contract crosses repos as a vendored `openapi.json` plus a CI freshness check

Vendoring is simple and reviewable; its failure mode is going stale, which the freshness check
in Phase 4 closes. A published build artifact is cleaner but needs infrastructure that does not
exist yet. Revisit if the repos gain shared CI.

### D5 — Pin `X-API-Version` explicitly in a functional interceptor

The frontend sends no version header and receives v1 by default-value accident. The app already
uses a functional interceptor (`errorInterceptor`), so this follows established local pattern and
the documented Angular approach:

```ts
export const apiVersionInterceptor: HttpInterceptorFn = (req, next) =>
  next(req.clone({ headers: req.headers.set('X-API-Version', '1') }));
```

### D6 — `openapi-typescript` (types only), not a full client generator

The frontend has hand-written services with interceptors and signal-based state. `orval` or
`ng-openapi-gen` would replace or duplicate that layer. Types-only swaps just the interfaces —
smaller, reversible, and preserves the existing architecture. Revisit once types are trusted.

---

## 3. Phased plan

Ordering matters: generating types from today's backend would mark ~84% of frontend calls as
nonexistent, producing a wall of errors and no working app. **Close the surface gap first, then
lock it with codegen.**

### Phase 1 — Reconcile the contract (decide per capability)

For each frontend call that does not resolve, choose: implement the backend endpoint, or retreat
the frontend to what exists. Driven by the PRD, capability by capability. Produces a signed-off
endpoint inventory — the input to every later phase.

Cheap wins available immediately (pure renames, no new logic):
- `/controlled-countries` → `/country-restrictions`
- `/stats` → `/dashboard`

**Exit:** an agreed endpoint list; every frontend call is either mapped or explicitly deferred.

### Phase 2 — Close the surface gap

Add thin controllers over the 67 unexposed service methods, per the Phase 1 inventory. Priority
by frontend need: ECCN classifications first (0% hit rate), then export-control, then product.

Follow the conventions the modernization already established: `ResponseEntity<T>` returns,
`@Valid` request bodies, 201 on create, `@Schema` annotations for OpenAPI quality.

**Exit:** every agreed endpoint returns 2xx against a running backend.

### Phase 3 — Generate types

1. Add `springdoc-openapi-maven-plugin`; emit and commit `docs/openapi.json`
2. Vendor into `frontend/src/api/openapi.json`
3. `"generate:api": "openapi-typescript src/api/openapi.json -o src/api/schema.d.ts"`
4. Re-export generated types from `src/models/*.ts`, then delete the hand-written interfaces

**Exit:** no hand-authored interface describes a backend payload; `ng build` passes.

**Risk:** springdoc emits weak schemas for `Map<String,String>` params (`getAllEccns`) and untyped
`Object` responses. Where it does, the fix is backend annotation work.

### Phase 4 — Make drift a build failure

- Backend CI regenerates `openapi.json`, fails if the committed copy is stale
- Frontend CI regenerates `schema.d.ts`, fails if it differs from the committed copy
- Freshness check that the vendored spec matches the backend's published one

Without the third, the frontend can pin a stale contract indefinitely — the present failure mode
in a new costume.

**Exit:** a backend field rename fails the frontend build.

### Phase 5 — Repair the test suite

Rewrite specs against generated types so they cannot assert a shape that does not exist. This
unblocks `ng test`, which is itself a prerequisite for the Angular v21/v22 upgrades tracked in
`frontend/docs/angular-frontend-version-audit.html`.

---

## 4. Runtime validation — do not skip

Generated types are compile-time only. Given the Angular documentation's explicit warning that
`HttpClient` does not verify responses, and given how far the two sides have already drifted,
compile-time types alone would have caught **none** of the current 404s.

`zod` is already a frontend dependency (4.1.11). Generate schemas from the same OpenAPI document
and validate at the HTTP boundary — in the existing interceptor, so it is one place. Recommend
introducing this in Phase 4 rather than deferring indefinitely.

---

## 5. Sequencing and risk

| Phase | Depends on | Risk if skipped |
|---|---|---|
| 1 Reconcile contract | — | Everything downstream is built on guesses |
| 2 Close surface gap | 1 | Frontend stays ~84% broken |
| 3 Generate types | 1, 2 | Codegen encodes a broken contract |
| 4 CI gate + runtime validation | 3 | Generation exists, nothing enforces it |
| 5 Tests | 3 | No safety net; blocks the Angular upgrade path |

**Largest risk:** Phase 1 is a product decision, not an engineering one, and will stall without an
owner. It is also where D3 (ECCN status) needs named compliance sign-off.

**Second risk:** Phase 2 is the bulk of the work. It is cheaper than it looks because the logic
exists — but 67 methods is still meaningful scope, and each new endpoint is a public contract.

---

## 6. What this does not cover

- Authentication contract. `SecurityConfig` permits actuator and docs paths; the frontend's auth
  posture was not examined.
- The Genkit AI server, which has a separate HTTP surface on port 3100.
- Whether the PRD's target API is still the intended destination. This plan assumes it is; if the
  product direction has changed, Phase 1 should retreat the frontend instead of building forward.
