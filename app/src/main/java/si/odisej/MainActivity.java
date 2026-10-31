package si.odisej;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.time.LocalDate;

import si.odisej.service.ServiceStatus;
import si.odisej.settings.BlockStats;
import si.odisej.settings.PauseCountdown;
import si.odisej.settings.Prefs;
import si.odisej.settings.ProtectionStatus;
import si.odisej.ui.CountdownRingView;
import si.odisej.ui.DurationText;
import si.odisej.ui.WeekBarsView;

public class MainActivity extends AppCompatActivity {

    private static final long HERO_CROSSFADE_MS = 250;
    private static final long TICK_MS = 200;

    private final PauseCountdown countdown = new PauseCountdown();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = this::onTick;
    /** Servis piše števce v istem procesu; zaslon se osveži v živo, ko je odprt. */
    private final SharedPreferences.OnSharedPreferenceChangeListener statsListener =
            (sp, key) -> {
                if (Prefs.isStatsKey(key)) {
                    renderStats();
                }
            };

    private Prefs prefs;
    private TextView countToday;
    private TextView countTotal;
    private WeekBarsView weekBars;
    private TextView usageLine;
    private View heroCount;
    private View heroCountdown;
    private CountdownRingView countdownRing;
    private TextView countdownSeconds;
    private TextView countdownLabel;
    private View statusDot;
    private TextView statusText;
    private View serviceOffGroup;
    private Button pauseButton;
    private View pauseDesc;
    private boolean serviceEnabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            // Sistemske vrstice + izrez kamere; ScrollView reže na padding (clipToPadding),
            // zato vsebina nikoli ni pod navigacijsko vrstico (kretnje ali trije gumbi).
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        prefs = new Prefs(this);

        countToday = findViewById(R.id.count_today);
        countTotal = findViewById(R.id.count_total);
        weekBars = findViewById(R.id.week_bars);
        usageLine = findViewById(R.id.usage_line);
        heroCount = findViewById(R.id.hero_count);
        heroCountdown = findViewById(R.id.hero_countdown);
        countdownRing = findViewById(R.id.countdown_ring);
        countdownSeconds = findViewById(R.id.countdown_seconds);
        countdownLabel = findViewById(R.id.countdown_label);
        statusDot = findViewById(R.id.status_dot);
        statusText = findViewById(R.id.status_text);
        serviceOffGroup = findViewById(R.id.service_off_group);
        pauseButton = findViewById(R.id.btn_pause);
        pauseDesc = findViewById(R.id.pause_desc);

        findViewById(R.id.btn_open_accessibility).setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        findViewById(R.id.btn_open_app_info).setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", getPackageName(), null))));

        // Stikalo "Trenutek premora": vklopljeno = overlay, izklopljeno = takoj nazaj.
        CompoundButton modeSwitch = findViewById(R.id.switch_mode);
        modeSwitch.setChecked(prefs.getMode() == Prefs.Mode.OVERLAY);
        modeSwitch.setOnCheckedChangeListener((b, checked) ->
                prefs.setMode(checked ? Prefs.Mode.OVERLAY : Prefs.Mode.BACK));
        findViewById(R.id.row_mode).setOnClickListener(v -> modeSwitch.toggle());

        CompoundButton dmSwitch = findViewById(R.id.switch_dm);
        dmSwitch.setChecked(prefs.isDmReelsAllowed());
        dmSwitch.setOnCheckedChangeListener((b, checked) -> prefs.setDmReelsAllowed(checked));
        findViewById(R.id.row_dm).setOnClickListener(v -> dmSwitch.toggle());

        pauseButton.setOnClickListener(v -> onPauseButton());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Uporabnik se iz sistemskih nastavitev vrne sem, zato stanje osvežimo ob vsaki vrnitvi.
        serviceEnabled = ServiceStatus.isEnabled(this);
        renderStats();
        prefs.registerListener(statsListener);
        render();
        if (prefs.isPaused(System.currentTimeMillis())) {
            handler.post(tick);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Odštevanje velja le, dokler gledaš ta zaslon: če odideš, se začne znova.
        if (countdown.isRunning()) {
            countdown.cancel();
            showCountdown(false, false);
        }
        handler.removeCallbacks(tick);
        prefs.unregisterListener(statsListener);
    }

