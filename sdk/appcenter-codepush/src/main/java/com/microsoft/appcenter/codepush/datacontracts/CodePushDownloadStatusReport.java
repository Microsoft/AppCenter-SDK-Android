package com.microsoft.appcenter.codepush.datacontracts;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a report sent after downloading package.
 */
public class CodePushDownloadStatusReport {

    /**
     * The id of the device.
     */
    @SerializedName("clientUniqueId")
    private String clientUniqueId;

    /**
     * The deployment key to use to query the CodePush server for an update.
     */
    @SerializedName("deploymentKey")
    private String deploymentKey;

    /**
     * The internal label automatically given to the update by the CodePush server.
     * This value uniquely identifies the update within its deployment.
     */
    @SerializedName("label")
    private String label;

    /**
     * Creates a report using the provided information.
     *
     * @param clientUniqueId id of the device.
     * @param deploymentKey  deployment key to use to query the CodePush server for an update.
     * @param label          internal label automatically given to the update by the CodePush server.
     * @return instance of {@link CodePushDownloadStatusReport}.
     */
    public static CodePushDownloadStatusReport createReport(final String clientUniqueId, final String deploymentKey, final String label) {
        CodePushDownloadStatusReport codePushDownloadStatusReport = new CodePushDownloadStatusReport();
        codePushDownloadStatusReport.setClientUniqueId(clientUniqueId);
        codePushDownloadStatusReport.setDeploymentKey(deploymentKey);
        codePushDownloadStatusReport.setLabel(label);
        return codePushDownloadStatusReport;
    }


    /**
     * Gets the id of the device and returns it.
     *
     * @return the id of the device.
     */
    public String getClientUniqueId() {
        return clientUniqueId;
    }

    /**
     * Gets the deployment key to use to query the CodePush server for an update and returns it.
     *
     * @return the deployment key to use to query the CodePush server for an update.
     */
    public String getDeploymentKey() {
        return deploymentKey;
    }

    /**
     * Gets the internal label automatically given to the update by the CodePush server and returns it.
     *
     * @return the internal label automatically given to the update by the CodePush server.
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets the internal label automatically given to the update by the CodePush server.
     *
     * @param label the internal label automatically given to the update by the CodePush server.
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Sets the id of the device.
     *
     * @param clientUniqueId the id of the device.
     */
    @SuppressWarnings("WeakerAccess")
    public void setClientUniqueId(String clientUniqueId) {
        this.clientUniqueId = clientUniqueId;
    }

    /**
     * Sets the deployment key to use to query the CodePush server for an update.
     *
     * @param deploymentKey the deployment key to use to query the CodePush server for an update.
     */
    @SuppressWarnings("WeakerAccess")
    public void setDeploymentKey(String deploymentKey) {
        this.deploymentKey = deploymentKey;
    }
}
