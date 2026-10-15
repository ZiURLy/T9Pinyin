package com.ziur.t9pinyin;

import java.util.Arrays;

final class T9Matcher {
    private static final int NO_MATCH = -1;
    private static final int UNKNOWN = -2;
    int endToken;
    private int[] memo = new int[64];
    private String query;
    int startToken;
    private int stride;
    private NameToken[] tokens;

    T9Matcher() {
    }

    boolean find(NameToken[] nameTokenArr, String str, boolean z) {
        int length = nameTokenArr.length;
        int length2 = str.length();
        if (length == 0 || length2 == 0) {
            return false;
        }
        this.tokens = nameTokenArr;
        this.query = str;
        this.stride = length2;
        int i = length2 * length;
        if (this.memo.length < i) {
            this.memo = new int[i];
        }
        Arrays.fill(this.memo, 0, i, UNKNOWN);
        for (int i2 = 0; i2 < length; i2++) {
            int iBest = best(i2, 0);
            if (iBest != NO_MATCH && (!z || hasHan(i2, iBest))) {
                this.startToken = i2;
                this.endToken = iBest;
                return true;
            }
        }
        return false;
    }

    private boolean hasHan(int i, int i2) {
        while (i < i2) {
            if (!this.tokens[i].han) {
                i++;
            } else {
                return true;
            }
        }
        return false;
    }

    String spell() {
        int length = this.query.length();
        StringBuilder sb = new StringBuilder(length);
        int i = 0;
        int i2 = this.startToken;
        while (i < length) {
            NameToken nameToken = this.tokens[i2];
            int i3 = length - i;
            int i4 = this.memo[(this.stride * i2) + i];
            int i5 = 0;
            String str = null;
            int i6 = 0;
            while (true) {
                int i7 = i6;
                if (i7 >= 4 || i5 != 0) {
                    break;
                }
                for (int i8 = 0; i8 < nameToken.codes.length && i5 == 0; i8++) {
                    String str2 = nameToken.codes[i8];
                    int iCommon = common(str2, i);
                    if (iCommon != 0) {
                        if (i7 == 0) {
                            if (iCommon == i3 && i4 == i2 + 1) {
                                i5 = iCommon;
                            }
                        } else if (i7 == 1) {
                            if (iCommon == str2.length() && iCommon < i3 && at(i2 + 1, i + iCommon) == i4) {
                                i5 = iCommon;
                            }
                        } else if (i7 == 2) {
                            if (nameToken.digraph[i8] && iCommon >= 2 && i3 > 2 && at(i2 + 1, i + 2) == i4) {
                                i5 = 2;
                            }
                        } else if (i3 > 1 && at(i2 + 1, i + 1) == i4) {
                            i5 = 1;
                        }
                        if (i5 != 0) {
                            str = nameToken.letters[i8];
                        }
                    }
                }
                i6 = i7 + 1;
            }
            if (i5 == 0) {
                throw new IllegalStateException("spell() without a match");
            }
            sb.append((CharSequence) str, 0, i5);
            i += i5;
            i2++;
        }
        return sb.toString();
    }

    private int common(String str, int i) {
        int iMin = Math.min(str.length(), this.query.length() - i);
        int i2 = 0;
        while (i2 < iMin && str.charAt(i2) == this.query.charAt(i + i2)) {
            i2++;
        }
        return i2;
    }

    private int at(int i, int i2) {
        int i3;
        return (i >= this.tokens.length || i2 >= this.query.length() || (i3 = this.memo[(this.stride * i) + i2]) == UNKNOWN) ? NO_MATCH : i3;
    }

    private int best(int i, int i2) {
        boolean z = false;
        int iMax = NO_MATCH;
        if (i >= this.tokens.length) {
            return NO_MATCH;
        }
        int i3 = (this.stride * i) + i2;
        if (this.memo[i3] != UNKNOWN) {
            return this.memo[i3];
        }
        NameToken nameToken = this.tokens[i];
        int length = this.query.length() - i2;
        int i4 = 0;
        while (true) {
            boolean z2 = z;
            if (i4 < nameToken.codes.length) {
                String str = nameToken.codes[i4];
                int iCommon = common(str, i2);
                if (iCommon == 0) {
                    z = z2;
                } else {
                    if (iCommon == length) {
                        iMax = Math.max(iMax, i + 1);
                    }
                    if (iCommon == str.length() && iCommon < length) {
                        iMax = Math.max(iMax, best(i + 1, i2 + iCommon));
                    }
                    if (nameToken.digraph[i4] && iCommon >= 2 && length > 2) {
                        iMax = Math.max(iMax, best(i + 1, i2 + 2));
                    }
                    if (length <= 1 || z2) {
                        z = z2;
                    } else {
                        iMax = Math.max(iMax, best(i + 1, i2 + 1));
                        z = true;
                    }
                }
                i4++;
            } else {
                this.memo[i3] = iMax;
                return iMax;
            }
        }
    }
}
