package com.ziur.t9pinyin;

import java.util.ArrayList;
import java.util.Collection;

final class LookupSql {
    static final String CONTACT = "contact_id";
    private static final int MAX_SQL = 4000;
    private static final int MAX_TYPED = 40;
    static final String NAME = "display_name";
    private static final String NAME_DIGITS = "name_by_number";
    private static final String NUMBER_TEST = "normalized_number LIKE '";
    static final String TABLE = "smartdial_table";
    private final String[] columns;
    private final int contactColumn;
    private final int nameColumn;
    private final String orderBy;
    final String typed;
    private final String where;

    private LookupSql(String str, String[] strArr, int i, int i2, String str2, String str3) {
        this.typed = str;
        this.columns = strArr;
        this.nameColumn = i;
        this.contactColumn = i2;
        this.where = str2;
        this.orderBy = str3;
    }

    static boolean concerns(String str) {
        return str != null && str.length() <= MAX_SQL && str.contains(TABLE) && str.contains(" LIKE ");
    }

    static LookupSql parse(String str) {
        int i;
        String[] strArrSplitColumns;
        int length;
        int i2;
        int iIndexOf;
        if (!concerns(str) || !startsWith(str, 0, "SELECT ") || (i = topLevel(str, " FROM ", 7)) < 0 || (strArrSplitColumns = splitColumns(str.substring(7, i))) == null) {
            return null;
        }
        int i3 = -1;
        int i4 = -1;
        for (int i5 = 0; i5 < strArrSplitColumns.length; i5++) {
            if (strArrSplitColumns[i5].equals(NAME)) {
                if (i4 >= 0) {
                    return null;
                }
                i4 = i5;
            } else if (strArrSplitColumns[i5].equals(CONTACT) && i3 < 0) {
                i3 = i5;
            }
        }
        if (i4 < 0 || (i2 = topLevel(str, " WHERE ", (length = " FROM ".length() + i))) < 0 || !str.substring(length, i2).trim().equals(TABLE)) {
            return null;
        }
        int length2 = i2 + " WHERE ".length();
        for (String str2 : new String[]{" GROUP BY ", " HAVING ", " LIMIT ", " UNION "}) {
            if (topLevel(str, str2, length2) >= 0) {
                return null;
            }
        }
        int i6 = topLevel(str, " ORDER BY ", length2);
        String strTrim = (i6 < 0 ? str.substring(length2) : str.substring(length2, i6)).trim();
        String strTrim2 = i6 < 0 ? null : str.substring(" ORDER BY ".length() + i6).trim();
        if (strTrim.isEmpty() || !balanced(strTrim)) {
            return null;
        }
        if ((strTrim2 != null && (strTrim2.isEmpty() || !balanced(strTrim2))) || (iIndexOf = strTrim.indexOf(NUMBER_TEST)) < 0 || !strTrim.contains(NAME_DIGITS)) {
            return null;
        }
        int length3 = (iIndexOf + NUMBER_TEST.length()) - 1;
        int iIndexOf2 = strTrim.indexOf(39, length3 + 1);
        if (iIndexOf2 < 0) {
            return null;
        }
        String strSubstring = strTrim.substring(length3 + 1, iIndexOf2);
        int i7 = strSubstring.startsWith("%") ? 1 : 0;
        if (!strSubstring.endsWith("%") || strSubstring.length() - 1 <= i7) {
            return null;
        }
        String strSubstring2 = strSubstring.substring(i7, strSubstring.length() - 1);
        if (strSubstring2.length() > MAX_TYPED || !T9.isDigits(strSubstring2) || strTrim.indexOf("'" + strSubstring + "'", iIndexOf2 + 1) < 0) {
            return null;
        }
        return new LookupSql(strSubstring2, strArrSplitColumns, i4, i3, strTrim, strTrim2);
    }

    int contactColumn() {
        return this.contactColumn;
    }

