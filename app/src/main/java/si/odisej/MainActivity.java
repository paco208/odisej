package si.odisej;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import si.odisej.service.ServiceStatus;
import si.odisej.settings.Prefs;

public class MainActivity extends AppCompatActivity {

    private TextView statusTitle;
    private TextView statusDetail;
    private View restrictedTitle;
    private View restrictedBody;
    private View openAppInfo;
    private Prefs prefs;

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
    }
}
