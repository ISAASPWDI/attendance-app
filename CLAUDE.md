# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Spring Boot 3.5.8 REST API (Java 21) backend for a school attendance-tracking system. Frontend is a separate Angular app (not in this repo). Detailed endpoint/entity/config documentation lives in `CONFIG.md` (Spanish) — read it for the full API contract, request/response shapes, and DB-maintenance workflow before making API changes.

## Commands

Build requires Java 21. If `JAVA_HOME` isn't already pointed at a JDK 21, set it explicitly, e.g.:

```bash
JAVA_HOME=/home/stevens/.sdkman/candidates/java/21.0.3-tem ./mvnw compile
```

- Compile: `./mvnw compile`
- Run: `./mvnw spring-boot:run`
- Run all tests: `./mvnw test`
- Run a single test: `./mvnw test -Dtest=AttendanceApplicationTests`
- Package: `./mvnw package`

There is currently only one placeholder test (`AttendanceApplicationTests`, a context-load smoke test) — no meaningful test coverage exists yet.

### Local setup

- Copy `application.example.properties` guidance: create `.env.properties` in the project root (gitignored) with `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, and `CLOUDINARY_CLOUD_NAME`/`CLOUDINARY_API_KEY`/`CLOUDINARY_API_SECRET`. `application.properties` imports it via `spring.config.import=optional:file:.env.properties`.
- `docker-compose.yml` runs Postgres 17 (host port `5435` → container `5432`) and the backend; both read `.env.properties`.
- `spring.jpa.hibernate.ddl-auto=update` — there are no Flyway/Liquibase migrations. Schema changes are driven purely by entity edits, and Hibernate reshapes the table on next startup.

## Architecture

Standard layered MVC, but each layer's package is split into domain sub-packages (`attendances`, `auth`, `users`, `dashboard`, `reports`) rather than one flat package per layer:

```
config/         SecurityConfig, JwtFilter, CustomUserDetails
controller/{domain}/
dto/{domain}/
entity/
exception/{domain}/   per-domain @RestControllerAdvice + exception classes
repository/
service/{domain}/
specification/        JPA Specification builders for dynamic filtering
```

- **Two roles**: `TEACHER`, `DIRECTOR`, stored on `User.role` as a plain enum **without** a `ROLE_` prefix — authorization checks must use `hasAuthority("DIRECTOR")`, not `hasRole(...)` (which expects the prefix).
- **Auth**: stateless JWT via `JwtFilter` (runs before `UsernamePasswordAuthenticationFilter`), access token expires in 1 hour, refresh token in 7 days. Only `/api/auth/login` and `/api/auth/register` are `permitAll()`; everything else requires authentication, with `@PreAuthorize("hasAuthority('DIRECTOR')")` on director-only endpoints.
- **JWT secret caveat**: `JWTService`'s constructor generates a fresh random `HmacSHA256` key on every instantiation and overwrites the static `secretKey` field, effectively ignoring the `jwt.secret` property. This means all outstanding tokens are invalidated on every app restart, and horizontally scaled instances won't validate each other's tokens. Be aware of this if debugging "invalid token" issues or working on JWT behavior.
- **Exception handling**: multiple `@RestControllerAdvice` classes ordered via `@Order` — domain-specific handlers (e.g. `RecordExceptionHandler` at `@Order(2)`) take priority over the catch-all `GlobalHandleException` at `@Order(99)`. Follow this pattern (dedicated exception classes + a per-domain handler) when adding new failure cases rather than adding to the global handler.
- **DTO pattern**: separate DTOs per direction/audience rather than one shared object, e.g. `AttendanceRecordDTO` (write/input), `AttendanceRecordResponseDTO` (teacher's own view), `AttendanceRecordWithUserDTO` (director's list view, includes teacher name). Mapping is done manually in services — no MapStruct/ModelMapper.
- **Dynamic filtering**: `AttendanceFilter`/`UserFilter` DTOs bind directly from request query params and are turned into JPA `Specification`s (`AttendanceSpecification`, `UserSpecification`) rather than derived query methods — extend these when adding new filter/sort fields.
- **Attendance status logic**: `Present` if `timeIn` ≤ 07:30, else `Late`; `Absent` is inferred (no record for the day), never stored as a status for a "checked in" record. `AttendanceRecord.setTimeIn`/`setTimeOut` self-validate ordering and throw domain exceptions if `timeIn`/`timeOut` are inconsistent.
- **Reports**: `ReportService` generates Excel (Apache POI) and PDF (OpenPDF, package `com.lowagie.text`) on the fly from the same `AttendanceSpecification` filter used by the list endpoint — no caching/pregeneration. Reports embed the user's photo, signature, and fingerprint as real images (downloaded from Cloudinary and cached per-URL for the duration of one report), sorted newest-first by default.
- **Image hosting**: photo/signature/fingerprint images go to Cloudinary (`CloudinaryService`), only the resulting URL is persisted on `User`. Upload endpoints enforce owner-or-director access in the controller (`isOwnerOrDirector`), not via `@PreAuthorize`. **`signatureUrl`/`fingerprintUrl` are intentionally excluded from every platform-facing DTO** (`UserProfileDTO`, `UserDetailDTO`, `AttendanceRecordWithUserDTO`) — they only ever appear embedded in reports. `photoUrl` is the only one of the three exposed to the frontend.
- **DB size constraint**: the target Postgres free tier caps at 512 MB. The intended operational flow (see `CONFIG.md`) is: download the Excel/PDF report for a date range, then `DELETE /api/attendances/by-date/{date}` to purge those rows — do this before suggesting schema/data changes that would grow storage further.
- **Known schema drift**: on some existing dev databases, `attendance_record.time_out` may still carry a stale `NOT NULL` constraint from an earlier schema iteration (like the `sign_field` legacy column) even though the entity has always mapped it as nullable — `ddl-auto=update` does not relax existing constraints. If `quick-checkin`/`create` fail with a Postgres not-null violation on `time_out`, run `ALTER TABLE attendance_record ALTER COLUMN time_out DROP NOT NULL;` directly against the dev DB.
- `src/main/java/com/attendance/demo/prueba.java` is a stray scratch file (has its own `main`, unrelated to the app) — not part of the real application; don't treat it as a reference for conventions.
