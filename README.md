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


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
