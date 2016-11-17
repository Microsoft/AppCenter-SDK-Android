/*
 * Copyright © Microsoft Corporation. All rights reserved.
 */

package com.microsoft.azure.mobile.sasquatch.features;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;

import com.microsoft.azure.mobile.crashes.Crashes;
import com.microsoft.azure.mobile.sasquatch.R;
import com.microsoft.azure.mobile.sasquatch.activities.DeviceInfoActivity;
import com.microsoft.azure.mobile.sasquatch.activities.DummyActivity;
import com.microsoft.azure.mobile.sasquatch.activities.EventActivity;
import com.microsoft.azure.mobile.sasquatch.activities.ManagedErrorActivity;
import com.microsoft.azure.mobile.sasquatch.activities.PageActivity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public final class TestFeatures {
    private static List<TestFeatureModel> sTestFeatureModel;
    private static WeakReference<Activity> sParentActivity;

    public static void initialize(Activity parentActivity) {
        sTestFeatureModel = new ArrayList<>();
        sParentActivity = new WeakReference<>(parentActivity);
        sTestFeatureModel.add(new TestFeatureModel(R.string.title_crash, R.string.description_crash, new View.OnClickListener() {

            @Override
            @SuppressWarnings({"ConstantConditions", "ResultOfMethodCallIgnored"})
            public void onClick(View v) {

                /* Make the app crash on purpose for testing report. */
                Crashes.generateTestCrash();
            }
        }));
        sTestFeatureModel.add(new TestFeatureModel(R.string.title_crash_2, R.string.description_crash_2, new View.OnClickListener() {

            @Override
            @SuppressWarnings({"ConstantConditions", "ResultOfMethodCallIgnored"})
            public void onClick(View v) {
                ("" + (42 / Integer.valueOf("0"))).toCharArray();
            }
        }));
        sTestFeatureModel.add(new TestFeatureModel(R.string.title_device_info, R.string.description_device_info, DeviceInfoActivity.class));
        sTestFeatureModel.add(new TestFeatureModel(R.string.title_error, R.string.description_error, ManagedErrorActivity.class));
        sTestFeatureModel.add(new TestFeatureModel(R.string.title_event, R.string.description_event, EventActivity.class));
        sTestFeatureModel.add(new TestFeatureModel(R.string.title_page, R.string.description_page, PageActivity.class));
        sTestFeatureModel.add(new TestFeatureModel(R.string.title_generate_page_log, R.string.description_generate_page_log, DummyActivity.class));
    }

    public static List<TestFeatureModel> getAvailableControls() {
        return sTestFeatureModel;
    }

    public static AdapterView.OnItemClickListener getOnItemClickListener() {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TestFeatureModel model = (TestFeatureModel) parent.getItemAtPosition(position);
                model.mOnClickListener.onClick(view);
            }
        };
    }

    private static View.OnClickListener getDefaultOnClickListener(final Class<? extends Activity> clazz) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sParentActivity.get().startActivity(new Intent(sParentActivity.get(), clazz));
            }
        };
    }

    static class TestFeatureModel {
        private final String mTitle;
        private final String mDescription;
        private final View.OnClickListener mOnClickListener;

        TestFeatureModel(int title, int description, Class<? extends Activity> clazz) {
            this(title, description, getDefaultOnClickListener(clazz));
        }

        TestFeatureModel(int title, int description, View.OnClickListener listener) {
            this.mTitle = title > 0 ? sParentActivity.get().getResources().getString(title) : "";
            this.mDescription = description > 0 ? sParentActivity.get().getResources().getString(description) : "";
            this.mOnClickListener = listener;
        }

        String getTitle() {
            return mTitle;
        }

        String getDescription() {
            return mDescription;
        }
    }
}
