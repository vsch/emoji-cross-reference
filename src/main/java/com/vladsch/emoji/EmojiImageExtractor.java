package com.vladsch.emoji;

import com.vladsch.boxed.json.*;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.json.JsonValue;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmojiImageExtractor {

    public static final String EMOJI_CHEAT_SHEET = "EmojiCheatsheet";
    public static final String EMOJI_CHEAT_SHEET_SUBDIRECTORY = "cheat";
    public static final String EMOJI_COMBINED_SUBDIRECTORY = "emoji";
    public static final String GITHUB_SUBDIRECTORY = "github";
    public static final String GITHUB_IMAGES_DIRECTORY = "github_emoji";
    public static final String urlFullEmojiList = "https://unicode.org/emoji/charts/full-emoji-list.html";
    public static final String urlFullEmojiModifierList = "https://unicode.org/emoji/charts/full-emoji-modifiers.html";
    public static final String urlGithubEmojiApi = "https://api.github.com/emojis";
    public static final String outputDirectory = "emoji_images";
    public static final String EMOJI_REFERENCE_JSON_PATH = "EmojiReference.json";
    public static final String EMOJI_REFERENCE_TXT_PATH = "EmojiReference.txt";
    public static final String FULL_EMOJI_HTML_PATH = "emoji_list.html";
    public static final String FULL_EMOJI_MODIFIER_HTML_PATH = "emoji_modifier_list.html";
    public static final String EMOJI_CHEAT_SHEET_HTML_PATH = "emoji-cheat-sheet.com/public/index.html";
    public static final String githubEmojiApiFilePath = "githubEmojiApi.json";
    public static final String githubEmojiCategoryMapPath = "GitHubCategoryMap.md";
    public static final String emojiCheatsheetDirectoryPath = "emoji-cheat-sheet.com/public/graphics/emojis";
    public static final String GITHUB_EMOJI_URL_PATH = "https://github.githubassets.com/images/icons/emoji/";

    public static final int DOWNLOAD_BUFFER = 65536;
    public static final int DOWNLOAD_UPDATE = 10;
    public static final String GRAPHICS_EMOJIS_PREFIX = "graphics/emojis/";

    // Used to remove duplicate file names which are mapped by alias shortcuts
    public static final HashMap<String, String> emojiCheatsheetFileAliasMap = new HashMap<>();
    static {
        emojiCheatsheetFileAliasMap.put("+1.png", "thumbsup.png");
        emojiCheatsheetFileAliasMap.put("-1.png", "thumbsdown.png");
        emojiCheatsheetFileAliasMap.put("plus1.png", "thumbsup.png");
        emojiCheatsheetFileAliasMap.put("minus1.png", "thumbsdown.png");
        emojiCheatsheetFileAliasMap.put("red_car.png", "car.png");
        emojiCheatsheetFileAliasMap.put("mans_shoe.png", "shoe.png");
        emojiCheatsheetFileAliasMap.put("person_with_blond_hair.png", "man.png");
        emojiCheatsheetFileAliasMap.put("memo.png", "pencil.png");
        emojiCheatsheetFileAliasMap.put("person_with_pouting_face.png", "girl.png");
        emojiCheatsheetFileAliasMap.put("hankey.png", "shit.png");
        emojiCheatsheetFileAliasMap.put("feet.png", "paw_prints.png");
        emojiCheatsheetFileAliasMap.put("heavy_exclamation_mark.png", "exclamation.png");
        emojiCheatsheetFileAliasMap.put("person_frowning.png", "girl.png");
        emojiCheatsheetFileAliasMap.put("sailboat.png", "boat.png");
        emojiCheatsheetFileAliasMap.put("poop.png", "shit.png");
        emojiCheatsheetFileAliasMap.put("telephone.png", "phone.png");
        emojiCheatsheetFileAliasMap.put("runner.png", "running.png");
        emojiCheatsheetFileAliasMap.put("satisfied.png", "laughing.png");
        emojiCheatsheetFileAliasMap.put("all_the_things.png", "joy.png");
        emojiCheatsheetFileAliasMap.put("e-mail.png", "email.png");
        emojiCheatsheetFileAliasMap.put("facepunch.png", "punch.png");
        emojiCheatsheetFileAliasMap.put("raised_hand.png", "hand.png");
        emojiCheatsheetFileAliasMap.put("collision.png", "boom.png");
        emojiCheatsheetFileAliasMap.put("tshirt.png", "shirt.png");
        emojiCheatsheetFileAliasMap.put("gb.png", "uk.png");
        emojiCheatsheetFileAliasMap.put("simple_smile.png", "smile.png");
    }

    /**
     * Map of Unicode Emoji List Category Header to reference category map, manually maintained
     */
    public static final HashMap<String, String> categoryMap = new HashMap<>();

    static {
        categoryMap.put("Smileys & Emotion", "people");
        categoryMap.put("People & Body", "people");
        categoryMap.put("Component", "component");
        categoryMap.put("Animals & Nature", "nature");
        categoryMap.put("Food & Drink", "food");
        categoryMap.put("Travel & Places", "places");
        categoryMap.put("Activities", "activities");
        categoryMap.put("Objects", "objects");
        categoryMap.put("Symbols", "symbols");
        categoryMap.put("Flags", "flags");
    }

    static class Emoji {
        public int emojiNumber;
        public List<String> shortcuts;
        public String category;
        public String subcategory;
        public String unicodeChars;
        public String unicodeSampleFile;
        public String unicodeCldr;
        public String githubFile;
        public String emojiCheatSheetFile;
        public List<String> browserTypes;

        public Emoji() {
            emojiNumber = 0;
            shortcuts = null;
            category = null;
            subcategory = null;
            unicodeChars = null;
            unicodeSampleFile = null;
            unicodeCldr = null;
            githubFile = null;
            emojiCheatSheetFile = null;
            browserTypes = null;
        }

        public String categoryOrSpace() {
            return category == null ? " " : category;
        }

        public String subcategoryOrSpace() {
            return subcategory == null ? " " : subcategory;
        }

        public String unicodeCharsOrSpace() {
            return unicodeChars == null ? " " : unicodeChars;
        }

        public String unicodeSampleFileOrSpace() {
            return unicodeSampleFile == null ? " " : unicodeSampleFile;
        }

        public String unicodeCldrOrSpace() {
            return unicodeCldr == null ? " " : unicodeCldr;
        }

        public String githubFileOrSpace() {
            return githubFile == null ? " " : githubFile;
        }

        public String emojiCheatSheetFileOrSpace() {
            return emojiCheatSheetFile == null ? " " : emojiCheatSheetFile;
        }
    }

    public static ArrayList<Emoji> emojiList = new ArrayList<>();

    public static HashMap<String, ArrayList<String>> fileNameToShortcutsMap = new HashMap<>();
    public static HashMap<String, String> fileNameToGitHubUrlMap = new HashMap<>();
    public static HashSet<String> githubFileSet = new HashSet<>();
    public static HashMap<String, String> shortcutToCommonFileNameMap = new HashMap<>();
    public static HashMap<String, String> shortcutToCategoryMap = new HashMap<>();
    public static HashSet<String> shortcutForcedMap = new HashSet<>();
    public static HashMap<String, String[]> emojiCheatSheetFileToShortcutsMap = new HashMap<>();
    public static HashMap<String, String> emojiCheatsheetFileMap = new HashMap<>();
    public static HashSet<String> emojiCheatsheetFileSet = new HashSet<>();

    public static void main(String[] args) {
        boolean downloadFullEmoji = false;
        boolean downloadFullEmojiModifiers = false;
        boolean downloadGitHubApi = false;
        boolean downloadGitHubImages = false;
        boolean noModifiers = false;
        boolean showForcedOverrides = false;

        for (String arg : args) {
            switch (arg) {
                case "--download":
                    downloadFullEmoji = true;
                    downloadFullEmojiModifiers = true;
                    downloadGitHubApi = true;
                    downloadGitHubImages = true;
                    break;

                case "--download-github-images":
                    downloadGitHubImages = true;
                    break;

                case "--download-files":
                    downloadFullEmoji = true;
                    downloadFullEmojiModifiers = true;
                    downloadGitHubApi = true;
                    break;

                case "--no-modifiers":
                    noModifiers = true;
                    break;

                case "--show-overrides":
                    showForcedOverrides = true;
                    break;

                default:
                    System.out.printf("Unknown command line option '%s', ignored.\n", arg);
            }
        }

        if (!downloadFullEmoji) {
            File htmlFile = new File(FULL_EMOJI_HTML_PATH);
            downloadFullEmoji = !htmlFile.exists();
        }

        if (!downloadFullEmojiModifiers && !noModifiers) {
            File htmlModifierFile = new File(FULL_EMOJI_MODIFIER_HTML_PATH);
            downloadFullEmojiModifiers = !htmlModifierFile.exists();
        }

        if (!downloadGitHubApi) {
            File githubEmojiApiFile = new File(githubEmojiApiFilePath);
            downloadGitHubApi = !githubEmojiApiFile.exists();
        }

        if (!downloadGitHubImages) {
            File githubImageDirectory = new File(GITHUB_IMAGES_DIRECTORY);
            if (!githubImageDirectory.exists() || !githubImageDirectory.isDirectory()) {
                downloadGitHubImages = true;
            } else {
                File[] files = githubImageDirectory.listFiles();
                downloadGitHubImages = files == null || files.length < 1000;
            }
        }

        try {
            if (downloadFullEmoji) {
                downloadPage(urlFullEmojiList, FULL_EMOJI_HTML_PATH);
            }

            if (downloadFullEmojiModifiers) {
                downloadPage(urlFullEmojiModifierList, FULL_EMOJI_MODIFIER_HTML_PATH);
            }

            if (downloadGitHubApi) {
                downloadPage(urlGithubEmojiApi, githubEmojiApiFilePath);
            }

            // read the emoji cheat sheet files and map the names as shortcut to common file names for compatibility
            processEmojiCheatSheetShortcuts(EMOJI_CHEAT_SHEET_HTML_PATH);

            File githubEmojiApiFile = new File(githubEmojiApiFilePath);
            InputStream githubJson = Files.newInputStream(githubEmojiApiFile.toPath());
            BoxedJsObject githubApi = BoxedJson.objectFrom(githubJson);
            HashMap<String, Integer> githubEmojiCategoryLineMap = new HashMap<>();

            // read the GitHub shortcut to category map
            File githubEmojiCategoryMapFile = new File(githubEmojiCategoryMapPath);
            StringBuilder overrides = new StringBuilder();
            StringBuilder mismatches = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(new FileReader(githubEmojiCategoryMapPath))) {
                String line;
                int lineNumber = 0;
                Pattern pattern = Pattern.compile(":(.+):\\s+([^ !]+)\\s*(!?)");

                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    Matcher matcher = pattern.matcher(line);

                    if (matcher.find()) {
                        String shortcut = matcher.group(1);
                        String category = matcher.group(2);
                        String force = matcher.group(3);
                        boolean forced = !force.isEmpty();

                        if (githubApi.containsKey(shortcut)) {
                            if (!shortcutToCategoryMap.containsKey(shortcut)) {
                                shortcutToCategoryMap.put(shortcut, category);
                                if (forced) shortcutForcedMap.add(shortcut);
                            } else {
                                if (githubEmojiCategoryLineMap.containsKey(shortcut)) {
                                    System.out.printf("GitHub shortcut '%s' is duplicated in %s:%d\n", shortcut, githubEmojiCategoryMapFile.getAbsolutePath(), lineNumber);
                                } else {
                                    // must be from emojiCheatSheet
                                    String cheatsheetCategory = shortcutToCategoryMap.get(shortcut);

                                    if (forced) {
                                        // override
                                        shortcutToCategoryMap.put(shortcut, category);
                                        shortcutForcedMap.add(shortcut);
                                        if (showForcedOverrides) {
                                            overrides.append(String.format("'%s' category '%s' overrides '%s', in %s:%d\n"
                                                    , shortcut, category, cheatsheetCategory
                                                    , githubEmojiCategoryMapFile.getAbsolutePath(), lineNumber
                                            ));
                                        }
                                    } else {
                                        if (!category.equals(cheatsheetCategory)) {
                                            mismatches.append(String.format("'%s' category '%s' != '%s', in %s:%d\n"
                                                    , shortcut, category, cheatsheetCategory
                                                    , githubEmojiCategoryMapFile.getAbsolutePath(), lineNumber
                                            ));
                                        }
                                    }
                                }
                            }
                        } else {
                            System.out.printf("GitHub shortcut '%s' at %s:%d is not found in GitHub API\n", shortcut, githubEmojiCategoryMapFile.getAbsolutePath(), lineNumber);
                        }

                        githubEmojiCategoryLineMap.put(shortcut, lineNumber);
                    } else {
                        System.out.printf("Malformed line '%s' at %s:%d\n", line, githubEmojiCategoryMapFile.getAbsolutePath(), lineNumber);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            String allMismatches = mismatches.toString();
            if (!allMismatches.isEmpty()) {
                System.out.print("Mismatched GitHub and emoji-cheatsheet category mappings:\n");
                System.out.print(allMismatches);
            }

            String allOverrides = overrides.toString();
            if (!allOverrides.isEmpty()) {
                System.out.print("Overridden emoji-cheatsheet category mappings:\n");
                System.out.print(allOverrides);
            }

            // parse and process the githubEmojiApi to extract file name (unicode format when available) and shortcut
            int githubImages = githubApi.size();
            int githubImageCount = 0;

            Path githubImagePath = Paths.get(GITHUB_IMAGES_DIRECTORY);
            if (downloadGitHubImages) {
                deleteDirectoryContents(githubImagePath.toFile());
                Files.createDirectories(githubImagePath);
            }

            Path githubEmojiPath = Paths.get(outputDirectory, GITHUB_SUBDIRECTORY);
            Path combinedEmojiPath = Paths.get(outputDirectory, EMOJI_COMBINED_SUBDIRECTORY);

            File emojiDirectory = new File(outputDirectory);
            if (emojiDirectory.exists()) {
                deleteDirectoryContents(emojiDirectory);
            }

            if (downloadGitHubImages) {
                System.out.printf("Downloading GitHub emoji images %d / %d ", githubImageCount, githubImages);
            }

            Files.createDirectories(githubEmojiPath);
            Files.createDirectories(combinedEmojiPath);

            for (Map.Entry<String, JsonValue> entry : githubApi.entrySet()) {
                String gitHubUrl = BoxedJson.of(entry.getValue()).asJsString().getString();
                String shortcut = entry.getKey();
                int startPos = gitHubUrl.lastIndexOf('/');
                int endPos = gitHubUrl.indexOf('?', startPos);

                if (startPos != -1) {
                    String fileName = gitHubUrl.substring(startPos + 1, endPos);
                    ArrayList<String> shortcuts = fileNameToShortcutsMap.computeIfAbsent(fileName, keyString -> new ArrayList<>());
                    if (!shortcuts.contains(shortcut)) shortcuts.add(shortcut);
                    String githubUrlPath = gitHubUrl.substring(GITHUB_EMOJI_URL_PATH.length());
                    fileNameToGitHubUrlMap.put(fileName, githubUrlPath);
                    githubFileSet.add(fileName);
                    shortcutToCommonFileNameMap.put(shortcut, fileName);

                    Path imagePath = githubImagePath.resolve(fileName);

                    if (downloadGitHubImages) {
                        downloadPage(gitHubUrl, imagePath.toString());
                        if (++githubImageCount % 100 == 0) {
                            System.out.printf("\nDownloading GitHub emoji images %d / %d ", githubImageCount, githubImages);
                        }
                        System.out.print('.');
                    }

                    // copy github image file to output directory
                    Path destinationPath = githubEmojiPath.resolve(fileName);
                    Files.copy(imagePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);

                    // copy github image file to combined directory
                    Path combinedDestinationPath = combinedEmojiPath.resolve(fileName);
                    Files.copy(imagePath, combinedDestinationPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }

            if (downloadGitHubImages) {
                System.out.print(" done.\n");
            }

            // process emoji cheat sheet image files
            Path cheatsheetEmojiPath = Paths.get(emojiCheatsheetDirectoryPath);
            for (Map.Entry<String, String[]> emojiCheatsheetEntry : emojiCheatSheetFileToShortcutsMap.entrySet()) {
                String emojiCheatsheetFileName = emojiCheatsheetEntry.getKey();
                String[] emojiShortcuts = emojiCheatsheetEntry.getValue();

                if (emojiCheatsheetFileAliasMap.containsKey(emojiCheatsheetFileName)) {
                    continue;
                }

                boolean hadCommonShortcut = false;

                for (String emojiShortcut : emojiShortcuts) {
                    if (shortcutToCommonFileNameMap.containsKey(emojiShortcut)) {
                        // map it to the common file, but only add the first file, ignore the rest
                        String commonFileName = shortcutToCommonFileNameMap.get(emojiShortcut);
                        hadCommonShortcut = true;

                        if (!emojiCheatsheetFileMap.containsKey(commonFileName)) {
                            emojiCheatsheetFileMap.put(commonFileName, emojiCheatsheetFileName);
                            emojiCheatsheetFileSet.add(emojiCheatsheetFileName);
                        } else {
                            System.out.printf("EmojiCheatsheet file '%s' duplicate of '%s'\n", emojiCheatsheetFileName, emojiCheatsheetFileMap.get(commonFileName));
                        }

                        break;
                    }
                }

                if (!hadCommonShortcut) {
                    // map it to the file name as is
                    shortcutToCommonFileNameMap.put(emojiShortcuts[0], emojiCheatsheetFileName);

                    System.out.printf("EmojiCheatsheet file '%s', shortcut '%s' not matched.\n", emojiCheatsheetFileName, emojiShortcuts[0]);
                    emojiCheatsheetFileMap.put(emojiCheatsheetFileName, emojiCheatsheetFileName);
                    emojiCheatsheetFileSet.add(emojiCheatsheetFileName);
                }

                // copy cheat sheet image file to combined directory, overwriting github ones
                Path combinedDestinationPath = combinedEmojiPath.resolve(emojiCheatsheetFileName);
                Path cheatsheetImagePath = cheatsheetEmojiPath.resolve(emojiCheatsheetFileName);
                Files.copy(cheatsheetImagePath, combinedDestinationPath, StandardCopyOption.REPLACE_EXISTING);
            }

            // Read the HTML file
            int maxEmojiNumber = processUnicodeEmojiTable(0, FULL_EMOJI_HTML_PATH);

            if (!noModifiers) {
                maxEmojiNumber = processUnicodeEmojiTable(maxEmojiNumber, FULL_EMOJI_MODIFIER_HTML_PATH);
            }

            // check for missing categories
            for (String shortcut : githubApi.keySet()) {
                if (!shortcutToCategoryMap.containsKey(shortcut)) {
                    System.out.printf("GitHub shortcut '%s' is missing category,  defined in %s:%d\n", shortcut
                            , githubEmojiCategoryMapFile.getAbsolutePath(), githubEmojiCategoryLineMap.get(shortcut)
                    );
                } else {
                    // check for shortcut aliases mapping to different categories
                    String category = shortcutToCategoryMap.get(shortcut);
                    String fileName = shortcutToCommonFileNameMap.get(shortcut);
                    ArrayList<String> shortcuts = fileNameToShortcutsMap.get(fileName);

                    if (shortcuts != null) {
                        for (String aliasShortcut : shortcuts) {
                            boolean forcedShortcut = shortcutForcedMap.contains(shortcut);
                            boolean forcedAliasShortcut = shortcutForcedMap.contains(aliasShortcut);
                            String aliasCategory = shortcutToCategoryMap.get(aliasShortcut);

                            String forcedShortcutText = forcedShortcut ? "!" : "";
                            String forcedAliasShortcutText = forcedAliasShortcut ? "!" : "";

                            if (shortcut.equals(aliasShortcut) || githubEmojiCategoryLineMap.get(shortcut) > githubEmojiCategoryLineMap.get(aliasShortcut)) {
                                if (!forcedShortcutText.equals(forcedAliasShortcutText)) {
                                    System.out.printf("GitHub shortcut '%s' category '%s'%s and alias '%s' category '%s'%s have mismatched force override,\n" +
                                                    "   shortcut defined in %s:%d\n" +
                                                    "   alias defined in %s:%d\n"
                                            , shortcut, category, forcedShortcutText
                                            , aliasShortcut, aliasCategory, forcedAliasShortcutText
                                            , githubEmojiCategoryMapFile.getAbsolutePath(), githubEmojiCategoryLineMap.get(shortcut)
                                            , githubEmojiCategoryMapFile.getAbsolutePath(), githubEmojiCategoryLineMap.get(aliasShortcut)
                                    );
                                }
                                continue;
                            }

                            if (aliasCategory == null) {
                                System.out.printf("GitHub shortcut '%s' alias '%s' is missing category,  defined in %s:%d\n"
                                        , shortcut
                                        , aliasShortcut
                                        , githubEmojiCategoryMapFile.getAbsolutePath(), githubEmojiCategoryLineMap.get(aliasShortcut)
                                );
                            } else if (!category.equals(aliasCategory)) {
                                System.out.printf("GitHub shortcut '%s' has '%s'%s category, while alias '%s' has '%s'%s category,\n" +
                                                "   shortcut defined in %s:%d\n" +
                                                "   alias defined in %s:%d\n"
                                        , shortcut, category, forcedShortcutText
                                        , aliasShortcut, aliasCategory, forcedAliasShortcutText
                                        , githubEmojiCategoryMapFile.getAbsolutePath(), githubEmojiCategoryLineMap.get(shortcut)
                                        , githubEmojiCategoryMapFile.getAbsolutePath(), githubEmojiCategoryLineMap.get(aliasShortcut)
                                );
                            }
                        }
                    } else {
                        System.out.printf("GitHub shortcut '%s' has no aliasShortcuts, defined in %s:%d\n", shortcut
                                , githubEmojiCategoryMapFile.getAbsolutePath(), githubEmojiCategoryLineMap.get(shortcut)
                        );
                    }
                }
            }

            // add custom GitHub shortcuts/URLs
            for (String fileName : githubFileSet) {
                // Add the image file name to the JSON object

                ArrayList<String> shortcuts = null;
                String githubUrl = null;
                String unicodeChars = null;

                if (fileNameToShortcutsMap.containsKey(fileName)) {
                    shortcuts = fileNameToShortcutsMap.get(fileName);
                }

                if (fileNameToGitHubUrlMap.containsKey(fileName)) {
                    githubUrl = fileNameToGitHubUrlMap.get(fileName);

                    if (githubUrl.startsWith("unicode/")) {
                        // create a unicode sequence from characters
                        int lastPos = githubUrl.lastIndexOf(".png");

                        unicodeChars = "U+" + githubUrl.substring("unicode/".length(), lastPos).replaceAll("-", " U+").toUpperCase();
                    }
                }

                // use emoji cheat sheet categories
                String category = null;
                if (shortcuts != null) {
                    for (String shortcut : shortcuts) {
                        if (shortcutToCategoryMap.containsKey(shortcut)) {
                            category = shortcutToCategoryMap.get(shortcut);
                            break;
                        }
                    }
                }

                Emoji emoji = new Emoji();
                emoji.emojiNumber = maxEmojiNumber++;
                emoji.category = category == null || category.isEmpty() ? "github" : category;
                emoji.subcategory = "custom";
                emoji.unicodeChars = unicodeChars;
                emoji.shortcuts = shortcuts;
                emoji.githubFile = githubUrl;
                emoji.unicodeSampleFile = fileName;

                if (emoji.category.equals("github")) {
                    for (String shortcut : shortcuts) {
                        if (shortcutToCategoryMap.containsKey(shortcut)) {
                            String categoryText = shortcutToCategoryMap.get(shortcut);
                        }
                        System.out.printf("GitHub shortcut without category: shortcutToCategoryMap.put( \"%s\",\"github\");\n", shortcut);
                    }
                }

                emojiList.add(emoji);

                // add the emoji-cheatsheet entry if available
                if (emojiCheatsheetFileMap.containsKey(fileName)) {
                    String emojiCheatsheetFileName = emojiCheatsheetFileMap.get(fileName);
                    emojiCheatsheetFileSet.remove(emojiCheatsheetFileName);
                    processEmojiCheatsheetFile(emojiCheatsheetFileName, fileName, emoji);
                }
            }

            // add custom emoji-cheatsheet shortcuts/files
            for (String fileName : emojiCheatsheetFileSet) {
                // Add the image file name to the JSON object
                String emojiShortcut = removeSuffix_PNG(fileName);

                if (fileNameToShortcutsMap.containsKey(fileName)) {
                    System.out.printf("Unexpected state: emoji-cheatsheet file '%s' matches common file name '%s', but was not processed\n", fileName, fileName);
                }

                // use emoji cheat sheet categories
                String category = shortcutToCategoryMap.getOrDefault(emojiShortcut, "emoji-cheat-sheet");

                Emoji emoji = new Emoji();
                emoji.emojiNumber = maxEmojiNumber++;
                emoji.category = category;
                emoji.subcategory = "custom";
                emoji.shortcuts = emojiCheatSheetFileToShortcutsMap.containsKey(fileName)
                        ? Arrays.asList(emojiCheatSheetFileToShortcutsMap.get(fileName))
                        : Collections.singletonList(emojiShortcut);
                emoji.unicodeSampleFile = fileName;

                emojiList.add(emoji);

                if (emoji.category.equals("emoji-cheat-sheet")) {
                    System.out.printf("emoji-cheat-sheet shortcut without category: shortcutToCategoryMap.put( \"%s\",\"emoji-cheat-sheet\");\n", emojiShortcut);
                }

                // add the emoji-cheatsheet
                processEmojiCheatsheetFile(fileName, fileName, emoji);
            }

            HashMap<String, Emoji> shortcutToEmojiMap = new HashMap<>();
            ArrayList<Emoji> noShortcutEmojiList = new ArrayList<>();
            ArrayList<String> shortcutList = new ArrayList<>();

            for (Emoji emoji : emojiList) {
                if (emoji.shortcuts != null && !emoji.shortcuts.isEmpty()) {
                    for (String shortcut : emoji.shortcuts) {
                        shortcutToEmojiMap.put(shortcut, emoji);
                        shortcutList.add(shortcut);
                    }
                } else {
                    noShortcutEmojiList.add(emoji);
                }
            }

            shortcutList.sort(String::compareToIgnoreCase);

            // generate reference files
            MutableJsObject emojiImagesJson = MutableJson.objectFrom("{}");
            emojiImagesJson.put("githubURL", GITHUB_EMOJI_URL_PATH);

            MutableJsArray emojiList = MutableJson.arrayFrom("[]");
            emojiImagesJson.put("emojiList", emojiList);

            for (String shortcut : shortcutList) {
                Emoji emoji = shortcutToEmojiMap.get(shortcut);
                MutableJsObject emojiJson = MutableJson.objectFrom("{}");
                emojiJson.put("shortcut", shortcut);

                if (emoji.shortcuts.size() > 1) {
                    MutableJsArray shortcutsAliasList = MutableJson.arrayFrom("[]");
                    for (String shortcutAlias : emoji.shortcuts) {
                        if (!shortcutAlias.equals(shortcut)) {
                            shortcutsAliasList.add(shortcutAlias);
                        }
                    }
                    emojiJson.put("aliasShortcuts", shortcutsAliasList);
                }

                if (emoji.category != null) emojiJson.put("category", emoji.category);
                if (emoji.subcategory != null) emojiJson.put("subcategory", emoji.subcategory);
                if (emoji.emojiCheatSheetFile != null) emojiJson.put("emojiCheatSheetFile", emoji.emojiCheatSheetFile);
                if (emoji.githubFile != null) emojiJson.put("githubFile", emoji.githubFile);
                if (emoji.unicodeChars != null) emojiJson.put("unicodeChars", emoji.unicodeChars);
                if (emoji.unicodeSampleFile != null) emojiJson.put("unicodeSampleFile", emoji.unicodeSampleFile);
                if (emoji.unicodeCldr != null) emojiJson.put("unicodeCldr", emoji.unicodeCldr);

                emojiList.add(emojiJson);
            }

            for (Emoji emoji : noShortcutEmojiList) {
                MutableJsObject emojiJson = MutableJson.objectFrom("{}");
                emojiJson.put("emojiNumber", emoji.emojiNumber);
                if (emoji.category != null) emojiJson.put("category", emoji.category);
                if (emoji.subcategory != null) emojiJson.put("subcategory", emoji.subcategory);
                if (emoji.emojiCheatSheetFile != null) emojiJson.put("emojiCheatSheetFile", emoji.emojiCheatSheetFile);
                if (emoji.githubFile != null) emojiJson.put("githubFile", emoji.githubFile);
                if (emoji.unicodeChars != null) emojiJson.put("unicodeChars", emoji.unicodeChars);
                if (emoji.unicodeSampleFile != null) emojiJson.put("unicodeSampleFile", emoji.unicodeSampleFile);
                if (emoji.unicodeCldr != null) emojiJson.put("unicodeCldr", emoji.unicodeCldr);

                if (emoji.browserTypes != null) {
                    MutableJsArray browserTypeList = MutableJson.arrayFrom("[]");
                    for (String browserType : emoji.browserTypes) {
                        browserTypeList.add(browserType);
                    }
                    emojiJson.put("browserTypes", browserTypeList);
                }

                emojiList.add(emojiJson);
            }

            // generate tab separated reference file
            // shortcut \t category \t emojiCheatSheetFile \t githubFile \t unicodeChars \t unicodeSampleFile \t unicodeCldr
            StringBuilder sb = new StringBuilder();
            sb.append("shortcut\tcategory\temojiCheatSheetFile\tgithubFile\tunicodeChars\tunicodeSampleFile\tunicodeCldr\tsubcategory\taliasShortcuts\tbrowserTypes\n");
            int emojiCount = 0;

            for (String shortcut : shortcutList) {
                emojiCount++;

                Emoji emoji = shortcutToEmojiMap.get(shortcut);
                sb.append(shortcut)
                        .append("\t")
                        .append(emoji.categoryOrSpace())
                        .append("\t")
                        .append(emoji.emojiCheatSheetFileOrSpace())
                        .append("\t")
                        .append(emoji.githubFileOrSpace())
                        .append("\t")
                        .append(emoji.unicodeCharsOrSpace())
                        .append("\t")
                        .append(emoji.unicodeSampleFileOrSpace())
                        .append("\t")
                        .append(emoji.unicodeCldrOrSpace())
                        .append("\t")
                        .append(emoji.subcategoryOrSpace())
                ;

                sb.append("\t");
                if (emoji.shortcuts != null && emoji.shortcuts.size() > 1) {
                    String delim = "";
                    for (String shortcutAlias : emoji.shortcuts) {
                        if (!shortcutAlias.equals(shortcut)) {
                            sb.append(delim).append(shortcutAlias);
                            delim = ",";
                        }
                    }
                } else {
                    sb.append(" ");
                }

                sb.append("\t");
                if (emoji.browserTypes != null && emoji.browserTypes.size() > 0) {
                    String delim = "";
                    for (String browserType : emoji.browserTypes) {
                        sb.append(delim).append(browserType);
                        delim = ",";
                    }
                } else {
                    sb.append(" ");
                }

                sb.append("\n");
            }

            for (Emoji emoji : noShortcutEmojiList) {
                emojiCount++;
                sb.append(" ")
                        .append("\t")
                        .append(emoji.categoryOrSpace())
                        .append("\t")
                        .append(emoji.emojiCheatSheetFileOrSpace())
                        .append("\t")
                        .append(emoji.githubFileOrSpace())
                        .append("\t")
                        .append(emoji.unicodeCharsOrSpace())
                        .append("\t")
                        .append(emoji.unicodeSampleFileOrSpace())
                        .append("\t")
                        .append(emoji.unicodeCldrOrSpace())
                        .append("\t")
                        .append(emoji.subcategoryOrSpace());

                sb.append("\t ");
                sb.append("\t");
                if (emoji.browserTypes != null && emoji.browserTypes.size() > 0) {
                    String delim = "";
                    for (String browserType : emoji.browserTypes) {
                        sb.append(delim).append(browserType);
                        delim = ",";
                    }
                } else {
                    sb.append(" ");
                }

                sb.append("\n");
            }

            // save JSON file
            try (FileWriter writer = new FileWriter(EMOJI_REFERENCE_JSON_PATH)) {
                writer.write(emojiImagesJson.toString());
            }

            // save TXT file
            try (FileWriter writer = new FileWriter(EMOJI_REFERENCE_TXT_PATH)) {
                writer.write(sb.toString());
            }

            System.out.printf("%d Emoji images and reference files saved successfully.\n", emojiCount);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @NotNull
    private static String removeSuffix_PNG(String fileName) {
        return fileName.endsWith(".png") ? fileName.substring(0, fileName.length() - ".png".length()) : fileName;
    }

    private static void processEmojiCheatSheetShortcuts(String htmlFilePath) throws IOException {
        File htmlFile = new File(htmlFilePath);
        Document doc = Jsoup.parse(htmlFile, "UTF-8");

        // Find all tables in the page
        Elements listItems = doc.select("li");

        // Create a BoxedJson object for storing the image file names
        String category = null;

        for (Element listItem : listItems) {
            Element parent = listItem.parent();
            if (parent == null) continue;

            String listId = parent.id();
            if (!listId.startsWith("emoji-")) continue;
            category = listId.substring("emoji-".length());

            Element img = listItem.getElementsByTag("img").first();
            if (img == null) continue;

            String imgSrc = img.attr("src");
            if (!imgSrc.startsWith(GRAPHICS_EMOJIS_PREFIX)) continue;

            String fileName = imgSrc.substring(GRAPHICS_EMOJIS_PREFIX.length());
            Element nameSpan = listItem.getElementsByTag("span").first();
            String shortcut = nameSpan != null ? nameSpan.text() : removeSuffix_PNG(fileName);
            String aliasShortcuts = nameSpan != null ? shortcut + ", " + nameSpan.attr("data-alternative-name") : shortcut;
            String[] shortcuts = aliasShortcuts.split(", ");

            if (emojiCheatsheetFileAliasMap.containsKey(fileName)) {
                // skip aliased files
                continue;
            }

            emojiCheatSheetFileToShortcutsMap.put(fileName, shortcuts);
            shortcutToCategoryMap.put(shortcut, category);
            for (String shortcutText : shortcuts) {
                shortcutToCategoryMap.put(shortcutText, category);
            }
        }
    }

    private static int processUnicodeEmojiTable(int maxEmojiNumber, String htmlFilePath) throws IOException {
        File htmlFile = new File(htmlFilePath);
        Document doc = Jsoup.parse(htmlFile, "UTF-8");

        // Find all tables in the page
        Element table = doc.select("table").first();

        // Create a BoxedJson object for storing the image file names
        String category = null;
        String unicodeCategory = null;
        String subcategory = null;
        Element categoryHeader = null;
        Element subcategoryHeader = null;
        int emojiNumberColumnIndex = -1;
        int cldrShortNameColumnIndex = -1;
        int codeColumnIndex = -1;
        String[] browserTypes = new String[50];
        int browserTypeCount = 0;
        int browserColumn = -1;

        if (table != null) {
            for (Element tbody : table.children()) {
                if (!tbody.tagName().equals("tbody")) continue;

                for (Element element : tbody.children()) {
                    if (!element.tagName().equals("tr")) {
                        continue;
                    }

                    Element firstChild = element.firstElementChild();
                    if (firstChild == null) {
                        continue;
                    }

                    if (firstChild.tagName().equals("th")) {
                        // headers
                        String colSpan = firstChild.attr("colspan");
                        if (!colSpan.isEmpty() && !colSpan.equals("1")) {
                            categoryHeader = subcategoryHeader;
                            subcategoryHeader = firstChild;
                        } else {
                            // Get the column index of the "№" and "CLDR short name" columns
                            emojiNumberColumnIndex = -1;
                            cldrShortNameColumnIndex = -1;
                            codeColumnIndex = -1;
                            int columnIndex = 0;
                            boolean haveBrowser = false;
                            browserTypeCount = 0;

                            for (Element column : element.children()) {
                                if (column.text().equalsIgnoreCase("№")) {
                                    emojiNumberColumnIndex = columnIndex;
                                } else if (column.text().equalsIgnoreCase("Code")) {
                                    codeColumnIndex = columnIndex;
                                } else if (column.text().equalsIgnoreCase("CLDR Short Name")) {
                                    cldrShortNameColumnIndex = columnIndex;
                                    break;
                                } else if (column.text().equalsIgnoreCase("Browser")) {
                                    haveBrowser = true;
                                } else if (haveBrowser) {
                                    // get browser type
                                    if (browserTypeCount >= browserTypes.length) {
                                        break;
                                    }

                                    browserTypes[browserTypeCount++] = column.text().toLowerCase();

                                    if (browserColumn == -1) {
                                        browserColumn = columnIndex;
                                    }
                                }
                                columnIndex++;
                            }

                            if (emojiNumberColumnIndex == -1) {
                                System.out.println("Emoji number column not found in the table. Skipping table section.");
                            }
                        }
                    } else if (firstChild.tagName().equals("td")) {
                        if (emojiNumberColumnIndex == -1) {
                            continue;
                        }

                        if (categoryHeader != null) {
                            unicodeCategory = categoryHeader.text();
                            if (!categoryMap.containsKey(unicodeCategory)) {
                                System.out.printf("Missing categoryMap entry for Unicode category '%s'\n", unicodeCategory);
                                categoryMap.put(unicodeCategory, unicodeCategory.toLowerCase().replace(" ", ""));
                            }
                            category = categoryMap.get(unicodeCategory);
                            categoryHeader = null;
                        }

                        if (subcategoryHeader != null) {
                            subcategory = subcategoryHeader.text();
                            subcategoryHeader = null;
                        }

                        Elements columns = element.children();
                        int[] columnMap = new int[cldrShortNameColumnIndex + 1];
                        int index = 0;
                        int columnIndex = 0;
                        int logicalSize = 0;

                        for (Element column : columns) {
                            String colspan = column.attr("colspan");

                            if (!colspan.isEmpty() && !colspan.equals("1")) {
                                int columnSpan = Integer.parseInt(colspan);
                                while (columnSpan-- > 0) {
                                    columnMap[index++] = columnIndex;
                                    logicalSize++;
                                }
                            } else {
                                columnMap[index++] = columnIndex;
                                logicalSize++;
                            }
                            columnIndex++;
                        }

                        if (logicalSize <= emojiNumberColumnIndex || logicalSize <= cldrShortNameColumnIndex) {
                            continue;
                        } else {  // Check if the row has at least 5 columns
                            String emojiNumber = columns.get(columnMap[emojiNumberColumnIndex]).text();
                            int emojiIntNumber = Integer.parseInt(emojiNumber);

                            if (maxEmojiNumber < emojiIntNumber) {
                                maxEmojiNumber = emojiIntNumber;
                            }

                            String codeString = "";
                            if (codeColumnIndex != -1) {
                                Element aTag = columns.get(columnMap[codeColumnIndex]).getElementsByTag("a").first();
                                if (aTag != null) {
                                    codeString = aTag.text();
                                }
                            }
                            String cldrShortName = cldrShortNameColumnIndex != -1 ? columns.get(columnMap[cldrShortNameColumnIndex]).text() : "";
                            String unicodeCharsName = codeString.isEmpty() ? emojiNumber : codeString.replaceAll("U\\+", "-").toLowerCase();
                            unicodeCharsName = unicodeCharsName.replaceAll(" ", "");

                            if (unicodeCharsName.startsWith("-")) {
                                unicodeCharsName = unicodeCharsName.substring(1);
                            }

                            String fileName = unicodeCharsName + ".png";

                            // Add the emoji to the list
                            Emoji emoji = new Emoji();
                            emoji.emojiNumber = emojiIntNumber;
                            emoji.category = category;
                            emoji.subcategory = subcategory;
                            emoji.unicodeCldr = cldrShortName;
                            if (!codeString.isEmpty()) emoji.unicodeChars = codeString;
                            emoji.unicodeSampleFile = fileName;

                            if (fileNameToShortcutsMap.containsKey(fileName)) {
                                emoji.shortcuts = fileNameToShortcutsMap.get(fileName);
                            }

                            if (fileNameToGitHubUrlMap.containsKey(fileName)) {
                                emoji.githubFile = fileNameToGitHubUrlMap.get(fileName);
                            }

                            githubFileSet.remove(fileName);

                            emojiList.add(emoji);

                            for (int column = 0; column < browserTypeCount; column++) {
                                if (column + browserColumn >= logicalSize) break;

                                String emojiImageUrl = columns.get(columnMap[column + browserColumn]).select("img").attr("src");
                                if (!emojiImageUrl.isEmpty()) {
                                    String browserType = browserTypes[column];
                                    // Create subdirectory for each browser type
                                    Path browserPath = Paths.get(outputDirectory, browserType);
                                    Files.createDirectories(browserPath);

                                    // Generate a unique filename for each image using the fileName
                                    Path imagePath = browserPath.resolve(fileName);

                                    // Download and save the emoji image
                                    saveEmojiImage(emojiImageUrl, imagePath);
                                    if (emoji.browserTypes == null) emoji.browserTypes = new ArrayList<>();
                                    emoji.browserTypes.add(browserType);
                                }
                            }

                            // add the emoji-cheatsheet entry if available
                            if (emojiCheatsheetFileMap.containsKey(fileName)) {
                                String emojiCheatsheetFileName = emojiCheatsheetFileMap.get(fileName);

                                emojiCheatsheetFileSet.remove(emojiCheatsheetFileName);
                                processEmojiCheatsheetFile(emojiCheatsheetFileName, fileName, emoji);
                            }
                        }
                    }
                }
            }
        }
        return maxEmojiNumber;
    }

    private static void processEmojiCheatsheetFile(String emojiCheatsheetFileName, String fileName, Emoji emoji) throws IOException {
        if (emojiCheatsheetFileAliasMap.containsKey(emojiCheatsheetFileName)) {
            // skip aliased files
            return;
        }

        File sourceFile = new File(emojiCheatsheetDirectoryPath, emojiCheatsheetFileName);
        File destinationDirectory = new File(outputDirectory, EMOJI_CHEAT_SHEET_SUBDIRECTORY);

        if (!destinationDirectory.exists()) {
            destinationDirectory.mkdirs();
        }

        Path sourcePath = sourceFile.toPath();
        Path destinationPath = new File(destinationDirectory, fileName).toPath();
        Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);

        emoji.emojiCheatSheetFile = emojiCheatsheetFileName;
    }

    private static void downloadPage(String url, String filePath) throws IOException {
        System.out.printf("Downloading URL '%s' to '%s' ", url, filePath);

        URL pageUrl = new URL(url);
        int count = DOWNLOAD_UPDATE;
        try (BufferedInputStream in = new BufferedInputStream(pageUrl.openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(filePath)) {

            byte[] dataBuffer = new byte[DOWNLOAD_BUFFER];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, dataBuffer.length)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
                if (--count == 0) {
                    System.out.print(".");
                    count = DOWNLOAD_UPDATE;
                }
            }
        }
        System.out.print(" done.\n");
    }

    private static void deleteDirectoryContents(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();

            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectoryContents(file);

                        // Delete the empty directory
                        file.delete();
                        System.out.println("Directory deleted: " + file.getPath());
                    } else {
                        file.delete();
                    }
                }
            }
        }
    }

    private static void saveEmojiImage(String imageUrl, Path outputPath) throws IOException {
        if (imageUrl.startsWith("data:image/png;base64,")) {
            String base64Image = imageUrl.split(",")[1]; // Extract the Base64 encoded image data

            byte[] imageBytes = Base64.getDecoder().decode(base64Image);

            try (FileOutputStream fileOutputStream = new FileOutputStream(outputPath.toFile())) {
                fileOutputStream.write(imageBytes);
            }
        } else {
            URL url = new URL(imageUrl);
            try (BufferedInputStream in = new BufferedInputStream(url.openStream());
                 FileOutputStream fileOutputStream = new FileOutputStream(outputPath.toFile())) {

                byte[] dataBuffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                    fileOutputStream.write(dataBuffer, 0, bytesRead);
                }
            }
        }
    }
}
