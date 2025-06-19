# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

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

### Publishing
- `./gradlew patchChangelog` - Update changelog
- `./gradlew signPlugin` - Sign the plugin ZIP
- `./gradlew publishPlugin` - Publish to JetBrains Marketplace (requires token)

## Architecture

This is an IntelliJ IDEA plugin that adds Alpine.js support. The plugin provides:
- Auto-completion for Alpine directives (x-data, x-show, x-model, etc.)
- JavaScript language injection in Alpine attributes
- Syntax highlighting within Alpine directives

### Key Components

1. **AttributesProvider** - Central class that provides Alpine attribute descriptors to the IDE's HTML/XML support system. It defines which Alpine attributes are available and their properties.

2. **AlpineJavaScriptAttributeValueInjector** - Injects JavaScript language into Alpine attribute values, enabling proper syntax highlighting and code completion within attributes like `x-data` and `x-show`. Includes comprehensive type definitions for `$ajax` magic property.

3. **AlpineCompletionContributor** - Handles auto-completion logic for Alpine directives, providing context-aware suggestions based on cursor position.

4. **AlpineTargetReferenceContributor** - Provides native IntelliJ reference support for `x-target` attributes, enabling go-to-definition, find usages, refactoring, and error highlighting for ID references.

5. **AttributeInfo** - Contains all Alpine directive definitions and metadata, including documentation and allowed contexts for each directive. Now includes Alpine AJAX directives (`x-target`, `x-headers`, `x-merge`, `x-autofocus`, `x-sync`).

### Plugin Configuration

The plugin is configured via:
- `plugin.xml` - Main plugin manifest defining extensions and dependencies
- `gradle.properties` - Version and platform configuration
- `build.gradle.kts` - Build configuration and dependencies

The plugin requires:
- IntelliJ IDEA 2023.1 or newer
- JavaScript and HtmlTools plugins as dependencies
- Java 17 runtime

### Release Process

1. Update version in `gradle.properties`
2. Update `CHANGELOG.md` Unreleased section with version number
3. Push changes to main branch
4. Create and publish a GitHub release - this triggers automatic publishing to JetBrains Marketplace