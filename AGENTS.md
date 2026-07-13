# Code Review Instructions

- All unit tests must extend  `io.antmedia.test.enterprise.UnitTestBase` and be in the same
  Java package as the class under test
- Unit tests must not use stdout and stderr for logging, they must use `logger` from `UnitTestBase`
- Tests that do not rely on external services, just the JVM and `@Testcontainers` should be tagged with `@Tag("fast")`
- Use AssertJ for fluent assertions, Awaitility library for timing related / async testing
- Use Lombok to avoid boilerplate getters and setters, suggest using Java records where applicable