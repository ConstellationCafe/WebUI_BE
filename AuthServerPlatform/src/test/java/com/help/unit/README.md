# Unit tests

This directory contains tests that isolate one class or boundary with mocks, fixtures, or in-memory validation. External HTTP and Redis calls are replaced with test doubles.

Run all backend tests from `AuthServerPlatform` with:

```bash
./gradlew test
```

The Java package declarations remain aligned with the production package under test. The `unit` directory classifies source files; it does not change runtime packages.
