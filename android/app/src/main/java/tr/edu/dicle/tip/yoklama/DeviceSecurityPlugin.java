package tr.edu.dicle.tip.yoklama;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.provider.Settings;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.content.pm.PackageManager;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.File;

/**
 * DeviceSecurityPlugin — Dicle Tip Yoklama icin native guvenlik kontrolleri.
 *
 * JS tarafindan: window.Capacitor.Plugins.DeviceSecurity.XXX() seklinde cagrilir.
 *
 * Ozellikler:
 * - getAndroidId(): Sabit cihaz kimligi (reset cekince degisir)
 * - checkMockLocation(): GPS spoofing tespiti
 * - checkRooted(): Root cihaz tespiti
 * - checkEmulator(): Emulator tespiti
 * - getCurrentLocation(): Mock kontrolu ile GPS
 */
@CapacitorPlugin(name = "DeviceSecurity")
public class DeviceSecurityPlugin extends Plugin {

    @PluginMethod
    public void getAndroidId(PluginCall call) {
        String androidId = Settings.Secure.getString(
            getContext().getContentResolver(),
            Settings.Secure.ANDROID_ID
        );
        JSObject ret = new JSObject();
        ret.put("androidId", androidId != null ? androidId : "");
        call.resolve(ret);
    }

    @PluginMethod
    public void getSecurityInfo(PluginCall call) {
        JSObject ret = new JSObject();

        // Android ID (reset cekmeden degismez)
        String androidId = Settings.Secure.getString(
            getContext().getContentResolver(),
            Settings.Secure.ANDROID_ID
        );
        ret.put("androidId", androidId != null ? androidId : "");

        // Root tespiti
        ret.put("isRooted", isDeviceRooted());

        // Emulator tespiti
        ret.put("isEmulator", isEmulator());

        // Developer mode aktif mi? (mock location icin gerekli)
        int devMode = 0;
        try {
            devMode = Settings.Secure.getInt(
                getContext().getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
                0
            );
        } catch (Exception e) {}
        ret.put("devMode", devMode == 1);

        // Debuggable mi? (release APK'da false olmali)
        boolean isDebuggable = (getContext().getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        ret.put("isDebuggable", isDebuggable);

        // Cihaz bilgisi
        ret.put("manufacturer", Build.MANUFACTURER);
        ret.put("model", Build.MODEL);
        ret.put("device", Build.DEVICE);
        ret.put("androidVersion", Build.VERSION.RELEASE);
        ret.put("sdkVersion", Build.VERSION.SDK_INT);

        call.resolve(ret);
    }

    @PluginMethod
    public void getCurrentLocationSecure(PluginCall call) {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            call.reject("Konum izni yok");
            return;
        }

        LocationManager lm = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) {
            call.reject("LocationManager yok");
            return;
        }

        try {
            Location gpsLoc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location netLoc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            Location best = gpsLoc != null ? gpsLoc : netLoc;
            if (best == null) {
                call.reject("Konum alinamadi");
                return;
            }

            JSObject ret = new JSObject();
            ret.put("latitude", best.getLatitude());
            ret.put("longitude", best.getLongitude());
            ret.put("accuracy", best.getAccuracy());
            ret.put("timestamp", best.getTime());
            ret.put("provider", best.getProvider());

            // ▼▼▼ KRITIK: Mock location tespiti ▼▼▼
            boolean isMock = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ — yeni API
                isMock = best.isMock();
            } else {
                // Eski Android — deprecated ama calisir
                isMock = best.isFromMockProvider();
            }
            ret.put("isMock", isMock);

            call.resolve(ret);
        } catch (SecurityException e) {
            call.reject("Konum izni hatasi: " + e.getMessage());
        }
    }

    // ── Root tespiti (3 farkli yontem) ──────────────────────────
    private boolean isDeviceRooted() {
        return checkRootMethod1() || checkRootMethod2() || checkRootMethod3();
    }

    private boolean checkRootMethod1() {
        String buildTags = Build.TAGS;
        return buildTags != null && buildTags.contains("test-keys");
    }

    private boolean checkRootMethod2() {
        String[] paths = {
            "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su",
            "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su",
            "/system/sd/xbin/su", "/system/bin/failsafe/su", "/data/local/su",
            "/su/bin/su", "/system/xbin/busybox"
        };
        for (String path : paths) {
            if (new File(path).exists()) return true;
        }
        return false;
    }

    private boolean checkRootMethod3() {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(new String[]{"/system/xbin/which", "su"});
            return process.getInputStream().read() != -1;
        } catch (Throwable t) {
            return false;
        } finally {
            if (process != null) process.destroy();
        }
    }

    // ── Emulator tespiti ────────────────────────────────────────
    private boolean isEmulator() {
        return (Build.FINGERPRINT.startsWith("generic")
            || Build.FINGERPRINT.startsWith("unknown")
            || Build.MODEL.contains("google_sdk")
            || Build.MODEL.contains("Emulator")
            || Build.MODEL.contains("Android SDK built for")
            || Build.MANUFACTURER.contains("Genymotion")
            || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
            || "google_sdk".equals(Build.PRODUCT));
    }
}
