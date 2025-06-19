# IntellijAlpine

![Build](https://github.com/inxilpro/IntellijAlpine/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/15251-alpine-js-support.svg)](https://plugins.jetbrains.com/plugin/15251-alpine-js-support)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/15251-alpine-js-support.svg)](https://plugins.jetbrains.com/plugin/15251-alpine-js-support)
![Release](https://github.com/inxilpro/IntellijAlpine/workflows/Release/badge.svg)

## Release Workflow

1. Update `gradle.properties` with the **new plugin version**
2. Update `CHANGELOG.md` to reflect the new changes in the **Unreleased** section
3. Push `main` branch and allow all GitHub Actions to run
4. Go to [releases page](https://github.com/inxilpro/IntellijAlpine/releases) and edit/publish draft release

## Plugin Description

<!-- Plugin description -->
![intellij-alpine](https://user-images.githubusercontent.com/21592/121929093-d7e0c400-cd0e-11eb-8ff3-d52db5831a55.gif)

This plugin adds support for the following [Alpine.js](https://github.com/alpinejs/alpine) features:

- Auto-complete alpine directives such as `x-data`
- Set the language to JavaScript inside your directives so that you have full
  syntax highlighting and code complete

<!-- Plugin description end -->

## Installation

- Using IDE built-in plugin system:
  
  <kbd>Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "Alpine.js Support"</kbd> >
  <kbd>Install Plugin</kbd>
  
- Manually:

  Download the [latest release](https://github.com/inxilpro/IntellijAlpine/releases/latest) and install it manually using
  <kbd>Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## Future Improvements

The Alpine.js Support plugin provides solid foundational features, but there are many opportunities to enhance the 
developer experience. Here are potential improvements organized by priority:

### High Priority Enhancements

- **Go to Declaration & Find Usages**: Navigate between x-data property definitions and their usage across components. 
  This would allow developers to quickly jump between related code sections.

- **Refactoring Support**: Enable renaming of Alpine properties/methods with automatic updates across all references. 
  This is essential for maintaining large Alpine.js applications.

- **Code Inspections**: Add intelligent inspections to catch common mistakes:

  - Undefined property usage in Alpine expressions
  - Deprecated Alpine v2 syntax when using v3
  - Missing required modifiers (e.g., `.prevent` on form submissions)
  - Performance anti-patterns

- **Test Coverage**: Implement comprehensive unit and integration tests to ensure plugin stability and make future development safer.

### Medium Priority Features

- **Live Templates**: Provide code snippets for common Alpine patterns:

  - Component boilerplate with x-data
  - Common directive combinations (x-show with x-transition)
  - Alpine store definitions
  - Event handler patterns

- **Quick Documentation**: Show inline documentation for Alpine directives and modifiers on hover or with F1/Ctrl+Q.

- **Structure View**: Display Alpine component hierarchy in the IDE's structure panel, showing x-data scopes and their properties.

- **Enhanced Type Support**: Improve JavaScript/TypeScript type inference within Alpine expressions for better auto-completion and error detection.

### Advanced Features

- **Debugging Support**: Special debugging features for Alpine components, such as inspecting reactive data state.

- **Code Generation**: Actions to quickly generate:
  - Alpine components from templates
  - Store definitions
  - Common patterns (modals, dropdowns, etc.)

- **Performance Analysis**: Tools to analyze component complexity and suggest optimizations.

- **Plugin Ecosystem Support**: Add support for popular Alpine plugins like Alpine Morph, Persist, Focus, and Intersect.

### Technical Improvements

- **Better Error Handling**: Provide clearer error messages and recovery options when things go wrong.

- **Performance Optimizations**: Profile and optimize completion providers and inspections for better IDE performance.

- **Configuration Options**: Allow users to customize plugin behavior, such as:

  - Preferred Alpine version for syntax checking
  - Custom directive definitions for third-party plugins
  - Code style preferences

- **Project-wide Analysis**: Analyze entire projects to find unused Alpine components or properties.

---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
