package com.vladsch.emoji;

import com.vladsch.boxed.json.BoxedJsObject;
import com.vladsch.boxed.json.BoxedJson;
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
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class EmojiImageExtractor {
    public static void main(String[] args) {
        String urlFullEmojiList = "https://unicode.org/emoji/charts/full-emoji-list.html";
        String urlGithubEmojiApi = "https://api.github.com/emojis";
        String outputDirectory = "emoji_images";
        String jsonFilePath = "emoji_images.json";
        String htmlFilePath = "emoji_list.html";
        String githubEmojiApiFilePath = "githubEmojiApi.json";

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
            HashMap<String, String> fileNameToShortcutMap = new HashMap<>();
            HashMap<String, String> fileNameToGitHubUrlMap = new HashMap<>();
            HashSet<String> foundFileNames = new HashSet<>();

            for (Map.Entry<String, JsonValue> entry : githubApi.entrySet()) {
                String gitHubUrl = BoxedJson.of(entry.getValue()).asJsString().getString();
                String shortcut = entry.getKey();
                int startPos = gitHubUrl.lastIndexOf('/');
                int endPos = gitHubUrl.indexOf('?', startPos);

                if (startPos != -1) {
                    String fileName = gitHubUrl.substring(startPos + 1, endPos);
                    fileNameToShortcutMap.put(fileName, shortcut);
                    fileNameToGitHubUrlMap.put(fileName, gitHubUrl.substring("https://github.githubassets.com/images/icons/emoji/".length()));
                    foundFileNames.add(fileName);
                }
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
                            String colspan = firstChild.attr("colspan");
                            if (!colspan.isEmpty() && !colspan.equals("1")) {
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
                                    emojiJson.put("shortcut", fileNameToShortcutMap.get(fileName));
                                }

                                if (fileNameToGitHubUrlMap.containsKey(fileName)) {
                                    emojiJson.put("githubURL", fileNameToGitHubUrlMap.get(fileName));
                                }

                                foundFileNames.remove(fileName);

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
                            }
                        }
                    }
                }
            }

            // add custom GitHub shortcuts/URLs
            for (String fileName : foundFileNames) {
                // Add the image file name to the JSON object
                MutableJsObject emojiJson = MutableJson.objectFrom("{}");

                emojiJson.put("category", "custom");
                emojiJson.put("subcategory", "github");
                String shortcut = null;
                String githubUrl = null;
                String unicodeChars = null;

                if (fileNameToShortcutMap.containsKey(fileName)) {
                    shortcut = fileNameToShortcutMap.get(fileName);
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

                if (shortcut != null) {
                    emojiJson.put("shortcut", shortcut);
                }

                if (githubUrl != null) {
                    emojiJson.put("githubURL", githubUrl);
                }

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
