# SkillNova AI

AI-powered career roadmap generator and skill gap analysis system.

SkillNova AI helps students, freshers, and professionals turn a target role into a personalized learning plan, then track progress against it.

## Repository layout

```text
skillnova-ai/
├── backend/                 # Java 17, Spring Boot, Gradle REST API
│   ├── gradlew              # Gradle wrapper (macOS/Linux)
│   ├── gradlew.bat          # Gradle wrapper (Windows)
│   └── src/
│       ├── main/java/com/skillnova/backend/
│       │   ├── config/      # Application and framework configuration
│       │   ├── controller/  # HTTP endpoints and transport concerns
│       │   ├── dto/         # Validated API request/response models
│       │   ├── entity/      # JPA persistence models
│       │   ├── exception/   # API errors and centralized error handling
│       │   ├── repository/  # Spring Data persistence interfaces
│       │   ├── security/    # Authentication filters and JWT components
│       │   ├── service/     # Business rules and orchestration
│       │   └── util/        # Small shared, stateless utilities
│       └── main/resources/  # Externalized application configuration
├── frontend/                # Next.js and TypeScript application (Phase 12)
├── docs/                    # Architecture and implementation notes
└── README.md
```

The detailed component responsibilities and planned entity relationships are in [`docs/architecture.md`](docs/architecture.md). The repository is being implemented incrementally; the frontend and later API features are intentionally not scaffolded ahead of their phases.

## Current backend scaffold

The backend uses Java 17, Spring Boot 3.3.5, Spring MVC, Spring Security, Bean Validation, Spring Data JPA, Flyway, Lombok, and the PostgreSQL JDBC driver. Dependencies are declared in `backend/build.gradle`.

## Database setup (Phase 2)

Schema is version-controlled with Flyway migrations in `backend/src/main/resources/db/migration`. Hibernate never auto-generates DDL (`ddl-auto=validate`); Flyway is the single source of truth for schema changes.

Local development uses a Docker Postgres container on host port **5433** (not 5432) to avoid clashing with any Postgres already installed on the machine:

```bash
cp .env.example .env   # adjust DB_USERNAME/DB_PASSWORD/DB_HOST_PORT if needed
docker-compose up -d   # starts Postgres on localhost:5433

cd backend
./gradlew bootRun      # active profile defaults to "dev"
```

On startup, Flyway creates `flyway_schema_history` and applies `V1__create_core_schema.sql`, creating `users`, `roadmaps`, `roadmap_tasks`, `skill_gaps`, `certification_recommendations`, and `project_recommendations` with foreign keys, CHECK constraints, and indexes.

Profiles:
* `dev` (default) — connects to the local Docker Postgres.
* `prod` — connects to Neon PostgreSQL using `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` environment variables (set `SPRING_PROFILES_ACTIVE=prod` on Render).

To verify the schema manually:

```bash
docker exec -it skillnova-postgres psql -U skillnova -d skillnova -c "\dt"
```

To compile and run the existing backend from the repository root:

```bash
cd backend
./gradlew test
./gradlew bootRun
```

The current health endpoint is `GET http://localhost:8080/api/health`. Registration and authentication behavior will be implemented in their respective phases.

## Implementation sequence

1. Project structure
2. Database setup
3. User entity
4. Registration API
5. Login API
6. JWT authentication
7. Profile management
8. AI integration
9. Roadmap persistence
10. Dashboard APIs
11. Progress tracking
12. Frontend setup
13. Frontend authentication
14. Dashboard UI
15. Roadmap UI
16. Deployment
17. GitHub Actions CI/CD
