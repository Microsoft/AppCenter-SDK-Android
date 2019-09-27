/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.download.http;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import com.microsoft.appcenter.distribute.PermissionUtils;
import com.microsoft.appcenter.distribute.R;
import com.microsoft.appcenter.distribute.ReleaseDetails;
import com.microsoft.appcenter.distribute.download.ReleaseDownloader;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.AsyncTaskUtils;
import com.microsoft.appcenter.utils.NetworkStateHelper;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import java.io.File;
import java.util.ArrayList;

import static com.microsoft.appcenter.distribute.DistributeConstants.KIBIBYTE_IN_BYTES;
import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE;

/**
 * Downloads new releases directly via HttpsURLConnection for Android versions prior to 5.0.
 */
public class HttpConnectionReleaseDownloader implements ReleaseDownloader {

    /**
     * Context.
     */
    private final Context mContext;

    /**
     * Release to download.
     */
    private final ReleaseDetails mReleaseDetails;

    /**
     * Listener of download status.
     */
    private final ReleaseDownloader.Listener mListener;

    /**
     * A file that used to write downloaded package.
     */
    private File mTargetFile;

    /**
     * Progress notification builder.
     * It must be stored to avoid notification flickering.
     */
    private Notification.Builder mNotificationBuilder;

    private HttpDownloadFileTask mHttpDownloadFileTask;

    public HttpConnectionReleaseDownloader(@NonNull Context context, @NonNull ReleaseDetails releaseDetails, @NonNull ReleaseDownloader.Listener listener) {
        mContext = context;
        mReleaseDetails = releaseDetails;
        mListener = listener;
    }

    @Nullable
    File getTargetFile() {
        if (mTargetFile == null) {
            File downloadsDirectory = mContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (downloadsDirectory != null) {
                mTargetFile = new File(downloadsDirectory, mReleaseDetails.getReleaseHash() + ".apk");
            }
        }
        return mTargetFile;
    }

    private NotificationManager getNotificationManager() {
        return (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    /**
     * Get progress notification builder.
     *
     * @see #mNotificationBuilder
     */
    @NonNull
    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    private Notification.Builder getNotificationBuilder() {
        if (mNotificationBuilder == null) {
            mNotificationBuilder = new Notification.Builder(mContext);
        }
        return mNotificationBuilder;
    }

    @Override
    public void resume() {
        File targetFile = getTargetFile();
        if (targetFile == null) {
            mListener.onError("Cannot access to downloads folder. Shared storage is not currently available.");
            return;
        }

        /* Check if we have already downloaded the release. */
        String downloadedReleaseFilePath = SharedPreferencesManager.getString(PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE, null);
        if (downloadedReleaseFilePath != null) {

            /* Check if it's the same release. */
            if (downloadedReleaseFilePath.equals(targetFile.getAbsolutePath())) {
                File downloadedReleaseFile = new File(downloadedReleaseFilePath);
                if (downloadedReleaseFile.exists()) {
                    onDownloadComplete(targetFile);
                    return;
                }
            } else {

                /* Remove previously downloaded release. */
                mContext.deleteFile(downloadedReleaseFilePath);
                SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE);
            }
        }
        if (!NetworkStateHelper.getSharedInstance(mContext).isNetworkConnected()) {
            mListener.onError("No network connection, abort downloading.");
            return;
        }
        String[] permissions = requiredPermissions();
        int[] permissionsState = PermissionUtils.permissionsState(mContext, permissions);
        if (!PermissionUtils.permissionsAreGranted(permissionsState)) {
            mListener.onError("No external storage permission.");
            return;
        }
        if (mHttpDownloadFileTask == null) {
            long enqueueTime = System.currentTimeMillis();
            mHttpDownloadFileTask = AsyncTaskUtils.execute(LOG_TAG, new HttpDownloadFileTask(this, mReleaseDetails.getDownloadUrl(), targetFile));
            mListener.onStart(enqueueTime);
            showProgressNotification(0, 0);
        } else {
            AppCenterLog.debug(LOG_TAG, "Downloading of " + targetFile.getPath() + " is already in progress.");
        }
    }

    @Override
    public void delete() {
        String filePath = SharedPreferencesManager.getString(PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE, null);
        if (filePath != null) {
            AsyncTaskUtils.execute(LOG_TAG, new RemoveFileTask(mContext, filePath));
            SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE);
        }
        cancelProgressNotification();
    }

    void showProgressNotification(long currentSize, long totalSize) {
        Notification.Builder builder = getNotificationBuilder();

        /* TODO: We should not reuse "mandatory" string here. */
        builder.setContentTitle(mContext.getString(R.string.appcenter_distribute_downloading_mandatory_update))
                .setSmallIcon(mContext.getApplicationInfo().icon)
                .setProgress((int) (totalSize / KIBIBYTE_IN_BYTES), (int) (currentSize / KIBIBYTE_IN_BYTES), totalSize <= 0);
        getNotificationManager().notify(getNotificationId(), builder.build());
    }

    private void cancelProgressNotification() {
        getNotificationManager().cancel(getNotificationId());
    }

    @WorkerThread
    void onDownloadProgress(final long currentSize, final long totalSize) {
        showProgressNotification(currentSize, totalSize);
        mListener.onProgress(currentSize, totalSize);
    }

    @WorkerThread
    void onDownloadComplete(File targetFile) {
        cancelProgressNotification();
        String downloadedReleaseFilePath = targetFile.getAbsolutePath();
        SharedPreferencesManager.putString(PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE, downloadedReleaseFilePath);
        mListener.onComplete(Uri.parse("file://" + downloadedReleaseFilePath));
    }

    void onDownloadError(String errorMessage) {
        mListener.onError(errorMessage);
    }

    private static String[] requiredPermissions() {
        ArrayList<String> permissions = new ArrayList<>();
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        return permissions.toArray(new String[0]);
    }

    /**
     * Get the notification identifier for displaying progress.
     *
     * @return notification identifier for displaying progress.
     */
    private static int getNotificationId() {
        return HttpConnectionReleaseDownloader.class.getName().hashCode();
    }
}