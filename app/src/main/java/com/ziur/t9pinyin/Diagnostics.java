package com.ziur.t9pinyin;

import de.robv.android.xposed.XposedBridge;
import java.util.HashSet;
import java.util.List;

final class Diagnostics {
    private static final int MAX_ERRORS = 20;
    private static final int MAX_FRAMES = 8;
    private static final int MAX_MESSAGE = 160;
    private static final int MAX_SHAPES = 8;
    static final String TAG = "T9Pinyin";
    private static int errors;
    private static boolean lookupSeen;
    private static int lookups;
    private static boolean looseSeen;
    private static boolean namesSeen;
    private static int namings;
    private static int widenings;
    private static final Object lock = new Object();
    private static final HashSet<String> shapes = new HashSet<>();

    private Diagnostics() {
    }

    static void info(String str) {
        try {
            XposedBridge.log("T9Pinyin: " + str);
        } catch (Throwable th) {
        }
    }

    static void error(String what, Throwable th) {
        synchronized (lock) {
            errors++;
            if (errors > MAX_ERRORS) {
                return;
            }
        }
        info("ERROR " + what + ": " + describe(th));
    }

    static String describe(Throwable th) {
        int i = 0;
        StringBuilder sb = new StringBuilder();
        Throwable cause = th;
        for (int i2 = 0; cause != null && i2 < 3; i2++) {
            if (i2 > 0) {
                try {
                    sb.append(" <- ");
                } catch (Throwable th2) {
                    sb.append(" ...");
                }
            }
            sb.append(cause.getClass().getName());
            String message = cause.getMessage();
            if (message != null) {
                String strMask = Shape.mask(message);
                StringBuilder sbAppend = sb.append(" (");
                if (strMask.length() > MAX_MESSAGE) {
                    strMask = strMask.substring(0, MAX_MESSAGE) + "...";
                }
                sbAppend.append(strMask).append(')');
            }
            cause = cause.getCause();
        }
        StackTraceElement[] stackTrace = th.getStackTrace();
        while (stackTrace != null) {
            if (i >= stackTrace.length || i >= 8) {
                break;
            }
            sb.append(" at ").append(stackTrace[i].getClassName()).append('.').append(stackTrace[i].getMethodName()).append(':').append(stackTrace[i].getLineNumber());
            i++;
        }
        return sb.toString();
    }

    static void lookup(String str, String str2, int i, int i2, long j) {
        boolean z;
        int i3;
        int i4;
        synchronized (lock) {
            z = !lookupSeen;
            lookupSeen = true;
            i3 = lookups + 1;
            lookups = i3;
            if (i2 > 0) {
                i4 = widenings + 1;
                widenings = i4;
            } else {
                i4 = widenings;
            }
        }
        if (z) {
            info("dial pad lookup recognised (in " + str + "): " + Shape.mask(str2));
        }
        if ((i2 > 0 && (i4 <= 3 || i4 % 100 == 0)) || (i2 == 0 && i3 <= 2)) {
            info("lookup #" + i3 + ": " + i + " Chinese names compared, " + i2 + " contacts added, " + (j / 1000000) + " ms");
        }
    }

    static void unknownLookup(String str) {
        once("search in smartdial_table that is not the lookup this module knows: " + Shape.mask(str));
    }

    static void namesQuery(boolean expected, List<String> columns, String[] projection, String selection, String[] args) {
        synchronized (lock) {
            if (expected ? namesSeen : looseSeen) {
                return;
            }
            if (expected) {
                namesSeen = true;
            } else {
                looseSeen = true;
            }
        }
        info("contact rows of the search recognised" + (expected ? ": " : " (not the expected contacts, taken anyway): ") + Shape.describe(columns, projection, selection, args));
    }

    static void names(int i) {
        int i2;
        synchronized (lock) {
            i2 = namings + 1;
            namings = i2;
        }
        if (i2 <= 3 || i2 % 100 == 0) {
            info("search #" + i2 + ": " + i + " names tagged");
        }
    }

    static void otherQuery(List<String> list, String[] strArr, String str, String[] strArr2) {
        once("contacts query during a search, left alone: " + Shape.describe(list, strArr, str, strArr2));
    }

    private static void once(String message) {
        synchronized (lock) {
            if (shapes.size() >= MAX_SHAPES || !shapes.add(message)) {
                return;
            }
        }
        info(message);
    }

    static void reset() {
        synchronized (lock) {
            shapes.clear();
            lookups = 0;
            widenings = 0;
            namings = 0;
            errors = 0;
            lookupSeen = false;
            namesSeen = false;
            looseSeen = false;
        }
    }
}
