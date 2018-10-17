package com.microsoft.appcenter.sasquatch.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.analytics.AnalyticsTransmissionTarget;
import com.microsoft.appcenter.analytics.EventProperties;
import com.microsoft.appcenter.sasquatch.R;
import com.microsoft.appcenter.sasquatch.fragments.TypedPropertyFragment;
import com.microsoft.appcenter.sasquatch.util.EventActivityUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EventActivity extends AppCompatActivity {

    /**
     * Remember for what targets the device id was enabled.
     * It shouldn't be lost on recreate activity.
     */
    private static final Set<AnalyticsTransmissionTarget> DEVICE_ID_ENABLED = new HashSet<>();

    private Spinner mTransmissionTargetSpinner;

    private CheckBox mTransmissionEnabledCheckBox;

    private CheckBox mDeviceIdEnabledCheckBox;

    private Button mConfigureTargetPropertiesButton;

    private Button mOverrideCommonSchemaButton;

    private Button mPauseTransmissionButton;

    private Button mResumeTransmissionButton;

    private List<AnalyticsTransmissionTarget> mTransmissionTargets = new ArrayList<>();

    private final List<TypedPropertyFragment> mProperties = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event);

        /* Test start from library. */
        AppCenter.startFromLibrary(this, Analytics.class);

        /* Transmission target views init. */
        mTransmissionTargetSpinner = findViewById(R.id.transmission_target);
        ArrayAdapter<String> targetAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.target_id_names));
        mTransmissionTargetSpinner.setAdapter(targetAdapter);
        mTransmissionTargetSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateButtonStates(getSelectedTarget());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        /* Init Configure target properties button. */
        mConfigureTargetPropertiesButton = findViewById(R.id.configure_button);
        mConfigureTargetPropertiesButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(EventActivity.this, EventPropertiesActivity.class);
                intent.putExtra(ActivityConstants.EXTRA_TARGET_SELECTED, mTransmissionTargetSpinner.getSelectedItemPosition() - 1);
                startActivity(intent);
            }
        });

        /* Init enabled check boxes. */
        mTransmissionEnabledCheckBox = findViewById(R.id.transmission_enabled);
        mDeviceIdEnabledCheckBox = findViewById(R.id.device_id_enabled);

        /*
         * The first element is a placeholder for default transmission.
         * The second one is the parent transmission target, the third one is a child,
         * the forth is a grandchild, etc...
         */
        mTransmissionTargets = EventActivityUtil.getAnalyticTransmissionTargetList(this);

        /* Init common schema properties button. */
        mOverrideCommonSchemaButton = findViewById(R.id.override_cs_button);

        /* Init override common schema properties button. */
        mOverrideCommonSchemaButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(EventActivity.this, CommonSchemaPropertiesActivity.class);
                intent.putExtra(ActivityConstants.EXTRA_TARGET_SELECTED, mTransmissionTargetSpinner.getSelectedItemPosition());
                startActivity(intent);
            }
        });

        /* Init pause/resume buttons. */
        mPauseTransmissionButton = findViewById(R.id.pause_transmission_button);
        mPauseTransmissionButton.setOnClickListener(new View.OnClickListener() {

            @Override
            @SuppressWarnings("ConstantConditions")
            public void onClick(View v) {
                getSelectedTarget().pause();
            }
        });
        mResumeTransmissionButton = findViewById(R.id.resume_transmission_button);
        mResumeTransmissionButton.setOnClickListener(new View.OnClickListener() {

            @Override
            @SuppressWarnings("ConstantConditions")
            public void onClick(View v) {
                getSelectedTarget().resume();
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add:
                addProperty();
                break;
        }
        return true;
    }

    private void addProperty() {
        TypedPropertyFragment fragment = new TypedPropertyFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.list, fragment).commit();
        mProperties.add(fragment);
    }

    private boolean onlyStringProperties() {
        for (TypedPropertyFragment fragment : mProperties) {
            if (fragment.getType() != TypedPropertyFragment.EventPropertyType.STRING) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unused")
    public void send(@SuppressWarnings("UnusedParameters") View view) {
        String name = ((TextView) findViewById(R.id.name)).getText().toString();
        Map<String, String> properties = null;
        EventProperties typedProperties = null;
        if (mProperties.size() > 0) {
            if (onlyStringProperties()) {
                properties = new HashMap<>();
                for (TypedPropertyFragment fragment : mProperties) {
                    fragment.set(properties);
                }
            } else {
                typedProperties = new EventProperties();
                for (TypedPropertyFragment fragment : mProperties) {
                    fragment.set(typedProperties);
                }
            }
        }

        /* First item is always empty as it's default value which means either AppCenter, one collector or both. */
        AnalyticsTransmissionTarget target = getSelectedTarget();
        if (target == null) {
            if (typedProperties != null) {
                Analytics.trackEvent(name, typedProperties);
            } else if (properties != null) {
                Analytics.trackEvent(name, properties);
            } else {
                Analytics.trackEvent(name);
            }
        } else {
            if (typedProperties != null) {
                target.trackEvent(name, typedProperties);
            } else if (properties != null) {
                target.trackEvent(name, properties);
            } else {
                target.trackEvent(name);
            }
        }
    }

    private AnalyticsTransmissionTarget getSelectedTarget() {
        return mTransmissionTargets.get(mTransmissionTargetSpinner.getSelectedItemPosition());
    }

    public void toggleTransmissionEnabled(View view) {
        boolean checked = mTransmissionEnabledCheckBox.isChecked();
        final AnalyticsTransmissionTarget target = getSelectedTarget();
        if (target != null) {
            target.setEnabledAsync(checked);
            updateButtonStates(target);
        }
    }

    public void toggleDeviceIdEnabled(View view) {
        final AnalyticsTransmissionTarget target = getSelectedTarget();
        if (target != null) {
            updateButtonStates(target);
            target.getPropertyConfigurator().collectDeviceId();
            mDeviceIdEnabledCheckBox.setChecked(true);
            mDeviceIdEnabledCheckBox.setText(R.string.device_id_enabled);
            mDeviceIdEnabledCheckBox.setEnabled(false);
            DEVICE_ID_ENABLED.add(target);
        }
    }

    private void updateButtonStates(AnalyticsTransmissionTarget target) {
        if (target == null) {
            mTransmissionEnabledCheckBox.setVisibility(View.GONE);
            mConfigureTargetPropertiesButton.setVisibility(View.GONE);
            mOverrideCommonSchemaButton.setVisibility(View.GONE);
            mDeviceIdEnabledCheckBox.setVisibility(View.GONE);
            mPauseTransmissionButton.setVisibility(View.GONE);
            mResumeTransmissionButton.setVisibility(View.GONE);
        } else {
            mTransmissionEnabledCheckBox.setVisibility(View.VISIBLE);
            mConfigureTargetPropertiesButton.setVisibility(View.VISIBLE);
            mOverrideCommonSchemaButton.setVisibility(View.VISIBLE);
            mPauseTransmissionButton.setVisibility(View.VISIBLE);
            mResumeTransmissionButton.setVisibility(View.VISIBLE);
            boolean enabled = target.isEnabledAsync().get();
            mTransmissionEnabledCheckBox.setChecked(enabled);
            mTransmissionEnabledCheckBox.setText(enabled ? R.string.transmission_enabled : R.string.transmission_disabled);
            boolean deviceIdEnabled = DEVICE_ID_ENABLED.contains(target);
            mDeviceIdEnabledCheckBox.setVisibility(View.VISIBLE);
            mDeviceIdEnabledCheckBox.setChecked(deviceIdEnabled);
            mDeviceIdEnabledCheckBox.setText(deviceIdEnabled ? R.string.device_id_enabled : R.string.device_id_disabled);
            mDeviceIdEnabledCheckBox.setEnabled(!deviceIdEnabled);
        }
    }
}
