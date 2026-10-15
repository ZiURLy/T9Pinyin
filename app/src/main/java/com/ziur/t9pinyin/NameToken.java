package com.ziur.t9pinyin;

import java.text.Normalizer;
import java.util.ArrayList;

final class NameToken {
    private static final NameToken[] EMPTY = new NameToken[0];
    private static final int MAX_TOKENS = 64;
    final String[] codes;
    final boolean[] digraph;
    final int end;
    final boolean han;
    final String[] letters;
    final int start;

    private NameToken(int i, int i2, String[] strArr, String[] strArr2, boolean[] zArr, boolean z) {
        this.start = i;
        this.end = i2;
        this.codes = strArr;
        this.letters = strArr2;
        this.digraph = zArr;
        this.han = z;
    }

    static NameToken[] tokenize(String str, PinyinTable pinyinTable) {
        if (str == null || str.isEmpty()) {
            return EMPTY;
        }
        ArrayList arrayList = new ArrayList();
        StringBuilder sb = new StringBuilder();
        int length = str.length();
        int i = 0;
        int i2 = 0;
        int i3 = 0;
        while (i < length && arrayList.size() < MAX_TOKENS) {
            int iCodePointAt = str.codePointAt(i);
            int iCharCount = Character.charCount(iCodePointAt) + i;
            String[] strArr = pinyinTable.readings(iCodePointAt);
            if (strArr.length > 0) {
                flushWord(arrayList, sb, i3, i2);
                arrayList.add(hanToken(i, iCharCount, strArr));
            } else if (isMark(iCodePointAt)) {
                if (sb.length() > 0) {
                    i2 = iCharCount;
                }
            } else if (!isDirectionMark(iCodePointAt)) {
                int length2 = sb.length();
                appendFolded(sb, iCodePointAt);
                if (sb.length() > length2) {
                    if (length2 != 0) {
                        i = i3;
                    }
                    i2 = iCharCount;
                    i3 = i;
                } else {
                    flushWord(arrayList, sb, i3, i2);
                }
            }
            i = iCharCount;
        }
        flushWord(arrayList, sb, i3, i2);
        return (NameToken[]) arrayList.toArray(new NameToken[arrayList.size()]);
    }

    private static boolean isMark(int i) {
        int type = Character.getType(i);
        return type == 6 || type == 8 || type == 7;
    }

    private static boolean isDirectionMark(int i) {
        return i == 8206 || i == 8207 || (i >= 8234 && i <= 8238) || (i >= 8294 && i <= 8297);
    }

    private static void appendFolded(StringBuilder sb, int i) {
        if (i < 128) {
            appendAscii(sb, (char) i);
            return;
        }
        String strNormalize = Normalizer.normalize(new String(Character.toChars(i)), Normalizer.Form.NFKD);
        for (int i2 = 0; i2 < strNormalize.length(); i2++) {
            char cCharAt = strNormalize.charAt(i2);
            if (cCharAt < 128) {
                appendAscii(sb, cCharAt);
            }
        }
    }

    private static void appendAscii(StringBuilder sb, char c) {
        if (c >= 'A' && c <= 'Z') {
            sb.append((char) ((c - 'A') + 97));
        } else if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
            sb.append(c);
        }
    }

    private static void flushWord(ArrayList<NameToken> arrayList, StringBuilder sb, int i, int i2) {
        if (sb.length() != 0) {
            String string = sb.toString();
            arrayList.add(new NameToken(i, i2, new String[]{T9.keys(string)}, new String[]{string}, new boolean[]{false}, false));
            sb.setLength(0);
        }
    }

    private static NameToken hanToken(int i, int i2, String[] strArr) {
        boolean[] zArr;
        String[] strArr2;
        String[] strArr3;
        String[] strArr4 = new String[strArr.length];
        String[] strArr5 = new String[strArr.length];
        boolean[] zArr2 = new boolean[strArr.length];
        int i3 = 0;
        for (String str : strArr) {
            String strKeys = T9.keys(str);
            boolean z = str.length() > 2 && str.charAt(1) == 'h' && (str.charAt(0) == 'z' || str.charAt(0) == 'c' || str.charAt(0) == 's');
            int i4 = 0;
            while (true) {
                if (i4 >= i3) {
                    i4 = -1;
                    break;
                }
                if (strArr4[i4].equals(strKeys)) {
                    break;
                }
                i4++;
            }
            if (i4 >= 0) {
                zArr2[i4] = z | zArr2[i4];
            } else {
                strArr4[i3] = strKeys;
                strArr5[i3] = str;
                zArr2[i3] = z;
                i3++;
            }
        }
        if (i3 < strArr4.length) {
            strArr3 = new String[i3];
            strArr2 = new String[i3];
            zArr = new boolean[i3];
            System.arraycopy(strArr4, 0, strArr3, 0, i3);
            System.arraycopy(strArr5, 0, strArr2, 0, i3);
            System.arraycopy(zArr2, 0, zArr, 0, i3);
        } else {
            zArr = zArr2;
            strArr2 = strArr5;
            strArr3 = strArr4;
        }
        return new NameToken(i, i2, strArr3, strArr2, zArr, true);
    }
}