    String candidates() {
        StringBuilder sb = new StringBuilder(256);
        sb.append("SELECT ").append(CONTACT).append(", ").append(NAME).append(" FROM ").append(TABLE).append(" WHERE ").append(NAME).append(" GLOB '*[^ -~]*'");
        if (this.orderBy != null) {
            sb.append(" ORDER BY ").append(this.orderBy);
        }
        return sb.toString();
    }

    String widened(Collection<Long> collection) {
        if (collection.isEmpty()) {
            throw new IllegalArgumentException("no contacts");
        }
        StringBuilder sb = new StringBuilder(collection.size() * 6);
        for (Long l : collection) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(l.longValue());
        }
        String str = "contact_id IN (" + ((Object) sb) + ")";
        StringBuilder sb2 = new StringBuilder(this.where.length() + (str.length() * 3) + 512);
        sb2.append("SELECT ");
        for (int i = 0; i < this.columns.length; i++) {
            if (i > 0) {
                sb2.append(", ");
            }
            if (i == this.nameColumn) {
                sb2.append("CASE WHEN ").append(str).append(" THEN ").append(NAME).append(" || '").append(note(this.typed)).append("' ELSE ").append(NAME).append(" END AS ").append(NAME);
            } else {
                sb2.append(this.columns[i]);
            }
        }
        sb2.append(" FROM ").append(TABLE).append(" WHERE (").append(this.where).append(") OR ").append(str).append(" ORDER BY (").append(str).append(") DESC");
        if (this.orderBy != null) {
            sb2.append(", ").append(this.orderBy);
        }
        return sb2.toString();
    }

    static String note(String str) {
        return " " + str + " " + T9.someLetters(str);
    }

    private static String[] splitColumns(String str) {
        ArrayList arrayList = new ArrayList();
        int i = 0;
        int i2 = 0;
        boolean z = false;
        int i3 = 0;
        while (i <= str.length()) {
            char cCharAt = i < str.length() ? str.charAt(i) : ',';
            if (i < str.length() && cCharAt == '\'') {
                z = !z;
            } else if (!z && cCharAt == MAX_TYPED) {
                i3++;
            } else if (!z && cCharAt == ')') {
                i3--;
            } else if (i == str.length() || (cCharAt == ',' && !z && i3 == 0)) {
                String strTrim = str.substring(i2, i).trim();
                if (strTrim.isEmpty()) {
                    return null;
                }
                arrayList.add(strTrim);
                i2 = i + 1;
            }
            i++;
        }
        if (z || i3 != 0 || arrayList.isEmpty() || startsWith((String) arrayList.get(0), 0, "DISTINCT ")) {
            return null;
        }
        return (String[]) arrayList.toArray(new String[arrayList.size()]);
    }

    private static int topLevel(String str, String str2, int i) {
        boolean z = false;
        int i2 = 0;
        for (int i3 = 0; i3 < str.length(); i3++) {
            char cCharAt = str.charAt(i3);
            if (cCharAt == '\'') {
                z = !z;
            } else if (z) {
                continue;
            } else if (cCharAt == MAX_TYPED) {
                i2++;
            } else if (cCharAt == ')') {
                i2--;
            } else if (i2 == 0 && i3 >= i && startsWith(str, i3, str2)) {
                return i3;
            }
        }
        return -1;
    }

    private static boolean startsWith(String str, int i, String str2) {
        return str.regionMatches(true, i, str2, 0, str2.length());
    }

    private static boolean balanced(String str) {
        boolean z = false;
        int i = 0;
        for (int i2 = 0; i2 < str.length(); i2++) {
            char cCharAt = str.charAt(i2);
            if (cCharAt == '\'') {
                z = !z;
            } else if (z) {
                continue;
            } else if (cCharAt == MAX_TYPED) {
                i++;
            } else if (cCharAt == ')' && i - 1 < 0) {
                return false;
            }
        }
        return i == 0 && !z;
    }
}