    private void renderStats() {
        BlockStats s = prefs.getStats();
        LocalDate today = LocalDate.now();
        long day = today.toEpochDay();
        countToday.setText(String.valueOf(s.todayOn(day)));
        countTotal.setText(getString(R.string.total, s.total));
        weekBars.setData(s.lastSevenDays(day), today);

        int viewed = prefs.getViewed().countOn(day);
        int seconds = prefs.getReelsSeconds().countOn(day);
        if (viewed == 0 && seconds == 0) {
            usageLine.setVisibility(View.GONE);
        } else {
            String time = DurationText.format(seconds, getString(R.string.duration_under_minute),
                    getString(R.string.duration_minutes), getString(R.string.duration_hours));
            usageLine.setText(getString(R.string.usage_line, viewed, time));
            usageLine.setVisibility(View.VISIBLE);
        }
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
            showCountdown(false, true);
        } else {
            long now = SystemClock.elapsedRealtime();
            countdown.start(now);
            countdownRing.start(now, PauseCountdown.COUNTDOWN_MS);
            showCountdown(true, true);
            handler.post(tick);
        }
        render();
    }

    private void onTick() {
        if (countdown.isFinished(SystemClock.elapsedRealtime())) {
            countdown.cancel();
            prefs.setPausedUntil(System.currentTimeMillis() + PauseCountdown.PAUSE_MS);
            showCountdown(false, true);
        }
        render();
        if (countdown.isRunning() || prefs.isPaused(System.currentTimeMillis())) {
            handler.postDelayed(tick, TICK_MS);
        }
    }

    private void render() {
        long now = System.currentTimeMillis();
        boolean paused = prefs.isPaused(now);

        int dotColor = R.color.od_dot_idle;
        switch (ProtectionStatus.of(serviceEnabled, paused)) {
            case OFF:
                statusText.setText(R.string.status_off);
                break;
            case PAUSED:
                String until = DateFormat.getTimeFormat(this).format(prefs.getPausedUntil());
                statusText.setText(getString(R.string.status_paused, until));
                break;
            case ACTIVE:
            default:
                statusText.setText(R.string.status_active);
                dotColor = R.color.od_accent;
                break;
        }
        statusDot.setBackgroundTintList(
                ColorStateList.valueOf(ContextCompat.getColor(this, dotColor)));
        serviceOffGroup.setVisibility(serviceEnabled ? View.GONE : View.VISIBLE);

        if (paused) {
            pauseButton.setText(R.string.btn_resume);
            pauseDesc.setVisibility(View.GONE);
        } else if (countdown.isRunning()) {
            int seconds = countdown.remainingSeconds(SystemClock.elapsedRealtime());
            countdownSeconds.setText(String.valueOf(seconds));
            countdownLabel.setText(
                    getResources().getQuantityString(R.plurals.countdown_label, seconds));
            pauseButton.setText(R.string.btn_pause_cancel);
            pauseDesc.setVisibility(View.VISIBLE);
        } else {
            pauseButton.setText(R.string.btn_pause);
            pauseDesc.setVisibility(View.VISIBLE);
        }
    }

    /** Junak se prelije med števcem in krogom odštevanja. */
    private void showCountdown(boolean show, boolean animate) {
        View in = show ? heroCountdown : heroCount;
        View out = show ? heroCount : heroCountdown;
        if (!show) {
            countdownRing.stop();
        }
        long d = animate ? HERO_CROSSFADE_MS : 0;
        out.animate().cancel();
        in.animate().cancel();
        out.animate().alpha(0f).setDuration(d)
                .withEndAction(() -> out.setVisibility(View.INVISIBLE)).start();
        in.setVisibility(View.VISIBLE);
        in.animate().alpha(1f).setDuration(d).start();
    }
}
