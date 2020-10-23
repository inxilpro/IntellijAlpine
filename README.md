# IntellijAlpine

![Build](https://github.com/inxilpro/IntellijAlpine/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/15251.svg)](https://plugins.jetbrains.com/plugin/15251-intellijalpine)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/15251.svg)](https://plugins.jetbrains.com/plugin/15251-intellijalpine)

## Template ToDo list
- [x] Create a new [IntelliJ Platform Plugin Template][template] project.
- [x] Verify the [pluginGroup](/gradle.properties), [plugin ID](/src/main/resources/META-INF/plugin.xml) and [sources package](/src/main/kotlin).
- [x] Review the [Legal Agreements](https://plugins.jetbrains.com/docs/marketplace/legal-agreements.html).
- [x] [Publish a plugin manually](https://www.jetbrains.org/intellij/sdk/docs/basics/getting_started/publishing_plugin.html) for the first time.
- [x] Set the Plugin ID in the above README badges.
- [x] Set the [Deployment Token](https://plugins.jetbrains.com/docs/marketplace/plugin-upload.html).
- [ ] Click the <kbd>Watch</kbd> button on the top of the [IntelliJ Platform Plugin Template][template] to be notified about releases containing new features and fixes.

<!-- Plugin description -->
![intellij-alpine](https://user-images.githubusercontent.com/21592/97035246-0b7a9e00-1534-11eb-9722-2492f380eca3.gif)

This plugin adds support for the following [Alpine.js](https://github.com/alpinejs/alpine) features:

- Auto-complete alpine directives such as `x-data`
- Set the language to JavaScript inside your directives so that you have full
  syntax highlighting and code complete

<!-- Plugin description end -->

## Installation

- Using IDE built-in plugin system:
  
  <kbd>Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "IntellijAlpine"</kbd> >
  <kbd>Install Plugin</kbd>
  
- Manually:

  Download the [latest release](https://github.com/inxilpro/IntellijAlpine/releases/latest) and install it manually using
  <kbd>Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
