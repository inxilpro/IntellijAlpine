<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Alpine.js Support

## [Unreleased]

## [0.6.6] - 2025-06-19

### Fixed
- Fixed [PsiInvalidElementAccessException bug](https://github.com/inxilpro/IntellijAlpine/issues/79)

## [0.6.5] - 2024-05-13
This is just a version bump to add support for new IntelliJ platforms.

## [0.6.4] - 2024-01-16

### Fixed
- Reverted 'go to' changes added in `0.6.1` — it introduced too many bugs when using templating languages like Blade

## [0.6.3] - 2024-01-02
A bug was introduced that impacted Alpine when you did something like `x-data="@js([...])"`. This may not be a perfect fix, but it should help address it somewhat until I can find a better fix.

### Fixed
- Improved 'go to' support in Blade

## [0.6.2] - 2024-01-02

### Fixed
- Fixed intellij platform version constraints

## [0.6.1] - 2024-01-02

### Added
- Added typing support for `$el` and `$root`
- Improved intellisense for `x-data` derived properties
- Added initial support for Go To Declaration for `x-data` derived properties

## [0.6.0] - 2023-08-15

### Added
- Added support for IntelliJ platform version 2023.2
- Added support for [`glhd/alpine-wizard`](https://github.com/glhd/alpine-wizard)

## [0.5.0] - 2023-05-31

### Added
- Added better type support for `$refs`
- Added support for `x-id` and `$id()`
- Added support for `x-mask`
- Added support for `x-modelable`
- Added support for `x-teleport`
- Added support for `x-trap`
- Added support for `x-collapse`
- Added new help text for `x-` attributes

## [0.4.2]

### Added
- Added support for Blade directives like `@entangle()` inside Alpine directives
- Added option to disable gutter icons

### Fixed
- Addressed some issues where certain characters couldn't appear at the beginning or end of certain directives
- Fixed issue where Markdown plugin was interfering with Alpine gutter icon

## [0.4.1]

### Added
- Added support for `x-intersect`

### Changed
- Improved `$persist()` behavior
- Improved autocomplete in `x-data` and `x-init`

## [0.4.0]

### Added
- Added support for PHP and Blade fragments inside of Alpine directives
- Added Alpine gutter icon for easier identification of lines that have Alpine directives
- Added support for Alpine v3 directives and magics
- Added better support for modifiers like `@click.prevent`

### Changed
- Improved auto-complete logic

## [0.3.0]

### Added
- Added auto-complete support for simple x-data expressions
- Added better support for `x-for` and `x-spread`
- Added better language injection support for all Alpine directives

### Fixed
- Fixed an issue where the plugin would cause the IDE freeze when editing XML files

## [0.2.2]

### Added
- Added improved support for transition attributes

## [0.2.1]

### Fixed
- Prevented autocompletion on Laravel blade components
- Prevented language injection and autocompletion when outside of HTML scope

## [0.2.0]

### Added
- Improved type support
- Better auto-complete for bound attributes
- Added attribute descriptions
- Handling of `x-cloak` which has no value

### Changed
- Refactored a lot of underlying code

## [0.1.1]

### Added
- Support for object style `:class=` binding

## [0.1.0]

### Added
- Better auto-complete for bound attributes and events (using the IDE's suggestions for attributes 
  rather than an internal list of available attributes)

### Changed
- Internals refactor

## [0.0.3]

### Added
- Better JS language injection inside `x-` directives
- Auto-complete for magic properties like `$el` and `$dispatch`

### Fixed
- Removed Alpine icon which seemed to cause issues for some people

[Unreleased]: https://github.com/inxilpro/IntellijAlpine/compare/v0.6.6...HEAD
[0.6.6]: https://github.com/inxilpro/IntellijAlpine/compare/v0.6.5...v0.6.6
[0.6.5]: https://github.com/inxilpro/IntellijAlpine/compare/v0.6.4...v0.6.5
[0.6.4]: https://github.com/inxilpro/IntellijAlpine/compare/v0.6.3...v0.6.4
[0.6.3]: https://github.com/inxilpro/IntellijAlpine/compare/v0.6.2...v0.6.3
[0.6.2]: https://github.com/inxilpro/IntellijAlpine/compare/v0.6.0...v0.6.2
[0.6.1]: https://github.com/inxilpro/IntellijAlpine/compare/v0.6.0...v0.6.1
[0.6.0]: https://github.com/inxilpro/IntellijAlpine/compare/v0.5.0...v0.6.0
[0.5.0]: https://github.com/inxilpro/IntellijAlpine/compare/v0.4.2...v0.5.0
[0.4.2]: https://github.com/inxilpro/IntellijAlpine/compare/v0.4.1...v0.4.2
[0.4.1]: https://github.com/inxilpro/IntellijAlpine/compare/v0.4.0...v0.4.1
[0.4.0]: https://github.com/inxilpro/IntellijAlpine/compare/v0.3.0...v0.4.0
[0.3.0]: https://github.com/inxilpro/IntellijAlpine/compare/v0.2.2...v0.3.0
[0.2.2]: https://github.com/inxilpro/IntellijAlpine/compare/v0.2.1...v0.2.2
[0.2.1]: https://github.com/inxilpro/IntellijAlpine/compare/v0.2.0...v0.2.1
[0.2.0]: https://github.com/inxilpro/IntellijAlpine/compare/v0.1.1...v0.2.0
[0.1.1]: https://github.com/inxilpro/IntellijAlpine/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/inxilpro/IntellijAlpine/compare/v0.0.3...v0.1.0
[0.0.3]: https://github.com/inxilpro/IntellijAlpine/commits/v0.0.3
