# Playwright Java E2E Template

Minimal, reusable foundation for UI tests based on:
- Playwright Java + JUnit 5
- ExtentReports HTML reporting
- AspectJ-powered `@Step` logging
- Parallel execution

## Quick Start

1) Install browsers
```
./gradlew -p template playwrightInstall
```

2) Run tests (local env)
```
./gradlew -p template test
```

3) Switch environment
```
./gradlew -p template test -Denv=ci
```

Reports, videos and traces appear under `template/build/reports`.

## Structure

- `configuration/` – config loader, Playwright container, reporting
- `pages/` – page objects
- `tests/` – JUnit tests with step logging
- `resources/envs/` – environment property files

## Notes

- `env` is selected by `-Denv=<name>`; defaults to `local`.
- `BASE_URL` is required in the env file.
- Sample test targets `https://example.com/` and should pass anywhere.

