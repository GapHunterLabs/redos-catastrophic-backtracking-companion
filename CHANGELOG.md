<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# ReDoS Catastrophic-Backtracking Companion Changelog

## [Unreleased]

### Added

- A description page for the inspection in **Settings | Editor |
  Inspections**, which showed "Under construction".

### Changed

- The rating prompt's local counter keeps one-way fingerprints of findings
  instead of their file paths, and deletes the list that earlier versions
  kept.
- `PRIVACY.md` describes the values the plugin keeps in the IDE's local
  settings.

## [0.1.1]

### Fixed

- Review/star CTA now links to this plugin's own Marketplace
  reviews page instead of the vendor's generic plugin list.

## [0.1.0]

### Added

- Warning on a Java `Pattern.compile("...")`/`"...".matches("...")`
  call whose literal regex has a catastrophic-backtracking structural
  pattern: a nested unbounded quantifier (`(a+)+`), or an
  unbounded-quantified alternation with overlapping branches
  (`(a|a)*`) — CWE-400, ReDoS.
- Hand-rolled regex grammar and parser, independent of
  `java.util.regex`.

[Unreleased]: https://github.com/GapHunterLabs/redos-catastrophic-backtracking-companion/compare/0.1.1...HEAD
[0.1.1]: https://github.com/GapHunterLabs/redos-catastrophic-backtracking-companion/compare/0.1.0...0.1.1
[0.1.0]: https://github.com/GapHunterLabs/redos-catastrophic-backtracking-companion/commits/0.1.0
