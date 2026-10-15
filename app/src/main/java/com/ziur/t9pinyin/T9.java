package com.ziur.t9pinyin;

final class T9 {
    private static final String FIRST = "adgjmptw";
    private static final char[] KEYS = {'2', '2', '2', '3', '3', '3', '4', '4', '4', '5', '5', '5', '6', '6', '6', '7', '7', '7', '7', '8', '8', '8', '9', '9', '9', '9'};

    private T9() {
    }

    static char key(char c) {
        if (c >= 'a' && c <= 'z') {
            return KEYS[c - 'a'];
        }
        if (c < '0' || c > '9') {
            return (char) 0;
        }
        return c;
    }

    static String keys(String str) {
        char[] cArr = new char[str.length()];
        for (int i = 0; i < cArr.length; i++) {
            char cKey = key(str.charAt(i));
            if (cKey == 0) {
                throw new IllegalArgumentException("not on the keypad: " + str);
            }
            cArr[i] = cKey;
        }
        return new String(cArr);
    }

    static String keysOrNull(CharSequence charSequence) {
        if (charSequence == null || charSequence.length() == 0) {
            return null;
        }
        char[] cArr = new char[charSequence.length()];
        for (int i = 0; i < cArr.length; i++) {
            char cKey = key(charSequence.charAt(i));
            if (cKey == 0) {
                return null;
            }
            cArr[i] = cKey;
        }
        return new String(cArr);
    }

    static String someLetters(String str) {
        char[] cArr = new char[str.length()];
        for (int i = 0; i < cArr.length; i++) {
            char cCharAt = str.charAt(i);
            if (cCharAt >= '2' && cCharAt <= '9') {
                cCharAt = FIRST.charAt(cCharAt - '2');
            }
            cArr[i] = cCharAt;
        }
        return new String(cArr);
    }

    static boolean isDigits(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        for (int i = 0; i < str.length(); i++) {
            char cCharAt = str.charAt(i);
            if (cCharAt < '0' || cCharAt > '9') {
                return false;
            }
        }
        return true;
    }
}
