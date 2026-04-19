---
name: code-review
description: Structured code review following software craftsmanship principles — Clean Code, SOLID, security, design patterns, testability, and Spring Boot conventions. Use when user asks to "review this code", "code review", "check this PR", "review my changes", "what's wrong with this code", or pastes code and asks for feedback. Also use when reviewing pull requests, evaluating code quality, or when the user wants a second pair of eyes on their implementation. Triggers on any request involving code quality assessment, even if the user doesn't explicitly say "review".
---

# Code Review Skill

Perform thorough, structured code reviews grounded in software craftsmanship principles. A good code review isn't about finding fault — it's about elevating the code and sharing knowledge. The goal is to leave the codebase better than you found it while respecting the author's intent.

## Review Philosophy

Code review is a conversation, not an audit. Focus on:
- **Correctness first** — Does it work? Are there bugs or edge cases?
- **Clarity second** — Can the next developer understand this in 6 months?
- **Craft third** — Does it follow established principles and patterns?

Avoid nitpicking formatting or style issues that an automated tool should handle. Focus your human attention on design, logic, and the things machines can't catch.

---

## Review Workflow

### Step 1: Understand Context

Before writing a single comment, understand what the code is trying to do:

1. Read the code the user points you at (or the files they changed)
2. Identify the **purpose** — what problem does this solve?
3. Check the **scope** — is this a new feature, bug fix, refactoring, or configuration change?
4. Look at **surrounding code** — read the classes, interfaces, and tests that interact with the changed code

This context-gathering step matters because a review that misunderstands intent wastes everyone's time. If the purpose isn't clear, ask the user before proceeding.

### Step 2: Multi-Lens Analysis

Review the code through each of these lenses, in order. Not every lens applies to every review — skip any that aren't relevant. For each lens, load the corresponding skill if a deeper analysis is warranted.

#### Lens 1: Correctness & Logic
The most important lens. No amount of clean code matters if the logic is wrong.

- **Edge cases**: null inputs, empty collections, boundary values, concurrent access
- **Error handling**: are exceptions meaningful? Are failure paths handled gracefully?
- **Data integrity**: race conditions in shared state, transaction boundaries, cache coherence
- **API contracts**: does the method do what its name and signature promise?

#### Lens 2: Clean Code
Reference the `clean-code` skill for detailed principles (DRY, KISS, YAGNI, naming, function size).

Key review checkpoints:
- Are names intention-revealing? Can you understand the code without comments?
- Are methods small and focused on a single abstraction level?
- Is there duplicated logic that should be extracted?
- Is any code "just in case" that isn't needed now?

#### Lens 3: SOLID Principles
Reference the `solid-principles` skill for detailed principles and Java examples.

Key review checkpoints:
- **SRP**: Does each class have one clear reason to change? Can you describe its purpose in one sentence without "and"?
- **OCP**: Will adding a new variant require modifying existing code, or can you extend?
- **LSP**: Do subclasses honor the contracts of their parent types?
- **ISP**: Are interfaces focused, or do implementors have empty/throwing methods?
- **DIP**: Does the code depend on abstractions or concrete implementations?

#### Lens 4: Security
Reference the `security-audit` skill for OWASP Top 10 and secure coding patterns.

Key review checkpoints:
- Input validation at system boundaries (DTOs, controllers, external API calls)
- SQL injection: parameterized queries, no string concatenation in JPQL/SQL
- Sensitive data: no secrets in code, no PII in logs
- Authorization: are access checks in place for protected operations?

#### Lens 5: Design Patterns
Reference the `design-patterns` skill for pattern catalog and anti-patterns.

Key review checkpoints:
- Is the code using an appropriate pattern for the problem? (Strategy for multiple algorithms, Builder for complex construction, etc.)
- Are patterns being forced where a simpler approach works?
- Are there anti-patterns? (God class, Feature Envy, Primitive Obsession)

#### Lens 6: Testability & Test Coverage
Code that's hard to test is usually hard to maintain.

- Can each class be tested in isolation? Are dependencies injectable?
- Are there side effects that make testing difficult? (static calls, `new` inside methods, hidden dependencies)
- If tests exist, do they test behavior (not implementation details)?
- Are edge cases covered in tests?

