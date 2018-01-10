package com.microsoft.appcenter.codepush.enums;

import com.google.gson.annotations.SerializedName;

/**
 * A enum defining the state of an update.
 */
public enum CodePushUpdateState {

    @SerializedName("0")
    RUNNING(0),

    @SerializedName("1")
    PENDING(1),

    @SerializedName("2")
    LATEST(2);

    private final int value;
    
    CodePushUpdateState(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }
}