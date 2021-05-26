<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Alpine.js Support

## [Unreleased]
### Added

- Added improved support for transition attributes

### Changed

### Deprecated

### Removed

### Fixed

### Security

## [0.2.1]

### Added

### Changed

### Deprecated

### Removed

### Fixed

- Prevented autocompletion on Laravel blade components
- Prevented language injection and autocompletion when outside of HTML scope

### Security

## [0.2.0]

### Added

- Improved type support
- Better auto-complete for bound attributes
- Added attribute descriptions
- Handling of `x-cloak` which has no value

### Changed

- Refactored a lot of underlying code

### Deprecated

### Removed

### Fixed

### Security

## [0.1.1]

### Added

- Support for object style `:class=` binding

### Changed

### Deprecated

### Removed

### Fixed

### Security

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

### Changed

### Deprecated

### Removed

### Fixed

- Removed Alpine icon which seemed to cause issues for some people
