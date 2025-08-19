# Contributing to Xorcery

Thank you for your interest in contributing to Xorcery! We welcome contributions from the community and are pleased to have you join us.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [How to Contribute](#how-to-contribute)
- [Pull Request Process](#pull-request-process)
- [Coding Standards](#coding-standards)
- [Testing Guidelines](#testing-guidelines)
- [Documentation](#documentation)
- [Issue Reporting](#issue-reporting)
- [Security Vulnerabilities](#security-vulnerabilities)
- [Community](#community)

## Code of Conduct

This project adheres to the [Contributor Covenant Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code. Please report unacceptable behavior to [project-email@example.com].

## Getting Started

### Prerequisites

Before you begin, ensure you have the following installed:

- **Java 21** or higher (we recommend using [SDKMAN!](https://sdkman.io/) for Java version management)
- **Maven 3.6+** or **Gradle 6.0+**
- **Git**
- **IDE** of your choice (we recommend IntelliJ IDEA)

### Verify Your Setup

```bash
java -version
javac -version
mvn -version
git --version
```

## Development Setup

### 1. Fork the Repository

Click the "Fork" button at the top right of the GitHub page to create your own copy of the repository.

### 2. Clone Your Fork

```bash
git clone https://github.com/Cantara/xorcery.git
cd xorcery
```

### 3. Add Upstream Remote

```bash
git remote add upstream https://github.com/Cantara/xorcery.git
git remote -v  # Verify remotes are set up correctly
```

### 4. Install Dependencies

```bash
mvn clean install
```

### 5. Verify Everything Works

```bash
# Build all modules and run tests
mvn install
```

## How to Contribute

### Types of Contributions

We welcome several types of contributions:

- 🐛 **Bug Reports**: Help us identify and fix issues
- 💡 **Feature Requests**: Suggest new functionality
- 📝 **Documentation**: Improve or add documentation
- 🧪 **Tests**: Add or improve test coverage
- 🔧 **Bug Fixes**: Fix identified issues
- ✨ **New Features**: Implement new functionality
- 🎨 **Code Quality**: Refactoring, performance improvements

### Before You Start

1. **Check existing issues** to avoid duplicating work
2. **Create an issue** for significant changes to discuss the approach
3. **Keep changes focused** - one feature or fix per pull request
4. **Follow our coding standards** (see below)

### Workflow

1. **Create a branch** for your work:
   ```bash
   git checkout -b feature/your-feature-name
   # or
   git checkout -b bugfix/issue-number-description
   ```

2. **Make your changes** following our coding standards

3. **Write tests** for your changes

4. **Test your changes**:
   ```bash
   mvn test
   mvn verify  # Run integration tests
   ```

5. **Update documentation** if needed

6. **Commit your changes**:
   ```bash
   git add .
   git commit -m "feat: add new feature description"
   ```

7. **Push to your fork**:
   ```bash
   git push origin feature/your-feature-name
   ```

8. **Create a Pull Request**

## Pull Request Process

### Before Submitting

- [ ] Code follows our style guidelines
- [ ] Tests pass locally
- [ ] New tests added for new functionality
- [ ] Documentation updated if needed
- [ ] Commit messages follow our convention
- [ ] No merge conflicts with main branch

### PR Template

When creating a pull request, please include:

```markdown
## Description
Brief description of changes made.

## Type of Change
- [ ] Bug fix (non-breaking change which fixes an issue)
- [ ] New feature (non-breaking change which adds functionality)
- [ ] Breaking change (fix or feature that would cause existing functionality to not work as expected)
- [ ] Documentation update

## Testing
Describe how you tested your changes:
- [ ] Unit tests added/updated
- [ ] Integration tests added/updated
- [ ] Manual testing performed

## Checklist
- [ ] My code follows the style guidelines
- [ ] I have performed a self-review of my code
- [ ] I have commented my code, particularly in hard-to-understand areas
- [ ] I have made corresponding changes to the documentation
- [ ] My changes generate no new warnings
- [ ] I have added tests that prove my fix is effective or that my feature works
- [ ] New and existing unit tests pass locally with my changes

## Related Issues
Fixes #(issue number)
```

### Review Process

1. **Automated checks** must pass (CI/CD pipeline)
2. **Code review** by at least one maintainer
3. **Testing** in different environments if applicable
4. **Documentation review** if docs were changed
5. **Final approval** and merge by maintainer

## Coding Standards

### Java Style Guide

We follow the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html) with some modifications:

#### Code Formatting

```java
// Use 4 spaces for indentation (not tabs)
public class ExampleClass {
    private String fieldName;
    
    public void methodName(String parameter) {
        if (condition) {
            doSomething();
        }
    }
}
```

#### Naming Conventions

- **Classes**: `PascalCase` (e.g., `DataProcessor`)
- **Methods/Variables**: `camelCase` (e.g., `processData`)
- **Constants**: `UPPER_SNAKE_CASE` (e.g., `MAX_RETRY_COUNT`)
- **Packages**: `lowercase` (e.g., `com.example.projectname`)

#### Documentation

```java
/**
 * Processes the given data according to specified rules.
 *
 * @param inputData the data to be processed, must not be null
 * @param options processing options, can be null for defaults
 * @return processed data result
 * @throws IllegalArgumentException if inputData is null
 * @throws ProcessingException if processing fails
 */
public ProcessResult processData(String inputData, ProcessingOptions options) {
    // Implementation
}
```

### Best Practices

- **Write self-documenting code** with clear variable and method names
- **Keep methods small** and focused on a single responsibility
- **Use meaningful comments** for complex business logic
- **Handle exceptions appropriately** - don't swallow them
- **Follow SOLID principles**
- **Prefer composition over inheritance**
- **Use immutable objects** where possible

## Testing Guidelines

### Test Structure

We follow the **Arrange-Act-Assert** pattern:

```java
@Test
public void shouldReturnFormattedStringWhenValidInput() {
    // Arrange
    String input = "test input";
    DataFormatter formatter = new DataFormatter();
    
    // Act
    String result = formatter.format(input);
    
    // Assert
    assertEquals("Test Input", result);
}
```

### Test Categories

#### Unit Tests
- Test individual methods/classes in isolation
- Use mocking for dependencies
- Fast execution (< 1 second per test)

```java
@ExtendWith(MockitoExtension.class)
class DataProcessorTest {
    
    @Mock
    private DataRepository repository;
    
    @InjectMocks
    private DataProcessor processor;
    
    @Test
    void shouldProcessDataSuccessfully() {
        // Test implementation
    }
}
```

### Test Naming

Use descriptive test names that explain the scenario:

```java
// Good
@Test
void shouldThrowExceptionWhenInputIsNull() { }

@Test
void shouldReturnEmptyListWhenNoDataFound() { }

// Avoid
@Test
void testMethod1() { }

@Test
void testNullInput() { }
```

## Documentation

### Code Documentation

- **Public APIs** must have comprehensive JavaDoc
- **Complex algorithms** should have inline comments
- **Configuration options** must be documented
- **Examples** should be provided for non-trivial usage

### README Updates

When adding new features, update the README.md with:
- Usage examples
- Configuration options
- API changes
- Migration notes (for breaking changes)

### Wiki Pages

For substantial features, consider adding wiki pages covering:
- Detailed usage scenarios
- Architecture decisions
- Troubleshooting guides
- FAQ entries

## Issue Reporting

### Bug Reports

Use the bug report template and include:

- **Java version** and operating system
- **Project version** where the bug occurs
- **Steps to reproduce** the issue
- **Expected behavior** vs actual behavior
- **Error messages** or stack traces
- **Minimal code example** that reproduces the issue

### Feature Requests

Use the feature request template and include:

- **Use case description** - why is this needed?
- **Proposed solution** - how should it work?
- **Alternatives considered** - what other approaches did you consider?
- **Additional context** - any relevant information

### Questions

For questions about usage:
1. Check existing documentation first
2. Search existing issues
3. Use GitHub Discussions for general questions
4. Create an issue only for documentation gaps

## Security Vulnerabilities

**Do not** report security vulnerabilities through public issues.

Instead, email us at [rickard@exoreaction.com] with:
- Description of the vulnerability
- Steps to reproduce
- Potential impact
- Suggested fixes (if any)

We will respond within 48 hours and work with you to address the issue.

## Community

### Communication Channels

- **GitHub Issues**: Bug reports and feature requests
- **GitHub Discussions**: Questions and general discussion
- **Email**: [rickard@exoreaction.com] for private inquiries

### Recognition

Contributors will be recognized in:
- Project README.md
- Release notes

### Becoming a Maintainer

Active contributors may be invited to become maintainers. Maintainers:
- Review and merge pull requests
- Triage issues
- Make technical decisions
- Mentor new contributors

## Development Tips

### IDE Setup

#### IntelliJ IDEA
1. Import the project as Maven project
2. Configure code style settings
3. Enable automatic formatting on save

#### Eclipse
1. Import as existing Maven project
2Configure code formatting preferences

### Useful Commands

```bash
# Keep your fork up to date
git fetch upstream
git checkout main
git merge upstream/main
git push origin main

# Clean rebuild
mvn clean install -U

# Run specific test
mvn test -Dtest=YourTestClass

# Skip tests (for quick builds)
mvn clean install -DskipTests

# Generate project documentation
mvn javadoc:javadoc
```

### Common Pitfalls

- Don't work on the main branch directly
- Don't include IDE-specific files in commits
- Don't make unrelated changes in a single PR
- Don't ignore failing tests
- Don't commit sensitive information (passwords, API keys)

---

Thank you for contributing to Xorcery! Your efforts help make this project better for everyone.

For questions about contributing, please reach out to [rickard@exoreaction.com] or create a discussion in our GitHub repository.
