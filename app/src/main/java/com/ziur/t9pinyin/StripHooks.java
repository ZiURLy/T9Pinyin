package com.ziur.t9pinyin;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.widget.TextView;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Member;
import java.util.concurrent.atomic.AtomicBoolean;

final class StripHooks {
    private static final AtomicBoolean installed = new AtomicBoolean();

    private interface Lookup {
        Member find() throws Exception;
    }

    private StripHooks() {
    }

    static void install() {
        if (installed.compareAndSet(false, true)) {
            XC_MethodHook xC_MethodHook = new XC_MethodHook() {
                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                    try {
                        Object[] objArr = methodHookParam.args;
                        if (objArr != null && objArr.length > 0 && (objArr[0] instanceof CharSequence) && Tag.has((CharSequence) objArr[0])) {
                            objArr[0] = StripHooks.clean((CharSequence) objArr[0]);
                        }
                    } catch (Throwable th) {
                        Diagnostics.error("display hook", th);
                    }
                }
            };
            XC_MethodHook xC_MethodHook2 = new XC_MethodHook() {
                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                    try {
                        Object[] objArr = methodHookParam.args;
                        if ((methodHookParam.thisObject instanceof SpannedString) && objArr != null && objArr.length >= 3 && (objArr[0] instanceof CharSequence) && (objArr[1] instanceof Integer) && (objArr[2] instanceof Integer)) {
                            CharSequence charSequence = (CharSequence) objArr[0];
                            if (((Integer) objArr[1]).intValue() == 0 && ((Integer) objArr[2]).intValue() == charSequence.length() && Tag.has(charSequence)) {
                                CharSequence charSequenceClean = StripHooks.clean(charSequence);
                                objArr[0] = charSequenceClean;
                                objArr[2] = Integer.valueOf(charSequenceClean.length());
                            }
                        }
                    } catch (Throwable th) {
                        Diagnostics.error("display hook", th);
                    }
                }
            };
            StringBuilder sb = new StringBuilder();
            hook(sb, "SpannedString(text)", xC_MethodHook, new Lookup() {
                @Override
                public Member find() throws Exception {
                    return SpannedString.class.getDeclaredConstructor(CharSequence.class);
                }
            });
            hook(sb, "SpannedString(text,flag)", xC_MethodHook, new Lookup() {
                @Override
                public Member find() throws Exception {
                    return SpannedString.class.getDeclaredConstructor(CharSequence.class, Boolean.TYPE);
                }
            });
            hook(sb, "SpannableStringInternal", xC_MethodHook2, new Lookup() {
                @Override
                public Member find() throws Exception {
                    Class<?> cls = Class.forName("android.text.SpannableStringInternal");
                    try {
                        return cls.getDeclaredConstructor(CharSequence.class, Integer.TYPE, Integer.TYPE, Boolean.TYPE);
                    } catch (NoSuchMethodException e) {
                        return cls.getDeclaredConstructor(CharSequence.class, Integer.TYPE, Integer.TYPE);
                    }
                }
            });
            hook(sb, "TextView.setText(text,type)", xC_MethodHook, new Lookup() {
                @Override
                public Member find() throws Exception {
                    return TextView.class.getDeclaredMethod("setText", CharSequence.class, TextView.BufferType.class);
                }
            });
            hook(sb, "TextView.setText(text,type,notify,length)", xC_MethodHook, new Lookup() {
                @Override
                public Member find() throws Exception {
                    return TextView.class.getDeclaredMethod("setText", CharSequence.class, TextView.BufferType.class, Boolean.TYPE, Integer.TYPE);
                }
            });
            Diagnostics.info("names are cleaned in:" + ((Object) sb));
        }
    }

    private static void hook(StringBuilder sb, String str, XC_MethodHook xC_MethodHook, Lookup lookup) {
        try {
            XposedBridge.hookMethod(lookup.find(), xC_MethodHook);
            sb.append(' ').append(str);
        } catch (Throwable th) {
            sb.append(" (not ").append(str).append(": ").append(th.getClass().getSimpleName()).append(')');
        }
    }

    static CharSequence clean(CharSequence text) {
        if (!(text instanceof Spanned)) {
            return Tag.strip(text.toString());
        }
        SpannableStringBuilder sb = new SpannableStringBuilder(text);
        int prev = 0;
        for (int start = Tag.start(sb, 0); start >= 0; start = Tag.start(sb, start)) {
            int end = Tag.end(sb, start);
            Object[] spans = null;
            int[] range = null;
            try {
                spans = highlights(sb, prev, end);
                if (spans.length > 0) {
                    range = Pinyin.INSTANCE.range(sb.toString().substring(prev, start), T9.keysOrNull(Tag.letters(sb, start, end)));
                }
            } catch (RuntimeException e) {
                range = null;
            }
            sb.delete(start, end);
            if (range != null && range[0] < range[1] && range[1] + prev <= start) {
                highlight(sb, spans, range[0] + prev, range[1] + prev);
            }
            prev = start;
        }
        return sb;
    }

    private static Object[] highlights(Spanned spanned, int i, int i2) {
        Object[] spans = spanned.getSpans(i, i2, StyleSpan.class);
        Object[] spans2 = spanned.getSpans(i, i2, ForegroundColorSpan.class);
        Object[] objArr = new Object[spans.length + spans2.length];
        System.arraycopy(spans, 0, objArr, 0, spans.length);
        System.arraycopy(spans2, 0, objArr, spans.length, spans2.length);
        return objArr;
    }

    /** Moves the first style span and the first colour span onto [start, end). */
    private static void highlight(SpannableStringBuilder text, Object[] spans, int start, int end) {
        boolean style = false;
        boolean colour = false;
        for (Object span : spans) {
            text.removeSpan(span);
            boolean isStyle = span instanceof StyleSpan;
            if (isStyle ? style : colour) {
                continue;
            }
            if (isStyle) {
                style = true;
            } else {
                colour = true;
            }
            text.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }
}
