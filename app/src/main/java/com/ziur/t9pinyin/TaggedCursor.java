package com.ziur.t9pinyin;

import android.database.Cursor;
import android.database.CursorWrapper;
import java.util.HashMap;

final class TaggedCursor extends CursorWrapper {
    private final int contactColumn;
    private final HashMap<String, String> done;
    private final int nameColumn;
    private final Search search;
    private int tagged;

    TaggedCursor(Cursor cursor, int i, int i2, Search search) {
        super(cursor);
        this.done = new HashMap<>();
        this.nameColumn = i;
        this.contactColumn = i2;
        this.search = search;
    }

    @Override
    public String getString(int i) {
        String strMatch;
        String string = super.getString(i);
        if (i != this.nameColumn || string == null) {
            return string;
        }
        try {
            if (!this.search.contacts.contains(Long.valueOf(super.getLong(this.contactColumn)))) {
                return string;
            }
            String strAppend = this.done.get(string);
            if (strAppend == null) {
                strAppend = (Tag.has(string) || (strMatch = Pinyin.INSTANCE.match(string, this.search.typed)) == null) ? string : Tag.append(string, strMatch, Pinyin.INSTANCE.hasOtherWords(string));
                this.done.put(string, strAppend);
            }
            if (strAppend != string) {
                this.tagged++;
                return strAppend;
            }
            return strAppend;
        } catch (RuntimeException e) {
            return string;
        }
    }

    @Override
    public void close() {
        try {
            Diagnostics.names(this.tagged);
        } finally {
            super.close();
        }
    }
}
