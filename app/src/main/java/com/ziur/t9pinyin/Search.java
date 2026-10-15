package com.ziur.t9pinyin;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

final class Search {
    static final int EXACTLY = 2;
    static final int LOOSELY = 1;
    static final long MAX_AGE_NANOS = 3000000000L;
    static final int NO = 0;
    static volatile long clockOffsetNanos;
    private static volatile Search latest;
    final Set<Long> contacts;
    private final Set<Long> expected;
    private final long madeAt;
    final String typed;
    private final AtomicBoolean used = new AtomicBoolean();

    private Search(String str, Set<Long> set, Set<Long> set2, long j) {
        this.typed = str;
        this.contacts = set;
        this.expected = set2;
        this.madeAt = j;
    }

    private static long now() {
        return System.nanoTime() + clockOffsetNanos;
    }

    static void publish(String str, Set<Long> set, Set<Long> set2) {
        latest = new Search(str, Collections.unmodifiableSet(new HashSet(set)), set2 == null ? null : new HashSet(set2), now());
    }

    static Search pending() {
        Search search = latest;
        if (search == null) {
            return null;
        }
        if (now() - search.madeAt <= MAX_AGE_NANOS) {
            return search;
        }
        latest = null;
        return null;
    }

    int accepts(Set<Long> set) {
        if (this.expected != null && this.expected.equals(set)) {
            this.used.set(true);
            return EXACTLY;
        }
        Iterator<Long> it = this.contacts.iterator();
        while (it.hasNext()) {
            if (set.contains(it.next())) {
                if (this.used.compareAndSet(false, true)) {
                    return LOOSELY;
                }
                return 0;
            }
        }
        return 0;
    }

    static void clear() {
        latest = null;
    }
}
