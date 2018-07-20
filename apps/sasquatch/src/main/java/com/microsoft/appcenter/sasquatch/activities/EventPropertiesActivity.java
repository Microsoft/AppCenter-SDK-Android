package com.microsoft.appcenter.sasquatch.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.microsoft.appcenter.analytics.AnalyticsTransmissionTarget;
import com.microsoft.appcenter.sasquatch.R;
import com.microsoft.appcenter.sasquatch.util.EventActivityUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EventPropertiesActivity extends AppCompatActivity {

    public final static String EXTRA_TARGET_SELECTED = "TARGET_SELECTED";

    private Spinner mTransmissionTargetSpinner;

    private ListView mListView;

    private LinearLayout mAddPropertyLayout;

    private PropertyListAdapter mPropertyListAdapter;

    private List<AnalyticsTransmissionTarget> mTransmissionTargets = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_properties);

        /* Initialize spinner for transmission targets. */
        mTransmissionTargetSpinner = findViewById(R.id.transmission_target);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.target_id_names));
        mTransmissionTargetSpinner.setAdapter(adapter);
        mTransmissionTargetSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updatePropertyList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        mTransmissionTargetSpinner.setSelection(getIntent().getIntExtra(EXTRA_TARGET_SELECTED, 0));

        /* Initialize layout for a new property. */
        mAddPropertyLayout = findViewById(R.id.add_property);

        /* Initialize list view. */
        mListView = findViewById(R.id.list);
        mPropertyListAdapter = new PropertyListAdapter(new ArrayList<Pair<String, String>>());

        /*
         * Initialize analytics transmission targets.
         * The first element is a placeholder for default transmission.
         * The second one is the parent transmission target, the third one is a child,
         * the forth is a grandchild, etc...
         */
        mTransmissionTargets = EventActivityUtil.getAnalyticTransmissionTargetList(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        /* TODO remove reflection after overriding common schema properties feature is released. */
        AnalyticsTransmissionTarget target = getSelectedTarget();
        Object configurator;
        Method method;
        try {
            method = target.getClass().getDeclaredMethod("getPropertyConfigurator");
            configurator = method.invoke(target);
            method = configurator.getClass().getDeclaredMethod("setEventProperty");
        } catch (Exception e) {
            configurator = null;
            method = null;
        }
        if (method != null) {
            switch (item.getItemId()) {
                case R.id.action_add:
                    final Object finalConfigurator = configurator;
                    final Method finalMethod = method;
                    final TextView keyView = mAddPropertyLayout.findViewById(R.id.key);
                    final TextView valueView = mAddPropertyLayout.findViewById(R.id.value);
                    keyView.setText("");
                    valueView.setText("");
                    mAddPropertyLayout.setVisibility(View.VISIBLE);
                    mAddPropertyLayout.findViewById(R.id.add_button).setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            CharSequence key = keyView.getText();
                            CharSequence value = valueView.getText();
                            if (!TextUtils.isEmpty(key) && !TextUtils.isEmpty(value)) {
                                try {
                                    finalMethod.invoke(finalConfigurator, key.toString(), value.toString());
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                                updatePropertyList();
                            }
                            mAddPropertyLayout.setVisibility(View.GONE);
                        }
                    });
                    mAddPropertyLayout.findViewById(R.id.cancel_button).setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            mAddPropertyLayout.setVisibility(View.GONE);
                        }
                    });
                    break;
            }
        }
        return true;
    }

    private void updatePropertyList() {

        /* TODO remove reflection after overriding common schema properties feature is released. */
        AnalyticsTransmissionTarget target = getSelectedTarget();
        Object configurator;
        Method method;
        Field field;
        try {
            method = target.getClass().getDeclaredMethod("getPropertyConfigurator");
            configurator = method.invoke(target);
            field = configurator.getClass().getDeclaredField("mEventProperties");
        } catch (Exception e) {
            field = null;
            configurator = null;
        }
        if (field != null) {
            try {
                field.setAccessible(true);

                //noinspection unchecked
                Map<String, String> map = (Map<String, String>) field.get(configurator);
                mPropertyListAdapter.mList.clear();
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    mPropertyListAdapter.mList.add(new Pair<>(entry.getKey(), entry.getValue()));
                }
                mListView.setAdapter(mPropertyListAdapter);
                mPropertyListAdapter.notifyDataSetChanged();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private AnalyticsTransmissionTarget getSelectedTarget() {
        return mTransmissionTargets.get(mTransmissionTargetSpinner.getSelectedItemPosition());
    }

    private class PropertyListAdapter extends BaseAdapter {

        private final static String KEY_VALUE_PAIR_FORMAT = "\"%s\":\"%s\"";

        private final List<Pair<String, String>> mList;

        private PropertyListAdapter(List<Pair<String, String>> list) {
            mList = list;
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public Object getItem(int position) {
            return mList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            /* TODO remove reflection after overriding common schema properties feature is released. */
            AnalyticsTransmissionTarget target = getSelectedTarget();
            Object configurator;
            Method method;
            try {
                method = target.getClass().getDeclaredMethod("getPropertyConfigurator");
                configurator = method.invoke(target);
                method = configurator.getClass().getDeclaredMethod("removeEventProperty");
            } catch (Exception e) {
                configurator = null;
                method = null;
            }

            /* Set key and value strings to the view. */
            View rowView;
            if (method != null) {
                final Object finalConfigurator = configurator;
                final Method finalMethod = method;

                //noinspection unchecked
                final Pair<String, String> item = (Pair<String, String>) getItem(position);
                ViewHolder holder;
                if (convertView != null && convertView.getTag() != null) {
                    holder = (ViewHolder) convertView.getTag();
                    rowView = convertView;
                } else {
                    rowView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_view_property, parent, false);
                    TextView textView = rowView.findViewById(R.id.property);
                    ImageButton imageButton = rowView.findViewById(R.id.delete_button);
                    holder = new ViewHolder(textView, imageButton);
                    rowView.setTag(holder);
                }
                holder.mTextView.setText(String.format(KEY_VALUE_PAIR_FORMAT, item.first, item.second));
                holder.mImageButton.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        mList.remove(item);
                        try {
                            finalMethod.invoke(finalConfigurator, item.first);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        notifyDataSetChanged();
                    }
                });
            } else {
                rowView = null;
            }
            return rowView;
        }

        private class ViewHolder {

            private final TextView mTextView;

            private final ImageButton mImageButton;

            private ViewHolder(TextView textView, ImageButton imageButton) {
                mTextView = textView;
                mImageButton = imageButton;
            }
        }
    }
}
