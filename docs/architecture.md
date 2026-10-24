# SkillNova AI architecture

## System shape

The application is a monorepo with two independently runnable applications:

```text
Browser
  └── Next.js frontend (Vercel)
        └── JSON/HTTPS API
              └── Spring Boot backend (Render)
                    ├── PostgreSQL (Neon)
                    └── AI provider (OpenAI or Gemini; provider integration in Phase 8)
```

The frontend owns presentation, form state, and calls to the API. The backend owns authentication, validation, authorization, business rules, persistence, and calls to the AI provider. The browser must never receive database credentials, JWT signing secrets, or AI provider keys.

## Backend package responsibilities

Base package: `com.skillnova.backend`.

| Package | Responsibility |
| --- | --- |
| `config` | Spring configuration such as CORS, serialization, and application beans. |
| `controller` | REST routes; translate HTTP input/output and delegate to services. |
| `dto` | API boundary types. Requests are validated; entities are not returned directly. |
| `entity` | JPA entities and persistence relationships. |
| `exception` | Domain exceptions and consistent HTTP error responses. |
| `repository` | Spring Data interfaces for database access. |
| `security` | JWT creation/validation, request authentication, and authorization components. |
| `service` | Business use cases and coordination between repositories and external services. |
| `util` | Small reusable stateless helpers only; business rules belong in services. |

Dependency direction is controller → service → repository. Services may use DTO/entity mappers and external-provider clients. Repositories must not be called directly by controllers. DTOs are the API contract; entities are the persistence contract.

## Planned data relationships

These relationships guide future entity work; the entities and schema will be introduced in their designated phases:

```text
User 1 ─── * Roadmap
Roadmap 1 ─── * RoadmapTask
Roadmap 1 ─── * SkillGap
Roadmap 1 ─── * CertificationRecommendation
Roadmap 1 ─── * ProjectRecommendation
```

Each roadmap belongs to one user. Roadmap-owned items belong to one roadmap, so they can be loaded, authorized, and deleted consistently with the roadmap. User profile fields are associated with the authenticated user; their exact persistence shape will be decided in the profile phase. Relationship ownership, cascade behavior, indexes, and constraints will be encoded in the JPA mappings and database setup rather than implied by this diagram.

## Request and security boundaries

* Controllers accept request DTOs and apply Bean Validation constraints.
* Services enforce application rules and ensure a user can only access their own profile and roadmap data.
* Repositories scope persistence operations to the owning user when needed.
* Authentication endpoints are public; protected API routes require a valid bearer token once JWT is introduced.
* Configuration and secrets are supplied through environment variables in deployment; they are not committed to the repository.
* AI output is treated as untrusted input: it must be parsed, validated, and normalized before persistence or return to the client.

## Frontend layout (Phase 12)

The planned Next.js application will use the App Router and TypeScript. Its initial boundaries are:

```text
frontend/
├── src/app/                 # Routes, layouts, and page composition
├── src/components/          # Shared UI and feature components
├── src/lib/                 # Axios client and shared client utilities
├── src/features/            # Feature-level API calls, schemas, and hooks
└── src/types/               # Shared frontend API types
```

Forms will use React Hook Form and schema validation, and shared UI primitives will follow the Shadcn UI conventions. Authentication token storage and refresh behavior must be selected and implemented as part of the frontend authentication phase; this diagram does not prescribe insecure browser storage.

## Current scaffold versus planned components

The repository currently contains a small Spring Boot application, a health route, and an initial registration route. The package tree above describes the target organization, not a claim that all feature packages or flows are already implemented. The frontend is intentionally empty until Phase 12. Each subsequent phase will introduce its own code and API contract without preemptively implementing later phases.
