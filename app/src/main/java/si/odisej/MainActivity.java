package si.odisej;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.format.DateFormat;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.time.LocalDate;

import si.odisej.service.ServiceStatus;
import si.odisej.settings.BlockStats;
import si.odisej.settings.PauseCountdown;
import si.odisej.settings.Prefs;

public class MainActivity extends AppCompatActivity {

    private TextView statusTitle;
    private TextView statusDetail;
    private View restrictedTitle;
    private View restrictedBody;
    private View openAppInfo;
    private Prefs prefs;
    private TextView stats;
    private TextView pauseStatus;
    private Button pauseButton;
    private final PauseCountdown countdown = new PauseCountdown();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = this::onTick;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        statusTitle = findViewById(R.id.status_title);
        statusDetail = findViewById(R.id.status_detail);
        restrictedTitle = findViewById(R.id.restricted_title);
        restrictedBody = findViewById(R.id.restricted_body);
        openAppInfo = findViewById(R.id.btn_open_app_info);

        findViewById(R.id.btn_open_accessibility).setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        openAppInfo.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", getPackageName(), null))));

        prefs = new Prefs(this);
        RadioGroup modeGroup = findViewById(R.id.mode_group);
        modeGroup.check(prefs.getMode() == Prefs.Mode.BACK ? R.id.mode_back : R.id.mode_overlay);
        modeGroup.setOnCheckedChangeListener((group, checkedId) ->
                prefs.setMode(checkedId == R.id.mode_back ? Prefs.Mode.BACK : Prefs.Mode.OVERLAY));
        CompoundButton allowDm = findViewById(R.id.switch_allow_dm);
        allowDm.setChecked(prefs.isDmReelsAllowed());
        allowDm.setOnCheckedChangeListener((button, checked) -> prefs.setDmReelsAllowed(checked));

        stats = findViewById(R.id.stats);
        pauseStatus = findViewById(R.id.pause_status);
        pauseButton = findViewById(R.id.btn_pause);
        pauseButton.setOnClickListener(v -> onPauseButton());
    }

    /**
     * Strogi način: premor zahteva 60 s odštevanja. Nadaljevanje blokiranja je takojšnje —
     * trenje velja samo v smeri, v katero vleče šibkost.
     */
    private void onPauseButton() {
        if (prefs.isPaused(System.currentTimeMillis())) {
            prefs.setPausedUntil(0);
        } else if (countdown.isRunning()) {
            countdown.cancel();
        } else {
            countdown.start(SystemClock.elapsedRealtime());
            handler.post(tick);
        }
        renderPause();
    }

    private void onTick() {
        if (countdown.isFinished(SystemClock.elapsedRealtime())) {
            countdown.cancel();
            prefs.setPausedUntil(System.currentTimeMillis() + PauseCountdown.PAUSE_MS);
        }
        renderPause();
        if (countdown.isRunning() || prefs.isPaused(System.currentTimeMillis())) {
            handler.postDelayed(tick, 500);
        }
    }

    private void renderPause() {
        long now = System.currentTimeMillis();
        if (prefs.isPaused(now)) {
            String until = DateFormat.getTimeFormat(this).format(prefs.getPausedUntil());
            pauseStatus.setText(getString(R.string.pause_active, until));
            pauseButton.setText(R.string.btn_resume);
        } else if (countdown.isRunning()) {
            pauseStatus.setText(R.string.pause_explain);
            pauseButton.setText(getString(R.string.btn_pause_waiting,
                    countdown.remainingSeconds(SystemClock.elapsedRealtime())));
        } else {
            pauseStatus.setText(R.string.pause_explain);
            pauseButton.setText(R.string.btn_pause);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Odštevanje velja le, dokler gledaš ta zaslon: če odideš, se začne znova.
        countdown.cancel();
        handler.removeCallbacks(tick);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Uporabnik se iz sistemskih nastavitev vrne sem, zato stanje osvežimo ob vsakem vrnitvi.
        boolean enabled = ServiceStatus.isEnabled(this);
        statusTitle.setText(enabled ? R.string.status_on : R.string.status_off);
        statusDetail.setText(enabled ? R.string.status_on_detail : R.string.status_off_detail);
        // Navodila za omejene nastavitve so pomembna le, dokler servis ni vklopljen.
        int restrictedVisibility = enabled ? View.GONE : View.VISIBLE;
        restrictedTitle.setVisibility(restrictedVisibility);
        restrictedBody.setVisibility(restrictedVisibility);
        openAppInfo.setVisibility(restrictedVisibility);

        BlockStats s = prefs.getStats();
        stats.setText(getString(R.string.stats, s.todayOn(LocalDate.now().toEpochDay()), s.total));
        renderPause();
        if (prefs.isPaused(System.currentTimeMillis())) {
            handler.post(tick);
        }
    }
}
