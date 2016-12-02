/*
 * Copyright © Microsoft Corporation. All rights reserved.
 */

package com.microsoft.azure.mobile.crashes.model;

import android.util.Base64;

import com.microsoft.azure.mobile.ingestion.models.Model;
import com.microsoft.azure.mobile.ingestion.models.json.JSONUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.Arrays;

/**
 * Binary attachment for error log.
 */
public class ErrorBinaryAttachment implements Model {

    /**
     * contentType property.
     */
    private static final String CONTENT_TYPE = "content_type";

    /**
     * fileName property.
     */
    private static final String FILE_NAME = "file_name";

    /**
     * data property.
     */
    private static final String DATA = "data";

    /**
     * Content type for binary data.
     */
    private String contentType;

    /**
     * File name for binary data.
     */
    private String fileName;

    /**
     * Binary data.
     */
    private byte[] data;

    /**
     * Get the contentType value.
     *
     * @return the contentType value
     */
    public String getContentType() {
        return this.contentType;
    }

    /**
     * Set the contentType value.
     *
     * @param contentType the contentType value to set
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * Get the fileName value.
     *
     * @return the fileName value
     */
    public String getFileName() {
        return this.fileName;
    }

    /**
     * Set the fileName value.
     *
     * @param fileName the fileName value to set
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Get the data value.
     *
     * @return the data value
     */
    public byte[] getData() {
        return this.data;
    }

    /**
     * Set the data value.
     *
     * @param data the data value to set
     */
    public void setData(byte[] data) {
        this.data = data;
    }

    @Override
    public void read(JSONObject object) throws JSONException {
        setContentType(object.getString(CONTENT_TYPE));
        setFileName(object.optString(FILE_NAME, null));
        if (object.optString(DATA, null) == null)
            setData(null);
        else
            setData(Base64.decode(object.getString(DATA), Base64.DEFAULT));
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        writer.key(CONTENT_TYPE).value(getContentType());
        JSONUtils.write(writer, FILE_NAME, getFileName());
        if (getData() != null)
            JSONUtils.write(writer, DATA, Base64.encodeToString(getData(), Base64.NO_WRAP));
    }

    @Override
    @SuppressWarnings("SimplifiableIfStatement")
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ErrorBinaryAttachment that = (ErrorBinaryAttachment) o;

        if (contentType != null ? !contentType.equals(that.contentType) : that.contentType != null)
            return false;
        if (fileName != null ? !fileName.equals(that.fileName) : that.fileName != null)
            return false;
        return Arrays.equals(data, that.data);

    }

    @Override
    public int hashCode() {
        int result = contentType != null ? contentType.hashCode() : 0;
        result = 31 * result + (fileName != null ? fileName.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }
}
