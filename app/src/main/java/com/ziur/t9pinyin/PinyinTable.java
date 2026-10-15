package com.ziur.t9pinyin;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

final class PinyinTable {
    private static final String[] NONE = new String[0];
    private final int[] extraCp;
    private final int[] extraStart;
    private final short[] ids;
    private final int[] rangeBase;
    private final int[] rangeFirst;
    private final int[] rangeLast;
    private final int[] starts;
    private final String[] syllables;

    private PinyinTable(String[] strArr, int[] iArr, int[] iArr2, int[] iArr3, int[] iArr4, int[] iArr5, int[] iArr6, short[] sArr) {
        this.syllables = strArr;
        this.rangeFirst = iArr;
        this.rangeLast = iArr2;
        this.rangeBase = iArr3;
        this.starts = iArr4;
        this.extraCp = iArr5;
        this.extraStart = iArr6;
        this.ids = sArr;
    }

    static PinyinTable load(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(102400);
        byte[] bArr = new byte[8192];
        while (true) {
            int i = inputStream.read(bArr);
            if (i != -1) {
                byteArrayOutputStream.write(bArr, 0, i);
            } else {
                try {
                    return parse(ByteBuffer.wrap(byteArrayOutputStream.toByteArray()));
                } catch (RuntimeException e) {
                    throw new IOException("bad pinyin table", e);
                }
            }
        }
    }

    private static PinyinTable parse(ByteBuffer byteBuffer) throws IOException {
        if (byteBuffer.get() != 84 || byteBuffer.get() != 57 || byteBuffer.get() != 80 || byteBuffer.get() != 89) {
            throw new IOException("bad pinyin table: magic");
        }
        int i = byteBuffer.get() & 255;
        if (i != 1) {
            throw new IOException("bad pinyin table: version " + i);
        }
        int i2 = byteBuffer.getShort() & 65535;
        String[] strArr = new String[i2];
        for (int i3 = 0; i3 < i2; i3++) {
            int i4 = byteBuffer.get() & 255;
            char[] cArr = new char[i4];
            for (int i5 = 0; i5 < i4; i5++) {
                cArr[i5] = (char) (byteBuffer.get() & 255);
            }
            strArr[i3] = new String(cArr);
        }
        int i6 = byteBuffer.get() & 255;
        int[] iArr = new int[i6];
        int[] iArr2 = new int[i6];
        int[] iArr3 = new int[i6];
        int i7 = 0;
        int i8 = 0;
        while (i7 < i6) {
            iArr[i7] = byteBuffer.getInt();
            iArr2[i7] = byteBuffer.getInt();
            if (iArr2[i7] < iArr[i7]) {
                throw new IOException("bad pinyin table: range");
            }
            iArr3[i7] = i8;
            int i9 = (iArr2[i7] - iArr[i7]) + 1 + i8;
            i7++;
            i8 = i9;
        }
        short[] sArr = new short[(byteBuffer.remaining() / 2) + 1];
        int i10 = 0;
        int[] iArr4 = new int[i8 + 1];
        for (int i11 = 0; i11 < i8; i11++) {
            iArr4[i11] = i10;
            int i12 = byteBuffer.get() & 255;
            int i13 = 0;
            while (i13 < i12) {
                sArr[i10] = checkId(byteBuffer.getShort(), i2);
                i13++;
                i10++;
            }
        }
        iArr4[i8] = i10;
        int i14 = byteBuffer.getShort() & 65535;
        int[] iArr5 = new int[i14];
        int[] iArr6 = new int[i14 + 1];
        int i15 = -1;
        int i16 = 0;
        while (i16 < i14) {
            iArr5[i16] = byteBuffer.getInt();
            if (iArr5[i16] <= i15) {
                throw new IOException("bad pinyin table: extras not sorted");
            }
            int i17 = iArr5[i16];
            iArr6[i16] = i10;
            int i18 = byteBuffer.get() & 255;
            int i19 = 0;
            while (i19 < i18) {
                sArr[i10] = checkId(byteBuffer.getShort(), i2);
                i19++;
                i10++;
            }
            i16++;
            i15 = i17;
        }
        iArr6[i14] = i10;
        if (byteBuffer.hasRemaining()) {
            throw new IOException("bad pinyin table: trailing bytes");
        }
        return new PinyinTable(strArr, iArr, iArr2, iArr3, iArr4, iArr5, iArr6, Arrays.copyOf(sArr, i10));
    }

    private static short checkId(short s, int i) throws IOException {
        if ((65535 & s) >= i) {
            throw new IOException("bad pinyin table: syllable index");
        }
        return s;
    }

    String[] readings(int i) {
        int iBinarySearch;
        for (int i2 = 0; i2 < this.rangeFirst.length; i2++) {
            if (i >= this.rangeFirst[i2] && i <= this.rangeLast[i2]) {
                int i3 = (i - this.rangeFirst[i2]) + this.rangeBase[i2];
                return slice(this.starts[i3], this.starts[i3 + 1]);
            }
        }
        if (i > 65535 && (iBinarySearch = Arrays.binarySearch(this.extraCp, i)) >= 0) {
            return slice(this.extraStart[iBinarySearch], this.extraStart[iBinarySearch + 1]);
        }
        return NONE;
    }

    private String[] slice(int i, int i2) {
        if (i == i2) {
            return NONE;
        }
        String[] strArr = new String[i2 - i];
        for (int i3 = i; i3 < i2; i3++) {
            strArr[i3 - i] = this.syllables[this.ids[i3] & 65535];
        }
        return strArr;
    }
}
