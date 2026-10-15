package com.ziur.t9pinyin;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class NamesHook {
    private static final String ARG_SELECTION = "android:query-arg-sql-selection";
    private static final String ARG_SELECTION_ARGS = "android:query-arg-sql-selection-args";
    private static final String CONTACT = "contact_id";
    static final String CONTACTS_AUTHORITY = "com.android.contacts";
    private static final String CONTACT_IN = "contact_id IN (";
    private static final String NAME = "display_name";

    private NamesHook() {
    }

    static Cursor process(Uri uri, String[] strArr, Bundle bundle, Cursor cursor) {
        String[] stringArray;
        String string;
        Search searchPending = Search.pending();
        if (searchPending == null || !CONTACTS_AUTHORITY.equals(uri.getAuthority())) {
            return null;
        }
        if (bundle != null) {
            string = bundle.getString(ARG_SELECTION);
            stringArray = bundle.getStringArray(ARG_SELECTION_ARGS);
        } else {
            stringArray = null;
            string = null;
        }
        List<String> pathSegments = uri.getPathSegments();
        Set<Long> setAsked = isPhoneEntries(pathSegments) ? asked(string, stringArray) : null;
        if (setAsked == null) {
            Diagnostics.otherQuery(pathSegments, strArr, string, stringArray);
            return null;
        }
        int columnIndex = cursor.getColumnIndex(NAME);
        int columnIndex2 = cursor.getColumnIndex(CONTACT);
        if (columnIndex < 0 || columnIndex2 < 0) {
            Diagnostics.otherQuery(pathSegments, cursor.getColumnNames(), string, stringArray);
            return null;
        }
        int iAccepts = searchPending.accepts(setAsked);
        if (iAccepts == 0) {
            Diagnostics.otherQuery(pathSegments, strArr, string, stringArray);
            return null;
        }
        Diagnostics.namesQuery(iAccepts == 2, pathSegments, cursor.getColumnNames(), string, stringArray);
        return new TaggedCursor(cursor, columnIndex, columnIndex2, searchPending);
    }

    private static boolean isPhoneEntries(List<String> list) {
        return list != null && list.size() == 2 && "data".equals(list.get(0)) && "phones".equals(list.get(1));
    }

    static Set<Long> asked(String str, String[] strArr) {
        int i;
        if (str == null) {
            return null;
        }
        int iIndexOf = str.indexOf(CONTACT_IN);
        if (iIndexOf < 0 || (iIndexOf > 0 && isWordChar(str.charAt(iIndexOf - 1)))) {
            return null;
        }
        int length = iIndexOf + CONTACT_IN.length();
        int iIndexOf2 = str.indexOf(41, length);
        if (iIndexOf2 < 0) {
            return null;
        }
        boolean z = false;
        int i2 = 0;
        for (int i3 = 0; i3 < iIndexOf; i3++) {
            char cCharAt = str.charAt(i3);
            if (cCharAt == '\'') {
                z = !z;
            } else if (cCharAt == '?' && !z) {
                i2++;
            }
        }
        HashSet hashSet = new HashSet();
        String[] strArrSplit = str.substring(length, iIndexOf2).split(",");
        int length2 = strArrSplit.length;
        int i4 = 0;
        int i5 = i2;
        while (i4 < length2) {
            String strTrim = strArrSplit[i4].trim();
            if (!strTrim.equals("?")) {
                i = i5;
            } else {
                if (strArr == null || i5 >= strArr.length) {
                    return null;
                }
                i = i5 + 1;
                strTrim = strArr[i5];
            }
            if (strTrim == null || !T9.isDigits(strTrim) || strTrim.length() > 18) {
                return null;
            }
            hashSet.add(Long.valueOf(Long.parseLong(strTrim)));
            i4++;
            i5 = i;
        }
        return hashSet;
    }

    private static boolean isWordChar(char c) {
        return c == '_' || (c >= '0' && c <= '9') || ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'));
    }
}
