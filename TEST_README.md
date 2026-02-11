# ShopKeeper Pro Test Suite

This document describes the comprehensive test suite implementation for the ShopKeeper Pro Android app.

## Test Coverage Goal

The project maintains a **minimum 80% test coverage** requirement, enforced through git pre-commit hooks.

## Test Structure

### 1. Unit Tests (Priority 1)
Located in `app/src/test/`

- **ViewModels**: Test business logic and state management
  - `LoginViewModelTest.kt` - Authentication logic, demo user creation
  - `DashboardViewModelTest.kt` - Dashboard data loading, calculations

- **Repositories**: Test data layer operations
  - `ItemRepositoryTest.kt` - Item CRUD operations, default items insertion

- **Utilities**: Test helper functions
  - `SalesCalculatorTest.kt` - Sales calculations with default quantity logic

### 2. Integration Tests (Priority 2)
Located in `app/src/androidTest/`

- **DAO Tests**: Test database operations with Room
  - `UserDaoTest.kt` - User CRUD operations
  - `SaleDaoTest.kt` - Sales queries, date filtering
  - `ItemDaoTest.kt` - Item search, active items filtering
  - `ExpenseDaoTest.kt` - Expense tracking and categorization

### 3. UI Tests (Priority 3)
Located in `app/src/androidTest/`

- **Fragment Tests**: Test UI interactions with Espresso
  - `LoginFragmentTest.kt` - Login flow, validation, navigation
  - `SalesFragmentTest.kt` - Sales entry, calculations, clearing

## Running Tests

### Local Development

```bash
# Run all unit tests (fast, no device needed)
./gradlew test

# Run instrumentation tests (requires emulator/device)
./gradlew connectedAndroidTest

# Run all tests with coverage report
./test.sh

# Run all tests including Android tests
./test.sh --with-android
```

### Test Reports

- **Coverage Report**: `app/build/reports/jacoco/test/html/index.html`
- **Unit Test Results**: `app/build/reports/tests/testDebugUnitTest/index.html`
- **Android Test Results**: `app/build/reports/androidTests/connected/index.html`

## Git Hooks

### Setup
Run the setup script to enable git hooks:
```bash
./setup-hooks.sh
```

### Pre-commit Hook
Automatically runs on every commit:
1. Executes all unit tests
2. Generates coverage report
3. Blocks commit if:
   - Any tests fail
   - Coverage is below 80%

To bypass temporarily (not recommended):
```bash
git commit --no-verify
```

## Test Conventions

### Naming Convention
Use descriptive test names with backticks:
```kotlin
@Test
fun `when user enters valid credentials then login succeeds`() { }
```

### Test Data
Use `TestData.kt` for creating consistent test objects:
```kotlin
val testUser = TestData.createTestUser()
val testItem = TestData.createTestItem()
```

### Mocking
- Use Mockito for external dependencies
- Use Room's in-memory database for DAO tests
- Use `TestCoroutineRule` for coroutine tests

## Key Testing Features

### Default Quantity Logic
The app implements smart default quantity:
- When price is entered but quantity is empty → defaults to 1
- Thoroughly tested in `SalesCalculatorTest.kt`

### Database Testing
- Uses in-memory database for fast, isolated tests
- Tests all CRUD operations
- Verifies date-based queries
- Tests Flow emissions for real-time updates

### UI Testing
- Tests user interactions
- Verifies navigation flows
- Validates error handling
- Tests calculation updates

## Coverage Distribution

Target coverage by component:
- ViewModels: 85%
- Repositories: 80%
- Utilities: 90%
- DAOs: 75%
- Overall: 80% (minimum)

## Continuous Integration

While no CI/CD is currently set up, the test suite is designed to be CI-ready:
- All tests can run headlessly
- JaCoCo generates machine-readable XML reports
- Gradle tasks provide clear success/failure status

## Troubleshooting

### Tests Not Running
- Ensure you have JDK installed
- Sync project with Gradle files
- Clean and rebuild: `./gradlew clean build`

### Coverage Not Generating
- Run `./gradlew clean test jacocoTestReport`
- Check for the XML report at `app/build/reports/jacoco/test/jacocoTestReport.xml`

### Instrumentation Tests Failing
- Ensure emulator/device is running
- Check that test APK is installed
- Verify device has enough storage

## Future Improvements

1. Add performance tests for database operations
2. Implement screenshot tests for UI consistency
3. Add integration tests for full user flows
4. Set up GitHub Actions for automated testing
5. Add mutation testing to verify test quality