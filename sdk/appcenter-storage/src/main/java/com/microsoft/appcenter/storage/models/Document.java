// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.appcenter.storage.models;

import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;
import com.microsoft.appcenter.storage.Constants;
import com.microsoft.appcenter.storage.Utils;
import com.microsoft.appcenter.storage.exception.StorageException;

/**
 * A document coming back from CosmosDB.
 */
public class Document<T> {

    @SerializedName(value = Constants.PARTITION_KEY_FIELD_NAME)
    private String partition;

    @SerializedName(value = Constants.ID_FIELD_NAME)
    private String id;

    @SerializedName(value = Constants.ETAG_FIELD_NAME)
    private String eTag;

    @SerializedName(value = Constants.TIMESTAMP_FIELD_NAME)
    private long timestamp;

    @SerializedName(value = Constants.DOCUMENT_FIELD_NAME)
    private T document;

    private transient DocumentError documentError;

    private transient boolean mFromCache;

    public Document() {
    }

    public Document(T document, String partition, String id) {
        this.partition = partition;
        this.id = id;
        this.document = document;
    }

    public Document(T document, String partition, String id, String eTag, long timestamp) {
        this(document, partition, id);
        this.eTag = eTag;
        this.timestamp = timestamp;
        this.document = document;
    }

    public Document(Throwable exception) {
        this.documentError = new DocumentError(exception);
    }

    public Document(String message, Throwable exception) {
        this.documentError = new DocumentError(new StorageException(message, exception));
    }

    /**
     * Get document.
     *
     * @return Deserialized document.
     */
    public T getDocument() {
        return document;
    }

    /**
     * Get document error.
     *
     * @return Document error.
     */
    public DocumentError getError() {
        return documentError;
    }

    /**
     * Get document partition.
     *
     * @return Document partition.
     */
    public String getPartition() {
        return partition;
    }

    /**
     * Get document id.
     *
     * @return Document id.
     */
    public String getId() {
        return id;
    }

    /**
     * Get Etag.
     *
     * @return Etag.
     */
    public String getEtag() {
        return eTag;
    }

    /**
     * Get document generated in UTC unix epoch.
     *
     * @return UTC unix timestamp.
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Get the document in string.
     *
     * @return Serialized document.
     */
    @NonNull
    @Override
    public String toString() {
        return Utils.getGson().toJson(this);
    }

    /**
     * Get the flag indicating if data was retrieved from the local cache (for offline mode)
     */
    public boolean isFromCache() {
        return mFromCache;
    }

    /**
     * Set the flag indicating if data was retrieved from the local cache (for offline mode)
     */
    public void setIsFromCache(boolean fromCache) {
        this.mFromCache = fromCache;
    }

    /**
     * @return whether the document has an error associated with it
     */
    public boolean failed() {
        return this.getError() != null;
    }
}
