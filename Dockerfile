# Playwright + Browsers preinstalled + Java
# Use Playwright's official image with system deps and browsers, plus Java.

ARG PLAYWRIGHT_VERSION=1.54.0
FROM mcr.microsoft.com/playwright/java:v${PLAYWRIGHT_VERSION}-jammy

ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright \
    PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1 \
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

# Browsers are preinstalled in the base image under /ms-playwright; no extra install needed.

# Default command runs tests; pass extra args after image name to override
CMD ["./gradlew", "--no-daemon", "test"]
