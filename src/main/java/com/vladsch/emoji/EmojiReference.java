package com.vladsch.emoji;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class EmojiReference {
    public static final String EMOJI_REFERENCE_TXT = "/EmojiReference.txt"; // resource path to text data file
    public static final String githubUrl = "https://assets-cdn.github.com/images/icons/emoji/";

    public static class Emoji {
        public final String shortcut;
        public final String category;
        public final String emojiCheatSheetFile; // name part of the file no extension
        public final String githubFile; // name part of the file no extension
        public final String unicodeChars; // unicode char codes space separated list
        public final String unicodeSampleFile; // name part of the file no extension
        public final String unicodeCldr;

        public Emoji(final String shortcut, final String category, final String emojiCheatSheetFile, final String githubFile, final String unicodeChars, final String unicodeSampleFile, final String unicodeCldr) {
            this.shortcut = shortcut;
            this.category = category;
            this.emojiCheatSheetFile = emojiCheatSheetFile;
            this.githubFile = githubFile;
            this.unicodeChars = unicodeChars;
            this.unicodeSampleFile = unicodeSampleFile;
            this.unicodeCldr = unicodeCldr;
        }
    }


    private static ArrayList<Emoji> emojiList = null;

    public static List<Emoji> getEmojiList() {
        if (emojiList == null) {
            // read it in
            emojiList = new ArrayList<>(3000);

            // NOTE:
            final String emojiReference = EMOJI_REFERENCE_TXT;
            InputStream stream = EmojiReference.class.getResourceAsStream(emojiReference);

            if (stream == null) {
                throw new IllegalStateException("Could not load " + emojiReference + " classpath resource");
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            String line;
            try {
                // skip first line, it is column names
                line = reader.readLine();
                while ((line = reader.readLine()) != null) {
                    String[] fields = line.split("\t");
                    try {

                        final Emoji emoji = new Emoji(
                                fields[0].charAt(0) == ' ' ? null : fields[0], // shortcut
                                fields[1].charAt(0) == ' ' ? null : fields[1], // category
                                fields[2].charAt(0) == ' ' ? null : fields[2], // emojiCheatSheetFile
                                fields[3].charAt(0) == ' ' ? null : fields[3], // githubFile
                                fields[4].charAt(0) == ' ' ? null : fields[4], // unicodeChars
                                fields[5].charAt(0) == ' ' ? null : fields[5], // unicodeSampleFile
                                fields[6].charAt(0) == ' ' ? null : fields[6] // unicodeCldr
                        );
                        emojiList.add(emoji);

                        //if (emoji.shortcut != null && emoji.unicodeChars == null) {
                        //    String type = emoji.githubFile == null ? "cheatSheet " : (emoji.emojiCheatSheetFile == null ? "gitHub " : "");
                        //    System.out.printf("Non unicode %sshortcut %s\n",type, emoji.shortcut);
                        //}
                    } catch (ArrayIndexOutOfBoundsException e) {
                        //e.printStackTrace();
                        throw new IllegalStateException("Error processing EmojiReference.txt", e);
                    }
                }
            } catch (IOException e) {
                //e.printStackTrace();
                throw new IllegalStateException("Error processing EmojiReference.txt", e);
            }
        }

        return emojiList;
    }
}
