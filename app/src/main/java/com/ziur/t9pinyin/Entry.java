package com.ziur.t9pinyin;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class Entry implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    static final String DIALER = "com.google.android.dialer";
    private static final long PROBE_DELAY_MILLIS = 20000;
    static final String VERSION = "0.3"; // keep in sync with versionName in app/build.gradle.kts
    private static final AtomicBoolean installed = new AtomicBoolean();
    static volatile String modulePath;

    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) {
        modulePath = startupParam.modulePath;
    }

    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        if (loadPackageParam != null) {
            try {
                if (DIALER.equals(loadPackageParam.packageName) && installed.compareAndSet(false, true)) {
                    install(loadPackageParam);
                }
            } catch (Throwable th) {
                Diagnostics.error("could not install", th);
            }
        }
    }

    private static void install(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        StringBuilder sb = new StringBuilder();
        hook(sb, "rawQueryWithFactory", new XC_MethodHook() {
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                Entry.afterRawQuery(methodHookParam);
            }
        }, SQLiteDatabase.class, "rawQueryWithFactory", SQLiteDatabase.CursorFactory.class, String.class, String[].class, String.class, CancellationSignal.class);
        hook(sb, "queryWithFactory", new XC_MethodHook() {
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                Entry.afterBuiltQuery(methodHookParam);
            }
        }, SQLiteDatabase.class, "queryWithFactory", SQLiteDatabase.CursorFactory.class, Boolean.TYPE, String.class, String[].class, String.class, String[].class, String.class, String.class, String.class, String.class, CancellationSignal.class);
        XC_MethodHook xC_MethodHook = new XC_MethodHook() {
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                Entry.afterQuery(methodHookParam);
            }
        };
        hook(sb, "ContentResolver.query", xC_MethodHook, ContentResolver.class, "query", Uri.class, String[].class, Bundle.class, CancellationSignal.class);
        hook(sb, "ContentProviderClient.query", xC_MethodHook, ContentProviderClient.class, "query", Uri.class, String[].class, Bundle.class, CancellationSignal.class);
        Diagnostics.info("v" + VERSION + " active in " + loadPackageParam.processName + " (" + DIALER + " " + versionOf(loadPackageParam) + ", Android API " + Build.VERSION.SDK_INT + "); hooked:" + ((Object) sb));
        startProbe(loadPackageParam);
    }

    private static void hook(StringBuilder sb, String str, XC_MethodHook xC_MethodHook, Class<?> cls, String str2, Class<?>... clsArr) {
        try {
            XposedBridge.hookMethod(cls.getDeclaredMethod(str2, clsArr), xC_MethodHook);
            sb.append(' ').append(str);
        } catch (Throwable th) {
            sb.append(" (not ").append(str).append(": ").append(th.getClass().getSimpleName()).append(')');
        }
    }

    static void afterRawQuery(XC_MethodHook.MethodHookParam methodHookParam) {
        Cursor cursorProcess;
        try {
            Object[] objArr = methodHookParam.args;
            if (objArr != null && objArr.length >= 5 && (objArr[1] instanceof String)) {
                String str = (String) objArr[1];
                if (LookupSql.concerns(str) && !LookupHook.busy() && !methodHookParam.hasThrowable() && (methodHookParam.getResult() instanceof Cursor) && (methodHookParam.thisObject instanceof SQLiteDatabase) && isA(objArr[0], SQLiteDatabase.CursorFactory.class) && isA(objArr[2], String[].class) && isA(objArr[3], String.class) && isA(objArr[4], CancellationSignal.class) && (cursorProcess = LookupHook.process("rawQueryWithFactory", (SQLiteDatabase) methodHookParam.thisObject, (SQLiteDatabase.CursorFactory) objArr[0], str, (String[]) objArr[2], (String) objArr[3], (CancellationSignal) objArr[4], (Cursor) methodHookParam.getResult())) != null) {
                    methodHookParam.setResult(cursorProcess);
                }
            }
        } catch (Throwable th) {
            Diagnostics.error("lookup hook", th);
        }
    }

    static void afterBuiltQuery(XC_MethodHook.MethodHookParam methodHookParam) {
        Cursor cursorProcess;
        try {
            Object[] objArr = methodHookParam.args;
            if (objArr != null && objArr.length >= 11 && "smartdial_table".equals(objArr[2]) && !LookupHook.busy() && !methodHookParam.hasThrowable() && (methodHookParam.getResult() instanceof Cursor) && (methodHookParam.thisObject instanceof SQLiteDatabase) && isA(objArr[0], SQLiteDatabase.CursorFactory.class) && (objArr[1] instanceof Boolean) && isA(objArr[3], String[].class) && isA(objArr[4], String.class) && isA(objArr[5], String[].class) && isA(objArr[6], String.class) && isA(objArr[7], String.class) && isA(objArr[8], String.class) && isA(objArr[9], String.class) && isA(objArr[10], CancellationSignal.class)) {
                Cursor cursor = (Cursor) methodHookParam.getResult();
                if (!LookupHook.handled(cursor)) {
                    String str = (String) objArr[2];
                    String strBuildQueryString = SQLiteQueryBuilder.buildQueryString(((Boolean) objArr[1]).booleanValue(), str, (String[]) objArr[3], (String) objArr[4], (String) objArr[6], (String) objArr[7], (String) objArr[8], (String) objArr[9]);
                    if (LookupSql.concerns(strBuildQueryString) && (cursorProcess = LookupHook.process("queryWithFactory", (SQLiteDatabase) methodHookParam.thisObject, (SQLiteDatabase.CursorFactory) objArr[0], strBuildQueryString, (String[]) objArr[5], SQLiteDatabase.findEditTable(str), (CancellationSignal) objArr[10], cursor)) != null) {
                        methodHookParam.setResult(cursorProcess);
                    }
                }
            }
        } catch (Throwable th) {
            Diagnostics.error("lookup hook", th);
        }
    }

    private static boolean isA(Object obj, Class<?> cls) {
        return obj == null || cls.isInstance(obj);
    }

    static void afterQuery(XC_MethodHook.MethodHookParam methodHookParam) {
        try {
            if (Search.pending() != null) {
                Object[] objArr = methodHookParam.args;
                if (!methodHookParam.hasThrowable() && (methodHookParam.getResult() instanceof Cursor) && objArr != null && objArr.length >= 3 && (objArr[0] instanceof Uri)) {
                    Cursor cursorProcess = NamesHook.process((Uri) objArr[0], objArr[1] instanceof String[] ? (String[]) objArr[1] : null, objArr[2] instanceof Bundle ? (Bundle) objArr[2] : null, (Cursor) methodHookParam.getResult());
                    if (cursorProcess != null) {
                        methodHookParam.setResult(cursorProcess);
                    }
                }
            }
        } catch (Throwable th) {
            Diagnostics.error("names hook", th);
        }
    }

    private static String versionOf(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        try {
            return "versionCode " + loadPackageParam.appInfo.getClass().getField("longVersionCode").get(loadPackageParam.appInfo);
        } catch (Throwable th) {
            return "version unknown";
        }
    }

    private static void startProbe(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        if (loadPackageParam.appInfo != null && DIALER.equals(loadPackageParam.processName)) {
            final List<String> listPaths = Probe.paths(loadPackageParam.appInfo.sourceDir, loadPackageParam.appInfo.splitSourceDirs);
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(Entry.PROBE_DELAY_MILLIS);
                        Diagnostics.info(Probe.code(listPaths));
                    } catch (Throwable th) {
                        Diagnostics.error("probe", th);
                    }
                }
            }, "t9pinyin-probe");
            thread.setDaemon(true);
            thread.setPriority(1);
            thread.start();
        }
    }
}
