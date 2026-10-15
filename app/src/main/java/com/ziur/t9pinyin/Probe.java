package com.ziur.t9pinyin;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

final class Probe {
    static final String[] FINGERPRINTS = {"getLooseMatches", "(normalized_number LIKE '%", " OR (name_by_number LIKE '", "Cp2PhoneEntryListRetriever.java", "SmartDial result did not match name or number", "(^|\\s|[-_@])", "smart dial query found %d results", "EnhancedQueryBoldingUtil.java"};

    private Probe() {
    }

    static String code(List<String> paths) {
        boolean[] found = new boolean[FINGERPRINTS.length];
        int dexFiles = 0;
        long scanned = 0;
        long start = System.nanoTime();
        byte[][] needles = new byte[FINGERPRINTS.length][];
        for (int i = 0; i < needles.length; i++) {
            needles[i] = constant(FINGERPRINTS[i]);
        }
        StringBuilder problems = new StringBuilder();
        for (String path : paths) {
            ZipFile zip = null;
            try {
                zip = new ZipFile(path);
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (name.startsWith("classes") && name.endsWith(".dex") && name.indexOf('/') < 0) {
                        InputStream in = zip.getInputStream(entry);
                        try {
                            scanned += search(in, needles, found);
                        } finally {
                            in.close();
                        }
                        dexFiles++;
                    }
                }
            } catch (IOException e) {
                problems.append(" unreadable:").append(e.getClass().getSimpleName());
            } catch (RuntimeException e) {
                problems.append(" error:").append(e.getClass().getSimpleName());
            } finally {
                if (zip != null) {
                    try {
                        zip.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }
        StringBuilder sb = new StringBuilder("probe: ");
        sb.append(dexFiles).append(" dex files, ").append(scanned / 1024).append(" KB, ").append((System.nanoTime() - start) / 1000000).append(" ms;");
        for (int i = 0; i < found.length; i++) {
            sb.append(' ').append(found[i] ? "FOUND" : "absent").append(" \"").append(FINGERPRINTS[i]).append('"').append(i < found.length - 1 ? "," : "");
        }
        sb.append((CharSequence) problems);
        return sb.toString();
    }

    static byte[] constant(String str) {
        if (str.length() >= 128) {
            throw new IllegalArgumentException("too long");
        }
        byte[] bArr = new byte[str.length() + 2];
        bArr[0] = (byte) str.length();
        for (int i = 0; i < str.length(); i++) {
            char cCharAt = str.charAt(i);
            if (cCharAt == 0 || cCharAt >= 128) {
                throw new IllegalArgumentException("not ASCII");
            }
            bArr[i + 1] = (byte) cCharAt;
        }
        return bArr;
    }

    static void mark(byte[] bArr, int i, byte[][] bArr2, boolean[] zArr) {
        boolean[] zArr2 = new boolean[256];
        int iMin = Integer.MAX_VALUE;
        for (int i2 = 0; i2 < bArr2.length; i2++) {
            zArr2[bArr2[i2][0] & 255] = true;
            iMin = Math.min(iMin, bArr2[i2].length);
        }
        int i3 = i - iMin;
        for (int i4 = 0; i4 <= i3; i4++) {
            if (zArr2[bArr[i4] & 255]) {
                for (int i5 = 0; i5 < bArr2.length; i5++) {
                    if (!zArr[i5] && matchesAt(bArr, i, i4, bArr2[i5])) {
                        zArr[i5] = true;
                    }
                }
            }
        }
    }

    private static boolean matchesAt(byte[] bArr, int i, int i2, byte[] bArr2) {
        if (bArr2.length + i2 > i) {
            return false;
        }
        for (int i3 = 0; i3 < bArr2.length; i3++) {
            if (bArr[i2 + i3] != bArr2[i3]) {
                return false;
            }
        }
        return true;
    }

    static long search(InputStream inputStream, byte[][] bArr, boolean[] zArr) throws IOException {
        int iMax = 1;
        for (byte[] bArr2 : bArr) {
            iMax = Math.max(iMax, bArr2.length);
        }
        int i = iMax - 1;
        byte[] bArr3 = new byte[65536 + i];
        long j = 0;
        int iMin = 0;
        while (true) {
            int i2 = inputStream.read(bArr3, iMin, bArr3.length - iMin);
            if (i2 >= 0) {
                if (i2 != 0) {
                    j += (long) i2;
                    int i3 = i2 + iMin;
                    mark(bArr3, i3, bArr, zArr);
                    iMin = Math.min(i, i3);
                    System.arraycopy(bArr3, i3 - iMin, bArr3, 0, iMin);
                }
            } else {
                return j;
            }
        }
    }

    static List<String> paths(String str, String[] strArr) {
        ArrayList arrayList = new ArrayList();
        if (str != null) {
            arrayList.add(str);
        }
        if (strArr != null) {
            for (int i = 0; i < strArr.length; i++) {
                if (strArr[i] != null) {
                    arrayList.add(strArr[i]);
                }
            }
        }
        return arrayList;
    }
}
