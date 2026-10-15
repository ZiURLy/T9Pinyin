package com.ziur.t9pinyin;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.CancellationSignal;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

final class LookupHook {
    private static final ThreadLocal<Boolean> BUSY = new ThreadLocal<>();
    private static final ThreadLocal<WeakReference<Cursor>> HANDLED = new ThreadLocal<>();
    static final int MAX_CONTACTS = 20;
    static final int ROWS_READ = 20;

    private LookupHook() {
    }

    static boolean busy() {
        return BUSY.get() != null;
    }

    static boolean handled(Cursor cursor) {
        WeakReference<Cursor> weakReference = HANDLED.get();
        return weakReference != null && weakReference.get() == cursor;
    }

    static Cursor process(String str, SQLiteDatabase sQLiteDatabase, SQLiteDatabase.CursorFactory cursorFactory, String str2, String[] strArr, String str3, CancellationSignal cancellationSignal, Cursor cursor) {
        HANDLED.set(new WeakReference<>(cursor));
        Cursor cursorWiden = widen(str, sQLiteDatabase, cursorFactory, str2, strArr, str3, cancellationSignal, cursor);
        if (cursorWiden != null) {
            HANDLED.set(new WeakReference<>(cursorWiden));
        }
        return cursorWiden;
    }

    private static Cursor widen(String hook, SQLiteDatabase db, SQLiteDatabase.CursorFactory factory, String sql, String[] args, String editTable, CancellationSignal cancel, Cursor original) {
        LookupSql lookup = LookupSql.parse(sql);
        if (lookup == null) {
            Diagnostics.unknownLookup(sql);
            return null;
        }
        if (!Pinyin.INSTANCE.ready()) {
            return null;
        }
        long start = System.nanoTime();
        LinkedHashSet<Long> matched = new LinkedHashSet<>();
        int compared = 0;
        Cursor widened = null;
        Set<Long> read;
        BUSY.set(Boolean.TRUE);
        try {
            try {
                Cursor candidates = db.rawQuery(lookup.candidates(), args);
                try {
                    HashSet<Long> seen = new HashSet<>();
                    while (matched.size() < MAX_CONTACTS && candidates.moveToNext()) {
                        if (candidates.isNull(0) || candidates.isNull(1)) {
                            continue;
                        }
                        Long contact = Long.valueOf(candidates.getLong(0));
                        if (!seen.add(contact)) {
                            continue;
                        }
                        compared++;
                        if (Pinyin.INSTANCE.match(candidates.getString(1), lookup.typed) != null) {
                            matched.add(contact);
                        }
                    }
                } finally {
                    candidates.close();
                }
                if (matched.isEmpty()) {
                    Search.clear();
                    Diagnostics.lookup(hook, sql, compared, 0, System.nanoTime() - start);
                    return null;
                }
                widened = db.rawQueryWithFactory(factory, lookup.widened(matched), args, editTable, cancel);
                int count = widened.getCount();
                if (!Arrays.equals(widened.getColumnNames(), original.getColumnNames())) {
                    throw new IllegalStateException("columns differ");
                }
                read = contactsRead(widened, lookup.contactColumn(), count);
            } catch (RuntimeException e) {
                close(widened);
                throw e;
            }
        } finally {
            BUSY.remove();
        }
        StripHooks.install();
        Search.publish(lookup.typed, matched, read);
        close(original);
        Diagnostics.lookup(hook, sql, compared, matched.size(), System.nanoTime() - start);
        return widened;
    }

    private static Set<Long> contactsRead(Cursor cursor, int i, int i2) {
        if (i < 0) {
            return null;
        }
        HashSet hashSet = new HashSet();
        try {
            int iMin = Math.min(i2, 20);
            for (int i3 = 0; i3 < iMin; i3++) {
                if (!cursor.moveToPosition(i3)) {
                    break;
                }
                hashSet.add(Long.valueOf(cursor.getLong(i)));
            }
            return hashSet;
        } finally {
            cursor.moveToPosition(-1);
        }
    }

    private static void close(Cursor cursor) {
        if (cursor != null) {
            try {
                cursor.close();
            } catch (RuntimeException e) {
            }
        }
    }
}
