# Contributing to NASA Mirror API

Thank you for your interest in contributing! This document provides guidelines for contributing to the project.

## Code of Conduct

### Our Pledge
We are committed to providing a welcoming and inclusive environment for all contributors.

### Standards
- Be respectful and considerate
- Focus on constructive feedback
- Accept differing viewpoints
- Show empathy toward others

## How to Contribute

### Reporting Bugs

Before creating a bug report:
1. Check existing issues to avoid duplicates
2. Gather relevant information (logs, error messages, etc.)

When creating a bug report, include:
- **Title**: Clear, descriptive summary
- **Description**: Detailed description of the issue
- **Steps to Reproduce**: Numbered steps to reproduce
- **Expected Behavior**: What should happen
- **Actual Behavior**: What actually happens
- **Environment**: OS, Rust version, wrangler version
- **Logs**: Relevant log output
- **Screenshots**: If applicable

### Suggesting Features

Feature requests are welcome! Please include:
- **Use Case**: Why is this feature needed?
- **Description**: Detailed description of the feature
- **Examples**: Examples of how it would work
- **Alternatives**: Alternative solutions considered
- **Priority**: How important is this feature?

### Pull Requests

#### Before Starting
1. Check existing issues and PRs
2. For major changes, open an issue first to discuss
3. Fork the repository
4. Create a feature branch from `main`

#### Development Process

1. **Clone and Setup**
   ```bash
   git clone https://github.com/yourusername/nasa-mirror-api.git
   cd nasa-mirror-api
   git checkout -b feature/your-feature-name
   npm install
   cargo build
   ```

2. **Make Changes**
   - Write clear, idiomatic Rust code
   - Follow existing code style
   - Add tests for new functionality
   - Update documentation as needed

3. **Test Your Changes**
   ```bash
   # Run all tests
   cargo test
   
   # Format code
   cargo fmt
   
   # Lint
   cargo clippy
   
   # Security audit
   cargo audit
   
   # Test locally
   npm run dev
   ```

4. **Commit Changes**
   Use conventional commit messages:
   ```
   <type>(<scope>): <subject>
   
   <body>
   
   <footer>
   ```
   
   Types:
   - `feat`: New feature
   - `fix`: Bug fix
   - `docs`: Documentation changes
   - `style`: Code style changes (formatting)
   - `refactor`: Code refactoring
   - `test`: Test additions/changes
   - `chore`: Maintenance tasks
   
   Example:
   ```
   feat(parser): add support for video thumbnails
   
   - Extract YouTube video IDs
   - Generate thumbnail URLs
   - Update tests
   
   Closes #123
   ```

5. **Push and Create PR**
   ```bash
   git push origin feature/your-feature-name
   ```
   
   Then create a PR on GitHub with:
   - Clear title and description
   - Reference related issues
   - Screenshots/examples if applicable
   - Checklist of what was done

#### PR Checklist

Before submitting:
- [ ] Code builds without errors
- [ ] All tests pass
- [ ] New tests added for new functionality
- [ ] Code formatted with `cargo fmt`
- [ ] Lints pass with `cargo clippy`
- [ ] Security audit clean with `cargo audit`
- [ ] Documentation updated (README, code comments, etc.)
- [ ] CHANGELOG.md updated
- [ ] Commit messages follow convention
- [ ] No merge conflicts with main

## Code Style

### Rust Style Guide

Follow the [Rust Style Guide](https://doc.rust-lang.org/nightly/style-guide/):

- Use `cargo fmt` for automatic formatting
- Use `cargo clippy` for linting
- Prefer explicit types for public APIs
- Use meaningful variable names
- Add doc comments for public items
- Keep functions focused and small

### Documentation

- **Public APIs**: Must have doc comments with examples
  ```rust
  /// Fetch the latest APOD entry
  ///
  /// # Examples
  ///
  /// ```
  /// let entry = ApodScraper::fetch_latest(&env).await?;
  /// ```
  ///
  /// # Errors
  ///
  /// Returns an error if the source is unreachable
  pub async fn fetch_latest(env: &Env) -> Result<ApodEntry> {
      // ...
  }
  ```

- **Private functions**: Brief comment explaining purpose
- **Complex logic**: Inline comments for clarity
- **TODOs**: Use `// TODO:` format with description

### Testing

- Write unit tests for all business logic
- Test edge cases and error conditions
- Use descriptive test names
  ```rust
  #[test]
  fn test_parse_month_handles_abbreviated_names() {
      assert_eq!(ApodParser::parse_month("Jan").unwrap(), 1);
  }
  ```

### Error Handling

- Use `Result<T>` for fallible operations
- Provide context in error messages
- Don't panic in production code
- Use `?` operator for error propagation

## Project Structure

```
src/
├── lib.rs          # Main entry, router
├── models.rs       # Data structures
├── parser.rs       # HTML parsing
├── scraper.rs      # Fetching/caching
├── handlers.rs     # Request handlers
└── security.rs     # Security layer
```

When adding new functionality:
- **New data type**: Add to `models.rs`
- **New route**: Add handler to `handlers.rs`, register in `lib.rs`
- **New parsing logic**: Add to `parser.rs` with tests
- **New security feature**: Add to `security.rs`

## Testing Guidelines

### Unit Tests

Place tests in the same file as the code:
```rust
#[cfg(test)]
mod tests {
    use super::*;
    
    #[test]
    fn test_something() {
        // Arrange
        let input = "test";
        
        // Act
        let result = function_under_test(input);
        
        // Assert
        assert_eq!(result, expected);
    }
}
```

### Integration Tests

For Worker testing, use `wrangler dev` and manual testing.

### Test Coverage

Aim for:
- Core parsing logic: 80%+ coverage
- Security functions: 90%+ coverage
- Handler functions: Integration testing sufficient

## Documentation Guidelines

### README Updates

Update README.md when:
- Adding new features
- Changing API behavior
- Modifying configuration
- Updating dependencies

### Code Documentation

- Public items: Full doc comments
- Complex algorithms: Detailed comments
- Configuration: Document expected values
- Examples: Show typical usage

## Release Process

(For maintainers)

1. **Update Version**
   - Update `Cargo.toml` version
   - Update `CHANGELOG.md`
   - Update README.md if needed

2. **Create Tag**
   ```bash
   git tag -a v0.2.0 -m "Release v0.2.0"
   git push origin v0.2.0
   ```

3. **Deploy**
   ```bash
   wrangler deploy --env production
   ```

4. **Announce**
   - GitHub Release with changelog
   - Update documentation
   - Notify users

## Getting Help

- **Questions**: Open a GitHub Discussion
- **Bugs**: Open a GitHub Issue
- **Security**: See SECURITY.md
- **Chat**: (Add Discord/Slack if available)

## Recognition

Contributors will be:
- Listed in the GitHub contributors page
- Mentioned in release notes (for significant contributions)
- Added to CONTRIBUTORS.md (if desired)

## License

By contributing, you agree that your contributions will be licensed under the MIT License.

---

Thank you for contributing to NASA Mirror API! 🚀
