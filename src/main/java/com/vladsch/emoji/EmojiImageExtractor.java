package com.vladsch.emoji;

import com.vladsch.boxed.json.BoxedJsObject;
import com.vladsch.boxed.json.BoxedJson;
import com.vladsch.boxed.json.MutableJsArray;
import com.vladsch.boxed.json.MutableJsObject;
import com.vladsch.boxed.json.MutableJson;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.json.JsonValue;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class EmojiImageExtractor {

    public static final String EMOJI_CHEAT_SHEET = "EmojiCheatsheet";
    public static final String EMOJI_CHEAT_SHEET_SUBDIRECTORY = "emojis";
    public static final String urlFullEmojiList = "https://unicode.org/emoji/charts/full-emoji-list.html";
    public static final String urlGithubEmojiApi = "https://api.github.com/emojis";
    public static final String outputDirectory = "emoji_images";
    public static final String jsonFilePath = "emoji_images.json";
    public static final String htmlFilePath = "emoji_list.html";
    public static final String githubEmojiApiFilePath = "githubEmojiApi.json";
    public static final String emojiCheatsheetDirectoryPath = "emoji-cheat-sheet.com/public/graphics/emojis";

    public static final HashMap<String, String> emojiCheatsheetAliasMap = new HashMap<>();
    static {
        emojiCheatsheetAliasMap.put("plus1", "+1");
        emojiCheatsheetAliasMap.put("minus1", "-1");
    }
    public static void main(String[] args) {

        try {
            // Download the HTML page and save it to a file
            if (args.length >= 1 && args[0].equals("--download")) {
                downloadPage(urlFullEmojiList, htmlFilePath);
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

            for (Map.Entry<String, JsonValue> entry : githubApi.entrySet()) {
                String gitHubUrl = BoxedJson.of(entry.getValue()).asJsString().getString();
                String shortcut = entry.getKey();
                int startPos = gitHubUrl.lastIndexOf('/');
                int endPos = gitHubUrl.indexOf('?', startPos);

                if (startPos != -1) {
                    String fileName = gitHubUrl.substring(startPos + 1, endPos);
                    ArrayList<String> shortcuts = fileNameToShortcutMap.computeIfAbsent(fileName, keyString -> new ArrayList<>());
                    if (!shortcuts.contains(shortcut)) shortcuts.add(shortcut);
                    fileNameToGitHubUrlMap.put(fileName, gitHubUrl.substring("https://github.githubassets.com/images/icons/emoji/".length()));
                    githubFileSet.add(fileName);
                    shortcutToFileNameMap.put(shortcut, fileName);
                }
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
            File htmlFile = new File(htmlFilePath);
            Document doc = Jsoup.parse(htmlFile, "UTF-8");

            // Find all tables in the page
            Element table = doc.select("table").first();

            // Create a BoxedJson object for storing the image file names
            MutableJsObject emojiImagesJson = MutableJson.objectFrom("{}");
            String category = null;
            String subcategory = null;
            Element categoryHeader = null;
            Element subcategoryHeader = null;
            int emojiNumberColumnIndex = -1;
            int cldrShortNameColumnIndex = -1;
            int codeColumnIndex = -1;
            String[] browserTypes = new String[50];
            int browserTypeCount = 0;
            int browserColumn = -1;

            int maxEmojiNumber = 0;

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

                                        browserTypes[browserTypeCount++] = column.text();

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
                                category = categoryHeader.text();
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

                                // Add the image file name to the JSON object
                                MutableJsObject emojiJson = MutableJson.objectFrom("{}");

                                emojiJson.put("category", category);
                                if (subcategory != null) {
                                    emojiJson.put("subcategory", subcategory);
                                }
                                emojiJson.put("unicodeCldr", cldrShortName);
                                if (!codeString.isEmpty()) {
                                    emojiJson.put("unicodeChars", codeString);
                                }

                                if (fileNameToShortcutMap.containsKey(fileName)) {
                                    ArrayList<String> shortcuts = fileNameToShortcutMap.get(fileName);
                                    if (shortcuts.size() == 1) {
                                        emojiJson.put("shortcut", shortcuts.get(0));
                                    } else {
                                        MutableJsArray shortcutsArray = MutableJson.arrayFrom("[]");

                                        for (String shortcut : shortcuts) {
                                            shortcutsArray.add(shortcut);
                                        }

                                        emojiJson.put("shortcut", shortcutsArray);
                                    }
                                }

                                if (fileNameToGitHubUrlMap.containsKey(fileName)) {
                                    emojiJson.put("githubURL", fileNameToGitHubUrlMap.get(fileName));
                                }

                                githubFileSet.remove(fileName);

                                emojiImagesJson.put(emojiNumber, emojiJson);

                                for (int column = 0; column < browserTypeCount; column++) {
                                    if (column + browserColumn >= logicalSize) break;

                                    String emojiImageUrl = columns.get(columnMap[column + browserColumn]).select("img").attr("src");
                                    if (!emojiImageUrl.isEmpty()) {
                                        String browserType = browserTypes[column];
                                        // Create subdirectory for each browser type
                                        Path browserPath = Paths.get(outputDirectory, browserType);
                                        Files.createDirectories(browserPath);

                                        // Generate a unique filename for each image using the emoji number
                                        Path imagePath = browserPath.resolve(fileName);

                                        // Download and save the emoji image
                                        saveEmojiImage(emojiImageUrl, imagePath);
                                        emojiJson.put(browserType, browserType + "/" + fileName);
                                    }
                                }

                                // add the emoji-cheatsheet entry if available
                                if (emojiCheatsheetFileMap.containsKey(fileName)) {
                                    String emojiCheatsheetFileName = emojiCheatsheetFileMap.get(fileName);

                                    emojiCheatsheetFileSet.remove(emojiCheatsheetFileName);
                                    processEmojiCheatsheetFile(emojiCheatsheetFileName, fileName, emojiJson);
                                }
                            }
                        }
                    }
                }
            }

            // add custom GitHub shortcuts/URLs
            for (String fileName : githubFileSet) {
                // Add the image file name to the JSON object
                MutableJsObject emojiJson = MutableJson.objectFrom("{}");

                emojiJson.put("category", "custom");
                emojiJson.put("subcategory", "github");
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

                if (unicodeChars != null) {
                    emojiJson.put("unicodeChars", unicodeChars);
                }

                if (shortcuts != null) {
                    if (shortcuts.size() == 1) {
                        emojiJson.put("shortcut", shortcuts.get(0));
                    } else {
                        MutableJsArray shortcutsArray = MutableJson.arrayFrom("[]");

                        for (String shortcut : shortcuts) {
                            shortcutsArray.add(shortcut);
                        }

                        emojiJson.put("shortcut", shortcutsArray);
                    }
                }

                if (githubUrl != null) {
                    emojiJson.put("githubURL", githubUrl);
                }

                // add the emoji-cheatsheet entry if available
                if (emojiCheatsheetFileMap.containsKey(fileName)) {
                    String emojiCheatsheetFileName = emojiCheatsheetFileMap.get(fileName);
                    emojiCheatsheetFileSet.remove(emojiCheatsheetFileName);
                    processEmojiCheatsheetFile(emojiCheatsheetFileName, fileName, emojiJson);
                }

                emojiImagesJson.put(String.valueOf(maxEmojiNumber++), emojiJson);
            }

            // add custom emoji-cheatsheet shortcuts/files
            for (String fileName : emojiCheatsheetFileSet) {
                // Add the image file name to the JSON object
                MutableJsObject emojiJson = MutableJson.objectFrom("{}");
                String emojiShortcut = fileName.endsWith(".png") ? fileName.substring(0, fileName.length() - ".png".length()) : fileName;

                if (fileNameToShortcutMap.containsKey(fileName)) {
                    System.out.printf("Unexpected state: emoji-cheatsheet file '%s' matches common file name '%s', but was not processed\n", fileName, fileName);
                }

                System.out.printf("Custom EmojiCheatsheet file '%s' for shortcut '%s'\n", fileName, emojiShortcut);

                emojiJson.put("category", "custom");
                emojiJson.put("subcategory", "emoji-cheatsheet");

                emojiJson.put("shortcut", emojiShortcut);

                // add the emoji-cheatsheet
                processEmojiCheatsheetFile(fileName, fileName, emojiJson);
                emojiImagesJson.put(String.valueOf(maxEmojiNumber++), emojiJson);
            }

            // Generate the JSON file
            try (FileWriter writer = new FileWriter(jsonFilePath)) {
                writer.write(emojiImagesJson.toString());
            }

            System.out.println("Emoji images and JSON file saved successfully!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void processEmojiCheatsheetFile(String emojiCheatsheetFileName, String fileName, MutableJsObject emojiJson) throws IOException {
        File sourceFile = new File(emojiCheatsheetDirectoryPath, emojiCheatsheetFileName);
        File destinationDirectory = new File(outputDirectory, EMOJI_CHEAT_SHEET_SUBDIRECTORY);

        if (!destinationDirectory.exists()) {
            destinationDirectory.mkdirs();
        }

        Path sourcePath = sourceFile.toPath();
        Path destinationPath = new File(destinationDirectory, fileName).toPath();
        Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);

        emojiJson.put(EMOJI_CHEAT_SHEET, EMOJI_CHEAT_SHEET_SUBDIRECTORY + "/" + fileName);
    }

    private static void downloadPage(String url, String filePath) throws IOException {
        URL pageUrl = new URL(url);
        try (BufferedInputStream in = new BufferedInputStream(pageUrl.openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(filePath)) {

            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
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
