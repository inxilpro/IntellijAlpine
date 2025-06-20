# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Code Guidelines

- Always use modern idiomatic Kotlin code
- When implementing singletons, prefer `Foo.instance` over `Foo.Companion.instance` or `Foo.getInstance()`

## Commands

### Building & Running
- `./gradlew build` - Build the plugin
- `./gradlew buildPlugin` - Assemble plugin ZIP for deployment
- `./gradlew runIde` - Run IntelliJ IDEA with the plugin installed for testing
- `./gradlew runIdeForUiTests` - Run IDE with robot-server for UI testing

### Testing & Verification
- `./gradlew test` - Run unit tests
- `./gradlew check` - Run all checks (tests + verification)
- `./gradlew verifyPlugin` - Validate plugin structure and descriptors
- `./gradlew runPluginVerifier` - Check binary compatibility with target IDEs
- `./gradlew runInspections` - Run Qodana code inspections
- `./gradlew koverReport` - Generate code coverage reports

## Architecture

This is an IntelliJ IDEA plugin that adds Alpine.js support. The plugin provides:

- Auto-completion for Alpine directives (x-data, x-show, x-model, etc.)
- JavaScript language injection in Alpine attributes
- Syntax highlighting within Alpine directives
- Plugin support for third-party alpine plugins

### Plugin Configuration

The plugin is configured via:

- `plugin.xml` - Main plugin manifest defining extensions and dependencies
- `gradle.properties` - Version and platform configuration
- `build.gradle.kts` - Build configuration and dependencies

The plugin requires:

- IntelliJ IDEA 2025.1 or newer
- JavaScript and HtmlTools plugins as dependencies
- Java 21 runtime

### Release Process

1. Update version in `gradle.properties`
2. Update `CHANGELOG.md` Unreleased section with version number
3. Push changes to main branch
4. Create and publish a GitHub release - this triggers automatic publishing to JetBrains Marketplace