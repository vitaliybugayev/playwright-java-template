# Playwright + Browsers preinstalled + Java
# Use Playwright's official image with system deps and browsers, plus Java.

ARG PLAYWRIGHT_VERSION=1.54.0
FROM mcr.microsoft.com/playwright/java:${PLAYWRIGHT_VERSION}-jammy

ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright \
    GRADLE_USER_HOME=/home/pwuser/.gradle

WORKDIR /app

# Copy Gradle wrapper and settings first to leverage Docker layer caching
COPY gradlew gradlew.bat settings.gradle build.gradle /app/
COPY gradle /app/gradle
RUN chmod +x /app/gradlew

# Pre-warm Gradle wrapper (optional)
RUN ./gradlew --no-daemon -v || true

# Copy sources
COPY src /app/src
COPY README.md /app/

# Ensure browsers are available for Java CLI context (already present in base, this is a no-op safety)
RUN ./gradlew --no-daemon playwrightInstall || true

# Default command runs tests; pass extra args after image name to override
CMD ["./gradlew", "--no-daemon", "test"]

