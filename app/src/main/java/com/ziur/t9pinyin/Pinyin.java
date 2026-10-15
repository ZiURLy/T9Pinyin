package com.ziur.t9pinyin;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

final class Pinyin {
    private static final String ASSET = "assets/pinyin.bin";
    static final Pinyin INSTANCE = new Pinyin();
    private static final int MAX_CACHED = 4000;
    private static final int MAX_NAME = 200;
    private boolean failed;
    private PinyinTable table;
    private final HashMap<String, NameToken[]> tokens = new HashMap<>();
    private final T9Matcher matcher = new T9Matcher();

    private Pinyin() {
    }

    synchronized void use(PinyinTable pinyinTable) {
        this.table = pinyinTable;
        this.failed = pinyinTable == null;
        this.tokens.clear();
    }

    synchronized void reload() {
        this.table = null;
        this.failed = false;
        this.tokens.clear();
    }

    synchronized boolean ready() {
        if (this.table == null && !this.failed) {
            try {
                this.table = load();
            } catch (Throwable th) {
                this.failed = true;
                Diagnostics.error("cannot load the pinyin table", th);
            }
        }
        return this.table != null;
    }

    synchronized String match(String str, String str2) {
        return find(str, str2) == null ? null : this.matcher.spell();
    }

    synchronized int[] range(String str, String str2) {
        NameToken[] nameTokenArrFind;
        nameTokenArrFind = find(str, str2);
        return nameTokenArrFind == null ? null : new int[]{nameTokenArrFind[this.matcher.startToken].start, nameTokenArrFind[this.matcher.endToken - 1].end};
    }

    private NameToken[] find(String str, String str2) {
        if (str == null || str2 == null || str2.isEmpty() || str.length() > MAX_NAME || !T9.isDigits(str2) || isAscii(str) || !ready()) {
            return null;
        }
        NameToken[] nameTokenArr = this.tokens.get(str);
        if (nameTokenArr == null) {
            nameTokenArr = NameToken.tokenize(str, this.table);
            if (this.tokens.size() >= MAX_CACHED) {
                this.tokens.clear();
            }
            this.tokens.put(str, nameTokenArr);
        }
        if (this.matcher.find(nameTokenArr, str2, true)) {
            return nameTokenArr;
        }
        return null;
    }

    synchronized boolean hasOtherWords(String str) {
        boolean z = true;
        synchronized (this) {
            if (ready()) {
                int iCharCount = 0;
                while (true) {
                    if (iCharCount >= str.length()) {
                        z = false;
                        break;
                    }
                    int iCodePointAt = str.codePointAt(iCharCount);
                    if (this.table.readings(iCodePointAt).length == 0 && (Character.isLetterOrDigit(iCodePointAt) || (iCodePointAt >= 192 && iCodePointAt <= 591))) {
                        break;
                    }
                    iCharCount += Character.charCount(iCodePointAt);
                }
            }
        }
        return z;
    }

    private static boolean isAscii(String str) {
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) >= 128) {
                return false;
            }
        }
        return true;
    }

    private static PinyinTable load() throws IOException {
        PinyinTable pinyinTableLoad;
        ClassLoader classLoader = Pinyin.class.getClassLoader();
        InputStream resourceAsStream = classLoader == null ? null : classLoader.getResourceAsStream(ASSET);
        if (resourceAsStream != null) {
            try {
                pinyinTableLoad = PinyinTable.load(resourceAsStream);
            } finally {
                resourceAsStream.close();
            }
        } else {
            String str = Entry.modulePath;
            if (str == null) {
                throw new FileNotFoundException("assets/pinyin.bin (module path unknown)");
            }
            ZipFile zipFile = new ZipFile(str);
            try {
                ZipEntry entry = zipFile.getEntry(ASSET);
                if (entry == null) {
                    throw new FileNotFoundException("assets/pinyin.bin in " + str);
                }
                InputStream inputStream = zipFile.getInputStream(entry);
                try {
                    pinyinTableLoad = PinyinTable.load(inputStream);
                } finally {
                    inputStream.close();
                }
            } finally {
                zipFile.close();
            }
        }
        return pinyinTableLoad;
    }
}
