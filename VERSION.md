# Version History

[TOC]: #

- [1.2.2](#122)
- [1.2.0](#120)
- [1.1.1](#111)
- [1.1.0](#110)
- [1.0.0](#100)


## 1.2.2

* Remove: using emoji cheat sheet alternative name as shortcuts. These
  are hodge-podge mappings and result in duplicate mappings.
* Add: code to validate all shortcut aliases are mapped to the same
  category.
* Add: category forced override in [GitHubCategoryMap.md] by using `!`
  after the category text.

## 1.2.0

* Change: create [GitHubCategoryMap.md] file to
  map GitHub shortcuts to categories.
* Change: cheat sheet emoji directory to `cheat`
* Add: cheat sheet and GitHub combined emoji directory to `emojis`

## 1.1.1

* Change: emoji cheat sheet and github custom emoji category to
  `emoji-cheat-sheet` and `github`, with custom as subcategory.
* Fix: `githubUrl` in `EmojiReference` to
  `https://github.githubassets.com/images/icons/emoji`
* Fix: `unicodeSampleFile` was not set in reference files.

## 1.1.0

* Add: emoji image extractor console app
* Add: download of full emoji list from
  <https://unicode.org/emoji/charts/full-emoji-list.html><!-- @IGNORE PREVIOUS: link -->
* Add: download of full emoji modifiers list from
  <https://unicode.org/emoji/charts/full-emoji-modifiers.html><!-- @IGNORE PREVIOUS: link -->
* Add: download of GitHub emoji API from <https://api.github.com/emojis>
* Add: downloading of GitHub emoji images
* Add: generating reference files from emoji-cheat-sheet, GitHub API and
  unicode reference HTML.

## 1.0.0

* Initial Release


[GitHubCategoryMap.md]: GitHubCategoryMap.md
