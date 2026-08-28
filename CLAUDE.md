# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Code Guidelines

- Always use modern idiomatic Kotlin code
- When implementing singletons, prefer `Foo.instance` over `Foo.Companion.instance` or `Foo.getInstance()`
- Only add docblocks and comments when they provide substantive value. Comments should always explain "why" not "what."
- Use IntelliJ platform features wherever possible—don't reinvent anything that already exists
- Keep code performant without making it difficult to understand

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

1. Land the changes on main. Each push to main refreshes a draft GitHub release for the current
   `pluginVersion`, using the `CHANGELOG.md` Unreleased section as its notes. The draft is skipped
   if that version has already been published.
2. Publish the draft release - this triggers automatic publishing to JetBrains Marketplace
3. The release workflow then opens a "Post-release update" pull request that files the notes under
   the published version in `CHANGELOG.md` and bumps `pluginVersion` to the next patch. Merge it,
   editing the version by hand first if the next release should be a minor or major bump.