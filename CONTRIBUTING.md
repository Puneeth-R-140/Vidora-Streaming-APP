# Contributing to Pocket Nexus

Thank you for your interest in contributing to this project. This document provides guidelines for contributing.

## Code of Conduct

- Be respectful and inclusive
- Focus on constructive feedback
- Help maintain a welcoming environment

## How to Contribute

### Reporting Bugs

1. Check if the bug has already been reported in the Issues section
2. If not, create a new issue with:
   - Clear title and description
   - Steps to reproduce the issue
   - Expected behavior vs actual behavior
   - Screenshots if applicable
   - Device information and Android version

### Suggesting Features

1. Check existing Issues for similar feature requests
2. Create a new issue describing:
   - Clear description of the feature
   - Use cases and benefits
   - Possible implementation approach

### Pull Requests

1. Fork the repository
2. Create a branch for your feature:
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. Make your changes following the code style guidelines
4. Test thoroughly on multiple devices
5. Commit with clear messages:
   ```bash
   git commit -m "Add: feature description"
   ```
6. Push to your fork:
   ```bash
   git push origin feature/your-feature-name
   ```
7. Create a Pull Request with:
   - Description of changes
   - Related issue number if applicable
   - Screenshots or videos of UI changes

## Code Style

### Kotlin
- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Add comments for complex logic
- Keep functions small and focused

### Compose
- Use `remember` and `LaunchedEffect` appropriately
- Avoid side effects in composables
- Extract reusable components
- Follow Material 3 guidelines

### Commits
- Use present tense ("Add feature" not "Added feature")
- Keep commits focused and atomic
- Reference issues when applicable

## Development Setup

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle dependencies
4. Run on emulator or physical device

## Testing

- Test on multiple Android versions (API 24 and above)
- Test on different screen sizes
- Verify dark mode compatibility
- Check for memory leaks

## Questions

Feel free to open an issue if you have any questions or need clarification on anything.

---

Thank you for contributing to Pocket Nexus