#### Lens 7: Spring Boot & Framework Conventions
For Spring Boot projects specifically:

- **Layering**: Controller -> Service -> Repository, no layer skipping
- **Transactions**: `@Transactional` on service methods, not controllers; `readOnly = true` for queries
- **Validation**: Bean validation on DTOs, `@Valid` in controllers
- **Exception handling**: Domain exceptions translated centrally via `@RestControllerAdvice` with `ProblemDetail`
- **Caching**: Proper cache key design, eviction on mutations
- **Configuration**: Environment-specific values externalized, no hardcoded connection strings or credentials

### Step 3: Categorize Findings

Organize findings by severity so the author knows what to prioritize:

| Severity | Meaning | Action Required |
|----------|---------|-----------------|
| **Critical** | Bug, security vulnerability, data loss risk | Must fix before merge |
| **Major** | Design flaw, SOLID violation, missing validation | Should fix before merge |
| **Minor** | Clean code improvement, naming, simplification | Nice to fix, not blocking |
| **Note** | Observation, alternative approach, learning point | No action required |

### Step 4: Present the Review

Structure the review output like this:

```
## Code Review: [File or Component Name]

### Summary
[1-2 sentence overview: what the code does well and the main area for improvement]

### Findings

#### Critical
- [Finding with file reference and line number]
  - **Why it matters**: [Brief explanation of impact]
  - **Suggestion**: [Concrete fix, ideally with code snippet]

#### Major
- [Same structure]

#### Minor
- [Same structure]

#### Notes
- [Observations, compliments on good patterns, alternative approaches]

### What's Done Well
[Specifically call out good practices — this matters for morale and learning]
```

Always include "What's Done Well" — recognizing good work is part of a healthy review culture. If everything is great, say so and explain why.

---

## Common Anti-Patterns to Flag

These come up frequently in Java/Spring projects:

| Anti-Pattern | What It Looks Like | Better Approach |
|---|---|---|
| **God Service** | Service class with 500+ lines, doing validation, mapping, business logic, and notifications | Split responsibilities: dedicated validator, mapper, domain service |
| **Anemic Domain Model** | Entities are pure data bags; all logic lives in services | Move behavior that belongs to the entity into the entity |
| **Catch-and-Ignore** | `catch (Exception e) { }` or `catch (Exception e) { log.error(e); }` without recovery | Handle meaningfully or let it propagate |
| **Primitive Obsession** | Email, phone, money represented as raw `String`/`double` | Introduce value objects when the same primitive is validated/formatted in multiple places |
| **Boolean Blindness** | `process(true, false, true)` — impossible to read at call site | Use enums or parameter objects |
| **Law of Demeter Violation** | `order.getCustomer().getAddress().getCity()` — chained getters reaching deep | Ask, don't tell — let objects do work, don't navigate their structure |
| **Missing Transaction Boundary** | Service methods that modify data without `@Transactional` | Add proper transaction annotations |

---

## Review Etiquette

When presenting findings, follow these communication principles:

- **Be specific**: "The `sortBy` parameter accepts arbitrary field names which could cause `PropertyReferenceException`" beats "the sorting is wrong"
- **Explain why**: Don't just say "this violates SRP" — explain the concrete consequence: "If the email template changes, you'll have to modify the order service"
- **Suggest, don't demand**: "Consider extracting this into a validator" reads better than "You must extract this"
- **Ask questions when unsure**: "Was there a reason this isn't using `@Transactional(readOnly = true)` here?" — maybe the author knows something you don't
- **Praise good work**: "Nice use of `@CachePut` here to keep the cache warm after updates" reinforces good patterns

---

## Related Skills

When a specific review lens needs deeper analysis, load the corresponding skill:

- `clean-code` — DRY, KISS, YAGNI, naming conventions, function design
- `solid-principles` — SRP, OCP, LSP, ISP, DIP with Java examples
- `security-audit` — OWASP Top 10, input validation, injection prevention
- `design-patterns` — Factory, Builder, Strategy, Observer, Decorator patterns
