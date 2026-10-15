package com.ziur.t9pinyin;

import java.util.List;

final class Shape {
    private static final int MAX_LENGTH = 400;

    private Shape() {
    }

    static String describe(List<String> list, String[] strArr, String str, String[] strArr2) {
        StringBuilder sb = new StringBuilder();
        sb.append('/');
        if (list != null) {
            int i = 0;
            while (i < list.size()) {
                if (i > 0) {
                    sb.append('/');
                }
                sb.append((i >= 2 || !isPlainWord(list.get(i))) ? "*" : list.get(i));
                i++;
            }
        }
        sb.append(" columns=");
        if (strArr == null) {
            sb.append("all");
        } else {
            sb.append('[');
            for (int i2 = 0; i2 < strArr.length && i2 < 40; i2++) {
                if (i2 > 0) {
                    sb.append(',');
                }
                sb.append(isPlainWord(strArr[i2]) ? strArr[i2] : "*");
            }
            sb.append(']');
        }
        sb.append(" where=").append(mask(str));
        sb.append(" args=").append(strArr2 == null ? "none" : Integer.toString(strArr2.length));
        return sb.toString();
    }

    private static boolean isPlainWord(String str) {
        if (str == null || str.isEmpty() || str.length() > 40) {
            return false;
        }
        int i = 0;
        for (int i2 = 0; i2 < str.length(); i2++) {
            char cCharAt = str.charAt(i2);
            boolean z = cCharAt >= '0' && cCharAt <= '9';
            boolean z2 = (cCharAt >= 'a' && cCharAt <= 'z') || (cCharAt >= 'A' && cCharAt <= 'Z') || cCharAt == '_';
            if (!z && !z2) {
                return false;
            }
            i = z ? i + 1 : 0;
            if (i >= 3) {
                return false;
            }
        }
        return true;
    }

    static String mask(String str) {
        if (str == null) {
            return "none";
        }
        StringBuilder sb = new StringBuilder();
        int length = str.length();
        int i = 0;
        while (i < length && sb.length() < MAX_LENGTH) {
            char cCharAt = str.charAt(i);
            if (cCharAt == '\'' || cCharAt == '\"') {
                int iIndexOf = str.indexOf(cCharAt, i + 1);
                if (iIndexOf < 0) {
                    iIndexOf = length - 1;
                }
                sb.append(cCharAt).append('?').append(cCharAt);
                i = iIndexOf + 1;
            } else if (cCharAt >= '0' && cCharAt <= '9') {
                int i2 = i;
                while (i2 < length && str.charAt(i2) >= '0' && str.charAt(i2) <= '9') {
                    i2++;
                }
                sb.append(i2 - i >= 3 ? "#" : str.substring(i, i2));
                i = i2;
            } else if (cCharAt > '~' || cCharAt < ' ') {
                sb.append('?');
                i++;
            } else {
                sb.append(cCharAt);
                i++;
            }
        }
        if (i < length) {
            sb.append("...");
        }
        return sb.toString();
    }
}
