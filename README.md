# Emoji Cross Reference

If you need to cross reference between [Unicode Emoji List, v15.0], [GitHub Emoji API] and
[Emoji Cheat Sheet] then these files can provide the raw information you need to create the
cross reference lookups. Including easy determination which Unicode Emoji symbols are missing
from the corresponding shortcut lists.

Files:

* [EmojiReference.txt](EmojiReference.txt) tab separated data, empty columns have a single
  space. Content can be pasted into a spreadsheet if desired.
* [EmojiReference.java](EmojiReference.java) java reference class, which loads the data file
  `EmojiReference.txt`. Too much data to include in a java class so has to be loaded
  dynamically. Put the `EmojiReference.txt` file in your app resources and modify
  `EmojiReference.java` so it can find it.
* [EmojiReference.json](EmojiReference.json) json reference file with all the data

Fields:

* `shortcut` : shortcut for either or both: [GitHub Emoji API] when `githubFile` is not null and
  [Emoji Cheat Sheet] when `emojiCheatSheetFile` is not null.
* `category` : where a shortcut is available
* `emojiCheatSheetFile` refers to the file name used by the [Emoji Cheat Sheet] for image files
  in the [`emoji-cheat-sheet.com/public/graphics/emojis/`] directory
* `githubFile` refers to the file name, used by [GitHub Emoji API], needs to be prefixed with
  the `githubUrl` to get the full URL of the image
* `unicodeChars` : list of unicode characters for the emoji
* `unicodeSampleFile` file name derived from unicode characters from the
  [Unicode Emoji List, v15.0], same as `githubFile` name derivation. Used for cross referencing
  only.
* `unicodeCldr` : unicode CLDR

**NOTE**: `category` is taken from [Emoji Cheat Sheet] category and filled in manually for
[GitHub Emoji API] which are not in [Emoji Cheat Sheet]. No category is set for
[Unicode Emoji List, v15.0] if no shortcut exists.

Please report any discrepancies so they can be corrected.

------

Copyright 2018, Vladimir Schneider

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the
License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
express or implied. See the License for the specific language governing permissions and
limitations under the License.

[`emoji-cheat-sheet.com/public/graphics/emojis/`]: https://github.com/WebpageFX/emoji-cheat-sheet.com/tree/master/public/graphics/emojis
[Emoji Cheat Sheet]: https://github.com/WebpageFX/emoji-cheat-sheet.com
[GitHub Emoji API]: https://api.github.com/emojis
[Unicode Emoji List, v15.0]: https://unicode.org/emoji/charts/emoji-list.html

