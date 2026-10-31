package si.odisej.tile;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.text.format.DateFormat;

import si.odisej.MainActivity;
import si.odisej.R;
import si.odisej.service.ServiceStatus;
import si.odisej.settings.Prefs;
import si.odisej.settings.ProtectionStatus;

/**
 * Ploščica v hitrih nastavitvah: "Odisej" + stanje. Tap odpre aplikacijo — premora NE sproži,
 * ta še vedno zahteva 60 s odštevanja v aplikaciji (strogi način).
 */
public class StatusTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        Tile tile = getQsTile();
        if (tile == null) {
            return;
        }
        Prefs prefs = new Prefs(this);
        ProtectionStatus status = ProtectionStatus.of(
                ServiceStatus.isEnabled(this), prefs.isPaused(System.currentTimeMillis()));
        String text;
        switch (status) {
            case ACTIVE:
                text = getString(R.string.status_active);
                break;
            case PAUSED:
                text = getString(R.string.status_paused,
                        DateFormat.getTimeFormat(this).format(prefs.getPausedUntil()));
                break;
            case OFF:
            default:
                text = getString(R.string.tile_off);
                break;
        }
        tile.setLabel(getString(R.string.app_name));
        tile.setState(status == ProtectionStatus.ACTIVE ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.setSubtitle(text);
        }
        tile.setContentDescription(getString(R.string.app_name) + ", " + text);
        tile.updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        if (isLocked()) {
            unlockAndRun(this::openApp);
        } else {
            openApp();
        }
    }

    @SuppressLint("StartActivityAndCollapseDeprecated") // veja za API < 34
    @SuppressWarnings("deprecation")
    private void openApp() {
        Intent intent = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
        } else {
            startActivityAndCollapse(intent);
        }
    }
}
