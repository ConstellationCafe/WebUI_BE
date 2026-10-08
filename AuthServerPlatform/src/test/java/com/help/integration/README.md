# Integration tests

This directory contains tests that exercise Spring MVC/security configuration, JPA repositories against H2, or interactions across application components. Test data and external dependencies must remain local to the test.

Run all backend tests from `AuthServerPlatform` with:

```bash
./gradlew test
```

The Java package declarations remain aligned with the production package under test. The `integration` directory classifies source files; it does not change runtime packages.
