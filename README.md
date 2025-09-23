# Playwright Java E2E Template

Minimal, reusable foundation for UI tests based on:
- Playwright Java + JUnit 5
- ExtentReports HTML reporting
- AspectJ-powered `@Step` logging
 - Parallel execution
 - AssertJ fluent assertions

## Quick Start

1) Install browsers 
```
./gradlew playwrightInstall
```

2) Run tests (local env)
```
./gradlew test
```

3) Switch environment
```
./gradlew test -Denv=ci
```

Reports, videos and traces appear under `build/reports`.

Examples:
- `./gradlew test -Dbrowser=firefox -Dheadless=true`
- `./gradlew test -Denv=ci -Dslowmo=0`

## Docker

Build image (contains browsers + JDK):
```
docker build -t pw-java .
```

Run tests inside Docker (override Gradle args as needed):
```
docker run --rm -v "$PWD":/app -w /app pw-java ./gradlew test -Denv=ci -Dbrowser=chromium -Dheadless=true
```

Notes:
- Image is based on Playwright’s official Java image with browsers preinstalled.
- By default, container runs `./gradlew --no-daemon test` if no command is provided.

## Structure

- `src/main/java/configuration/` – config loader, Playwright container, reporting
- `src/main/java/pages/` – page objects
- `src/test/java/tests/` – JUnit tests with step logging
- `src/main/resources/envs/` – environment property files

## Notes

- `env` is selected by `-Denv=<name>`; defaults to `local`.
- `BASE_URL` is required in the env file.
- Sample test targets `https://example.com/` and should pass anywhere.

Supported system properties:
- `-Dbrowser=chromium|firefox|webkit`
- `-Dheadless=true|false`
- `-Dslowmo=<ms>`
- `-Dbase_url=https://your-host/`

## CI

GitHub Actions workflow `.github/workflows/ci.yml` builds the Docker image and runs tests in a matrix across all browsers, then uploads `build/reports/**` as artifacts.
