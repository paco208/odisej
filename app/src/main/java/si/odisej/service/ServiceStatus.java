package si.odisej.service;

import android.content.ComponentName;
import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;

/** Ali je naš servis vklopljen v sistemskih nastavitvah dostopnosti. */
public final class ServiceStatus {

    private ServiceStatus() {
    }

    public static boolean isEnabled(Context context) {
        String enabled = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (TextUtils.isEmpty(enabled)) {
            return false;
        }
        ComponentName ours = new ComponentName(context, ReelsAccessibilityService.class);
        // Oblika: "paket/razred:paket/razred:..."
        for (String entry : enabled.split(":")) {
            ComponentName cn = ComponentName.unflattenFromString(entry);
            if (ours.equals(cn)) {
                return true;
            }
        }
        return false;
    }
}
