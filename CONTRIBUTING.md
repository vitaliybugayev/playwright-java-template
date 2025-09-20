# Contributing to Playwright Java E2E Template

Thank you for your interest in this Playwright Java testing template! 

## Using as Foundation

**If you want to use this template for your own project:**
1. **Fork this repository** to create your own copy
2. Clone your fork locally
3. Install browsers: `./gradlew playwrightInstall`
4. Customize for your needs and start building your tests

## Contributing to the Template

**If you want to improve the template itself for everyone:**

### Getting Started

1. Fork the repository
2. Clone your fork locally
3. Install browsers: `./gradlew playwrightInstall`
4. Run tests to ensure everything works: `./gradlew test`

## Development Guidelines

### Code Style
- Follow standard Java conventions
- Use meaningful variable and method names
- Keep page objects simple and focused
- Add `@Step` annotations for test reporting

### Testing
- Write tests that are stable and reliable
- Use AssertJ for fluent assertions
- Ensure tests can run in parallel
- Test against multiple browsers when possible

### Page Objects
- Keep page objects in `src/main/java/pages/`
- Use Playwright locators effectively
- Add wait strategies for dynamic content
- Group related actions into logical methods

## Making Changes

### Before You Start
1. Check existing issues to avoid duplicates
2. Create an issue to discuss major changes
3. Ensure your local environment is set up correctly

### Pull Request Process
1. Create a feature branch from `main`
2. Make your changes with clear commit messages
3. Run the full test suite: `./gradlew test`
4. Test in different environments if applicable
5. Update documentation if needed
6. Submit a pull request with a clear description

### Commit Messages
- Use clear, descriptive commit messages
- Start with a verb (Add, Fix, Update, Remove)
- Keep the first line under 50 characters
- Add details in the body if needed

Examples:
```
Add support for Safari browser testing
Fix flaky login test timeout issue
Update README with Docker instructions
```

## Testing Your Changes

### Local Testing
```bash
# Run all tests
./gradlew test

# Test specific browser
./gradlew test -Dbrowser=firefox

# Test in headless mode
./gradlew test -Dheadless=true

# Test different environment
./gradlew test -Denv=ci
```

### Docker Testing
```bash
# Build and test in Docker
docker build -t pw-java .
docker run --rm -v "$PWD":/app -w /app pw-java ./gradlew test -Dheadless=true
```

## Project Structure

When contributing, understand the project layout:
- `src/main/java/configuration/` – Core config and setup
- `src/main/java/pages/` – Page object models
- `src/test/java/tests/` – Test classes
- `src/main/resources/envs/` – Environment configurations

## What We're Looking For

### High Priority
- Additional browser support improvements
- Better error handling and reporting
- Performance optimizations
- More robust waiting strategies

### Medium Priority
- Additional page object examples
- Enhanced configuration options
- Better CI/CD pipeline features
- Documentation improvements

### Ideas Welcome
- Mobile testing support
- API testing integration
- Visual regression testing
- Test data management

## Questions or Issues?

- Create an issue for bugs or feature requests
- Use discussions for questions about usage
- Check existing issues before creating new ones

## Code of Conduct

- Be respectful and constructive
- Help newcomers learn and contribute
- Focus on improving the project for everyone
- Follow standard open source etiquette

Thank you for helping make this template better for the testing community! 🚀