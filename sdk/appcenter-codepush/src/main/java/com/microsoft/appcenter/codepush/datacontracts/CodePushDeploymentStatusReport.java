package com.microsoft.appcenter.codepush.datacontracts;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a report about the deployment.
 */
public class CodePushDeploymentStatusReport extends CodePushDownloadStatusReport {

    /**
     * The version of the app that was deployed (for a native app upgrade).
     */
    @SerializedName("appVersion")
    private String appVersion;

    /**
     * Deployment key used when deploying the previous package.
     */
    @SerializedName("previousDeploymentKey")
    private String previousDeploymentKey;

    /**
     * The label (v#) of the package that was upgraded from.
     */
    @SerializedName("previousLabelOrAppVersion")
    private String previousLabelOrAppVersion;

    /**
     * Whether the deployment succeeded or failed.
     */
    @SerializedName("status")
    private String status;

    /**
     * Gets the version of the app that was deployed and returns it.
     *
     * @return the version of the app that was deployed.
     */
    public String getAppVersion() {
        return appVersion;
    }

    /**
     * Sets the version of the app that was deployed.
     *
     * @param appVersion the version of the app that was deployed.
     */
    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    /**
     * Gets deployment key used when deploying the previous package and returns it.
     *
     * @return deployment key used when deploying the previous package.
     */
    public String getPreviousDeploymentKey() {
        return previousDeploymentKey;
    }

    /**
     * Sets deployment key used when deploying the previous package.
     *
     * @param previousDeploymentKey deployment key used when deploying the previous package.
     */
    public void setPreviousDeploymentKey(String previousDeploymentKey) {
        this.previousDeploymentKey = previousDeploymentKey;
    }

    /**
     * Gets the label (v#) of the package that was upgraded from and returns it.
     *
     * @return the label (v#) of the package that was upgraded from.
     */
    public String getPreviousLabelOrAppVersion() {
        return previousLabelOrAppVersion;
    }

    /**
     * Sets the label (v#) of the package that was upgraded from.
     *
     * @param previousLabelOrAppVersion the label (v#) of the package that was upgraded from.
     */
    public void setPreviousLabelOrAppVersion(String previousLabelOrAppVersion) {
        this.previousLabelOrAppVersion = previousLabelOrAppVersion;
    }

    /**
     * Gets whether the deployment succeeded or failed and returns it.
     *
     * @return whether the deployment succeeded or failed.
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets whether the deployment succeeded or failed.
     *
     * @param status whether the deployment succeeded or failed.
     */
    public void setStatus(String status) {
        this.status = status;
    }
}
