# CLAUDE.md - Project Rules for Claude Code

Read this file at the start of every session. Follow every rule here without exception.
Never deviate from these rules unless the user explicitly updates this file.

---

## Project Overview

Second Brain is an Android app for capturing, organizing, and searching text, images,
and links. It uses offline OCR via Google ML Kit for image text extraction.
Features 1 (Delete and Edit) and 2 (Tags) are complete.
Feature 3 (RAG Chat Agent) is currently being built.

---

## Architecture Rules

- MVVM pattern with Repository abstraction. Do not deviate.
- All database access goes through `DatabaseManager` only. No direct DAO calls from UI.
- No business logic in `MainActivity`. UI logic only.
- ViewModels are not currently used. Do not introduce them unless the user asks.
- Single-activity architecture. All UI is in `MainActivity` using Jetpack Compose.
- Repository classes handle all data operations for their respective content types.

---

## Code Style

- Kotlin only. No Java.
- Jetpack Compose for all UI. No XML layouts.
- Coroutines and Flow for all async work. No callbacks, no RxJava.
- Use `lifecycleScope` for coroutine launching from the activity.
- Use `IO` dispatcher for all database and network operations.
- Use `Main` dispatcher for UI updates only.
- Follow existing naming conventions in the codebase.

---

## Database Rules

- Room only. No raw SQLite outside DAO files.
- Every schema change requires a Room migration. Never use `fallbackToDestructiveMigration`.
- Migration version must increment by 1 from the current version.
- Every new entity or column must have a corresponding DAO method.
- Junction tables use composite primary keys, not auto-generated IDs.
- Current database version is 2. Feature 3 migration targets version 3.

---

## Dependency Rules

- Do not add new Gradle dependencies without explicitly asking the user first.
- Prefer libraries already in the project before introducing new ones.
- Current libraries in use: Room, Jetpack Compose, Google ML Kit 16.0.1, Coil, Jsoup,
  Gson, Material 3, Kotlin Coroutines, Kotlin Flow, androidx.security:security-crypto.
- security-crypto is already approved and added. Do not ask again.
- For Feature 3, one new dependency is pre-approved: nothing extra needed.
  HttpURLConnection handles all API calls. Gson handles all JSON. No new libs required.

---

## API Rules

- All external API calls go through OpenRouter only.
- OpenRouter base URL: https://openrouter.ai/api/v1
- Chat model: anthropic/claude-haiku-4-5
- Embedding model: openai/text-embedding-3-small
- Auth header: Authorization: Bearer {key}
- Required header on every request: HTTP-Referer: second-brain-android
- Never hardcode the API key. Store in EncryptedSharedPreferences via SettingsManager.
- Never log the API key, even partially.

---

## Security Rules

- Never hardcode API keys in source files.
- Store the OpenRouter API key in EncryptedSharedPreferences only.
- Never log API keys, even partially.
- Never expose API keys in UI components.

---

## Testing Rules

- Every new DAO method requires a unit test.
- Every new Repository method requires a unit test.
- Use existing test patterns in the project.

---

## Scope Rules

- Implement only the task specified. Do not touch unrelated files.
- Do not refactor existing code unless it is directly in the path of the task.
- Do not rename existing classes, methods, or variables.
- Always state which files you are modifying before making changes.

---

## What NOT to Do

- Do not introduce ViewModels unless instructed.
- Do not convert XML layouts (there are none, keep it that way).
- Do not switch Room to any other database library.
- Do not replace Coroutines with any other async pattern.
- Do not add any analytics or crash reporting library.
- Do not add OkHttp, Retrofit, or any HTTP library. Use HttpURLConnection only.
- Do not modify `CLAUDE.md`, `REQUIREMENTS.md`, `TECHNICAL_DESIGN.md`, or `TASKS.md`
  unless the user explicitly asks.
