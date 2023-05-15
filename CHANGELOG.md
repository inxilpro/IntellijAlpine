<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Alpine.js Support

## [Unreleased]
### Added

- Added better type support for `$refs`
- Added support for `x-id` and `$id()` 
- Added support for `x-mask`
- Added support for `x-modelable`
- Added support for `x-teleport`
- Added support for `x-trap`
- Added support for `x-collapse`
- Added new help text for `x-` attributes

### Changed

### Deprecated

### Removed

### Fixed

### Security
## [0.4.2]
### Added

- Added support for Blade directives like `@entangle()` inside Alpine directives
- Added option to disable gutter icons

### Changed

### Deprecated

### Removed

### Fixed

- Addressed some issues where certain characters couldn't appear at the beginning or end of certain directives
- Fixed issue where Markdown plugin was interfering with Alpine gutter icon

### Security
## [0.4.1]
### Added

- Added support for `x-intersect`

### Changed

- Improved `$persist()` behavior
- Improved autocomplete in `x-data` and `x-init`

### Deprecated

### Removed

### Fixed

### Security
## [0.4.0]

### Added

- Added support for PHP and Blade fragments inside of Alpine directives
- Added Alpine gutter icon for easier identification of lines that have Alpine directives
- Added support for Alpine v3 directives and magics
- Added better support for modifiers like `@click.prevent`

### Changed

- Improved auto-complete logic

### Deprecated

### Removed

### Fixed

### Security

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
