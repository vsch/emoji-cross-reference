package com.vladsch.emoji;

import com.vladsch.boxed.json.*;
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

public class EmojiImageExtractor {

    public static final String EMOJI_CHEAT_SHEET = "EmojiCheatsheet";
    public static final String EMOJI_CHEAT_SHEET_SUBDIRECTORY = "emojis";
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
    public static final String githubEmojiApiFilePath = "githubEmojiApi.json";
    public static final String emojiCheatsheetDirectoryPath = "emoji-cheat-sheet.com/public/graphics/emojis";
    public static final String GITHUB_EMOJI_URL_PATH = "https://github.githubassets.com/images/icons/emoji/";

    public static final HashMap<String, String> emojiCheatsheetAliasMap = new HashMap<>();
    public static final int DOWNLOAD_BUFFER = 65536;
    public static final int DOWNLOAD_UPDATE = 10;

    static {
        emojiCheatsheetAliasMap.put("+1", "plus1");
        emojiCheatsheetAliasMap.put("-1", "minus1");
    }

    /**
     * Map of Unicode Emoji List Category Header to reference category map, manually maintained
     */
    public static final HashMap<String, String> categoryMap = new HashMap<>();

    static {
        categoryMap.put("Smileys & Emotion", "smileys/emotion");
        categoryMap.put("People & Body", "people/body");
        categoryMap.put("Component", "component");
        categoryMap.put("Animals & Nature", "animals/nature");
        categoryMap.put("Food & Drink", "food/drink");
        categoryMap.put("Travel & Places", "travel/places");
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

    public static void main(String[] args) {
        boolean downloadFullEmoji = false;
        boolean downloadFullEmojiModifiers = false;
        boolean downloadGitHubApi = false;
        boolean downloadGitHubImages = false;
        boolean noModifiers = false;

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

            File githubEmojiApiFile = new File(githubEmojiApiFilePath);
            InputStream githubJson = Files.newInputStream(githubEmojiApiFile.toPath());
            BoxedJsObject githubApi = BoxedJson.objectFrom(githubJson);

            // parse and process the githubEmojiApi to extract file name (unicode format when available) and shortcut
            HashMap<String, ArrayList<String>> fileNameToShortcutMap = new HashMap<>();
            HashMap<String, String> fileNameToGitHubUrlMap = new HashMap<>();
            HashSet<String> githubFileSet = new HashSet<>();
            HashMap<String, String> shortcutToFileNameMap = new HashMap<>();

            int githubImages = githubApi.size();
            int githubImageCount = 0;

            Path githubImagePath = Paths.get(GITHUB_IMAGES_DIRECTORY);
            if (downloadGitHubImages) {
                deleteDirectoryContents(githubImagePath.toFile());
                Files.createDirectories(githubImagePath);
            }

            Path githubEmojiPath = Paths.get(outputDirectory, GITHUB_SUBDIRECTORY);

            File emojiDirectory = new File(outputDirectory);
            if (emojiDirectory.exists()) {
                deleteDirectoryContents(emojiDirectory);
            }

            if (downloadGitHubImages) {
                System.out.printf("Downloading GitHub emoji images %d / %d ", githubImageCount, githubImages);
            }

            Files.createDirectories(githubEmojiPath);

            for (Map.Entry<String, JsonValue> entry : githubApi.entrySet()) {
                String gitHubUrl = BoxedJson.of(entry.getValue()).asJsString().getString();
                String shortcut = entry.getKey();
                int startPos = gitHubUrl.lastIndexOf('/');
                int endPos = gitHubUrl.indexOf('?', startPos);

                if (startPos != -1) {
                    String fileName = gitHubUrl.substring(startPos + 1, endPos);
                    ArrayList<String> shortcuts = fileNameToShortcutMap.computeIfAbsent(fileName, keyString -> new ArrayList<>());
                    if (!shortcuts.contains(shortcut)) shortcuts.add(shortcut);
                    String githubUrlPath = gitHubUrl.substring(GITHUB_EMOJI_URL_PATH.length());
                    fileNameToGitHubUrlMap.put(fileName, githubUrlPath);
                    githubFileSet.add(fileName);
                    shortcutToFileNameMap.put(shortcut, fileName);

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
                }
            }

            if (downloadGitHubImages) {
                System.out.print(" done.\n");
            }

            // read the emoji cheat sheet files and map the names as shortcut to common file names for compatibility
            File directory = new File(emojiCheatsheetDirectoryPath);
            HashMap<String, String> emojiCheatsheetFileMap = new HashMap<>();
            HashSet<String> emojiCheatsheetFileSet = new HashSet<>();

            if (directory.exists() && directory.isDirectory()) {
                File[] files = directory.listFiles();

                if (files != null) {
                    for (File file : files) {
                        if (file.isFile()) {
                            // Process the file
                            String emojiCheatsheetFileName = file.getName();
                            String emojiShortcut = emojiCheatsheetFileName.endsWith(".png") ? emojiCheatsheetFileName.substring(0, emojiCheatsheetFileName.length() - ".png".length()) : emojiCheatsheetFileName;

                            if (emojiCheatsheetAliasMap.containsKey(emojiShortcut)) {
                                System.out.printf("EmojiCheatsheet alias '%s' --> '%s'\n", emojiShortcut, emojiCheatsheetAliasMap.get(emojiShortcut));

                                // skip these files
                                continue;
                            }

                            if (shortcutToFileNameMap.containsKey(emojiShortcut)) {
                                // map it to the common file, but only add the first file, ignore the rest
                                String commonFileName = shortcutToFileNameMap.get(emojiShortcut);
                                if (!emojiCheatsheetFileMap.containsKey(commonFileName)) {
                                    emojiCheatsheetFileMap.put(commonFileName, emojiCheatsheetFileName);
                                    emojiCheatsheetFileSet.add(emojiCheatsheetFileName);
                                }
                            } else {
                                // map it to the file name as is
                                System.out.printf("EmojiCheatsheet file '%s', shortcut '%s' not matched.\n", emojiCheatsheetFileName, emojiShortcut);
                                emojiCheatsheetFileMap.put(emojiCheatsheetFileName, emojiCheatsheetFileName);
                                emojiCheatsheetFileSet.add(emojiCheatsheetFileName);
                            }
                        }
                    }
                } else {
                    System.out.printf("No files found in the '%s' directory.\n", emojiCheatsheetDirectoryPath);
                }
            } else {
                System.out.printf("Invalid directory path: '%s'.\n", emojiCheatsheetDirectoryPath);
            }

            // Read the HTML file
            int maxEmojiNumber = processUnicodeEmojiTable(0, FULL_EMOJI_HTML_PATH, fileNameToShortcutMap, fileNameToGitHubUrlMap, githubFileSet, emojiCheatsheetFileMap, emojiCheatsheetFileSet);

            if (!noModifiers) {
                maxEmojiNumber = processUnicodeEmojiTable(maxEmojiNumber, FULL_EMOJI_MODIFIER_HTML_PATH, fileNameToShortcutMap, fileNameToGitHubUrlMap, githubFileSet, emojiCheatsheetFileMap, emojiCheatsheetFileSet);
            }

            // add custom GitHub shortcuts/URLs
            for (String fileName : githubFileSet) {
                // Add the image file name to the JSON object

                ArrayList<String> shortcuts = null;
                String githubUrl = null;
                String unicodeChars = null;

                if (fileNameToShortcutMap.containsKey(fileName)) {
                    shortcuts = fileNameToShortcutMap.get(fileName);
                }

                if (fileNameToGitHubUrlMap.containsKey(fileName)) {
                    githubUrl = fileNameToGitHubUrlMap.get(fileName);

                    if (githubUrl.startsWith("unicode/")) {
                        // create a unicode sequence from characters
                        int lastPos = githubUrl.lastIndexOf(".png");

                        unicodeChars = "U+" + githubUrl.substring("unicode/".length(), lastPos).replaceAll("-", " U+").toUpperCase();
                    }
                }

                String shortcutsText = shortcuts == null ? "" : shortcuts.stream().reduce("", (t1, t2) -> (t1.isEmpty() ? t1 : t1 + ", ") + t2);
                System.out.printf("Custom GitHub emoji file '%s' for shortcut '%s'\n", fileName, shortcutsText);

                Emoji emoji = new Emoji();
                emoji.emojiNumber = maxEmojiNumber++;
                emoji.category = "custom";
                emoji.subcategory = "github";
                emoji.unicodeChars = unicodeChars;
                emoji.shortcuts = shortcuts;
                emoji.githubFile = githubUrl;

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
                String emojiShortcut = fileName.endsWith(".png") ? fileName.substring(0, fileName.length() - ".png".length()) : fileName;

                if (fileNameToShortcutMap.containsKey(fileName)) {
                    System.out.printf("Unexpected state: emoji-cheatsheet file '%s' matches common file name '%s', but was not processed\n", fileName, fileName);
                }

                System.out.printf("Custom EmojiCheatsheet file '%s' for shortcut '%s'\n", fileName, emojiShortcut);

                Emoji emoji = new Emoji();
                emoji.emojiNumber = maxEmojiNumber++;
                emoji.category = "custom";
                emoji.subcategory = "emoji-cheatsheet";
                emoji.shortcuts = Collections.singletonList(emojiShortcut);

                emojiList.add(emoji);

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

    private static int processUnicodeEmojiTable(int maxEmojiNumber, String htmlFilePath, HashMap<String, ArrayList<String>> fileNameToShortcutMap, HashMap<String, String> fileNameToGitHubUrlMap, HashSet<String> githubFileSet, HashMap<String, String> emojiCheatsheetFileMap, HashSet<String> emojiCheatsheetFileSet) throws IOException {
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

                            if (fileNameToShortcutMap.containsKey(fileName)) {
                                emoji.shortcuts = fileNameToShortcutMap.get(fileName);
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
