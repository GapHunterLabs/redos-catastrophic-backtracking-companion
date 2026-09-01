<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# ReDoS Catastrophic-Backtracking Companion Changelog

## [Unreleased]

## [0.1.0]

### Added

- Warning on a Java `Pattern.compile("...")`/`"...".matches("...")`
  call whose literal regex has a catastrophic-backtracking structural
  pattern: a nested unbounded quantifier (`(a+)+`), or an
  unbounded-quantified alternation with overlapping branches
  (`(a|a)*`) — CWE-400, ReDoS.
- Hand-rolled regex grammar and parser, independent of
  `java.util.regex`.

[Unreleased]: https://github.com/GapHunterLabs/redos-catastrophic-backtracking-companion/compare/0.1.0...HEAD
[0.1.0]: https://github.com/GapHunterLabs/redos-catastrophic-backtracking-companion/commits/0.1.0
