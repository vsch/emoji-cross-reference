# Emoji Cross Reference

If you need to cross-reference between [Unicode Emoji List, v15.0] and
optionally [Unicode Emoji Modifiers List, v15.0], [GitHub Emoji API] and
[emoji-cheat-sheet.com] then these files can provide the raw information
you need to create the cross-reference lookup. Including easy
determination which Unicode Emoji symbols are missing from the
corresponding shortcut lists.

The [EmojiImageExtractor.java] is a console application with two
options:

* `--download` force download of reference files and GitHub images
* `--download-files` force download of reference files
* `--download-github-images` force download of GitHub emoji images to
  `github_images`
* `--no-modifiers` to exclude [Unicode Emoji Modifiers List, v15.0] from
  processing.

the application will:

* download the [Unicode Emoji List, v15.0] page, and
  [Unicode Emoji Modifiers List, v15.0] unless disabled with
  `--no-modifiers` command argument, if it is missing, or requested
  through command line options
* download the [GitHub Emoji API] file, if it is missing, or requested
  through command line options
* download all images listed in [GitHub Emoji API] into `github_emoji`
  if the `github_images` directory is missing, has fewer than 1000
  files, or requested through command line options.
* extract images from the downloaded [Unicode Emoji List, v15.0] file by
  browser type to `emoji_images` to a subdirectory per browser column
  header, converted to lowercase.
* copy `github_emoji` images to `emoji_images/ghub` subdirectory
* copy [`emoji-cheat-sheet.com/public/graphics/emojis/`] images to
  `emoji_images/emojis` subdirectory, renaming the files to the common
  file name used by GitHub or Unicode emoji, when applicable.
* create a text reference file [EmojiReference.txt] to be loaded by
  [EmojiReference.java] at run time.
* create a JSON reference file [EmojiReference.json] in case this format
  is more comfortable for your application.

Files:

* [EmojiReference.txt] tab separated data, empty columns have a single
  space. Content can be pasted into a spreadsheet if desired.
* [EmojiReference.java] java reference class, which loads the
  [EmojiReference.txt] data file. There is too much data to include in a
  java class, so it has to be loaded dynamically. Put the file in your
  app resources and modify the loader to load it from the resource path.
* [EmojiReference.json] json reference file with all the cross-reference
  data.

Fields:

* `shortcut` : shortcut for either or both: [GitHub Emoji API] when
  `githubFile` is not null and [emoji-cheat-sheet.com] when
  `emojiCheatSheetFile` is not null.
* `aliasShortcuts` : alias shortcuts, when applicable or empty.
* `category` : category from [Unicode Emoji List, v15.0]
* `subcategory` : subcategory [Unicode Emoji List, v15.0]
* `emojiCheatSheetFile` refers to the file name used by the
  [emoji-cheat-sheet.com] for image files in the
  [`emoji-cheat-sheet.com/public/graphics/emojis/`] directory. The file
  name in the `emoji_images/emojis` directory will be the one given in
  the `unicodeSampleFile` field.
* `githubFile` refers to the file name, used by [GitHub Emoji API],
  needs to be prefixed with the `githubUrl`:
  `https://github.githubassets.com/images/icons/emoji/` to get the full
  image URL.
* `unicodeChars` : list of unicode characters for the emoji
* `unicodeSampleFile` file name derived from unicode characters from the
  [Unicode Emoji List, v15.0], same as `githubFile` name derivation.
  Used for cross-referencing only.
* `unicodeCldr` : unicode CLDR
* `browserTypes` : browser type from [Unicode Emoji List, v15.0] column
  headers for various types, lowercase of column headers. Note, that
  emoji-cheatsheet and GitHub do not appear in the list, use
  `emojiCheatSheetFile` entry to determine if available in
  emoji-cheatsheet or `githubFile` for GitHub availability.

**NOTE**: `category` and `subcategory` are taken from
[Unicode Emoji List, v15.0] table header rows. Category is mapped in the
[EmojiImageExtractor.java] from the text appearing in the
[Unicode Emoji List, v15.0], subcategory is taken as is.

[emoji-cheat-sheet.com] repository needs to be cloned to
`emoji-cheat-sheet.com` subdirectory in this project. Use the following
command in the root directory for the project to clone it:

```shell
git clone https://github.com/WebpageFX/emoji-cheat-sheet.com
```

Please report any discrepancies so they can be corrected.

------

Copyright 2018-2023, Vladimir Schneider

Licensed under the Apache License, Version 2.0 (the "License"); you may
not use this file except in compliance with the License. You may obtain
a copy of the License at

<https://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

[`emoji-cheat-sheet.com/public/graphics/emojis/`]: https://github.com/WebpageFX/emoji-cheat-sheet.com/tree/master/public/graphics/emojis
[EmojiImageExtractor.java]: src/main/java/com/vladsch/emoji/EmojiImageExtractor.java
[EmojiReference.json]: src/main/resources/EmojiReference.json
[EmojiReference.java]: src/main/java/com/vladsch/emoji/EmojiReference.java
[EmojiReference.txt]: src/main/resources/EmojiReference.txt
[emoji-cheat-sheet.com]: https://github.com/WebpageFX/emoji-cheat-sheet.com
[GitHub Emoji API]: https://api.github.com/emojis
[Unicode Emoji List, v15.0]: https://unicode.org/emoji/charts/emoji-list.html
<!-- @IGNORE PREVIOUS: link -->

[Unicode Emoji Modifiers List, v15.0]: https://unicode.org/emoji/charts/full-emoji-modifiers.html
<!-- @IGNORE PREVIOUS: link -->

