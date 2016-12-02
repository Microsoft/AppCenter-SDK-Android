/*
 * Copyright © Microsoft Corporation. All rights reserved.
 */

package com.microsoft.azure.mobile.ingestion.models;

import java.util.UUID;

public interface Log extends Model {

    /**
     * Get the type value.
     *
     * @return the type value
     */
    String getType();

    /**
     * Get the toffset value.
     *
     * @return the toffset value
     */
    long getToffset();

    /**
     * Set the toffset value.
     *
     * @param toffset the toffset value to set
     */
    void setToffset(long toffset);

    /**
     * Get the sid value.
     *
     * @return the sid value
     */
    UUID getSid();

    /**
     * Set the sid value.
     *
     * @param sid the sid value to set
     */
    void setSid(UUID sid);

    /**
     * Get the device value.
     *
     * @return the device value
     */
    @SuppressWarnings("unused")
    Device getDevice();

    /**
     * Set the device value.
     *
     * @param device the device value to set
     */
    void setDevice(Device device);
}
