package com.ziur.t9pinyin;

final class Tag {
    static final char CLOSE = 8292;
    static final char OPEN = 8291;

    private Tag() {
    }

    static String append(String str, String str2, boolean z) {
        StringBuilder sb = new StringBuilder(str.length() + (str2.length() * 2) + 4);
        sb.append(str).append(' ').append(OPEN).append(str2);
        if (z) {
            sb.append(' ').append(str2);
        }
        return sb.append(CLOSE).toString();
    }

    static boolean has(CharSequence charSequence) {
        return charSequence != null && indexOf(charSequence, OPEN, 0) >= 0;
    }

    static int start(CharSequence charSequence, int i) {
        int iIndexOf = indexOf(charSequence, OPEN, i);
        if (iIndexOf < 0) {
            return -1;
        }
        return (iIndexOf <= i || charSequence.charAt(iIndexOf + (-1)) != ' ') ? iIndexOf : iIndexOf - 1;
    }

    static int end(CharSequence charSequence, int i) {
        int iIndexOf = indexOf(charSequence, CLOSE, i);
        return iIndexOf < 0 ? charSequence.length() : iIndexOf + 1;
    }

    static String letters(CharSequence charSequence, int i, int i2) {
        int iIndexOf = indexOf(charSequence, OPEN, i);
        if (iIndexOf < 0 || iIndexOf >= i2) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i3 = iIndexOf + 1; i3 < i2; i3++) {
            char cCharAt = charSequence.charAt(i3);
            if (cCharAt == ' ' || cCharAt == 8292) {
                break;
            }
            if ((cCharAt >= 'a' && cCharAt <= 'z') || (cCharAt >= '0' && cCharAt <= '9')) {
                sb.append(cCharAt);
            }
        }
        if (sb.length() > 0) {
            return sb.toString();
        }
        return null;
    }

    static String strip(String str) {
        if (str == null) {
            return null;
        }
        int iStart = start(str, 0);
        if (iStart >= 0) {
            StringBuilder sb = new StringBuilder(str);
            while (iStart >= 0) {
                sb.delete(iStart, end(sb, iStart));
                iStart = start(sb, iStart);
            }
            return sb.toString();
        }
        return str;
    }

    private static int indexOf(CharSequence charSequence, char c, int i) {
        int length = charSequence.length();
        for (int iMax = Math.max(i, 0); iMax < length; iMax++) {
            if (charSequence.charAt(iMax) == c) {
                return iMax;
            }
        }
        return -1;
    }
}
