// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.appcenter.storage;

import com.microsoft.appcenter.utils.AppCenterLog;

/**
 * Constants for Storage module.
 */
public final class Constants {

    public static final String TOKEN_RESULT_SUCCEED = "Succeed";

    public static final String DOCUMENT_FIELD_NAME = "document";

    public static final String DOCUMENTS_FILED_NAME = "Documents";

    public static final String PARTITION_KEY_FIELD_NAME = "PartitionKey";

    public static final String ID_FIELD_NAME = "id";

    public static final String ETAG_FIELD_NAME = "_etag";

    public static final String TIMESTAMP_FIELD_NAME = "_ts";

    /**
     * Cached partition names list file name.
     */
    static final String PARTITION_NAMES = "partitions";

    /**
     * Name of the service.
     */
    static final String SERVICE_NAME = "Storage";

    /**
     * TAG used in logging for Storage.
     */
    public static final String LOG_TAG = AppCenterLog.LOG_TAG + SERVICE_NAME;

    /**
     * Constant marking event of the storage group.
     */
    static final String STORAGE_GROUP = "group_storage";

    /**
     * User partition.
     * An authenticated user can read/write documents in this partition.
     */
    public static final String USER = "user-{%s}";

    /**
     * Readonly partition.
     * Everyone can read documents in this partition.
     * Writes are not allowed via the SDK.
     */
    public static final String READONLY = "readonly";

    /**
     * The continuation token header used to set continuation token.
     */
    public static final String CONTINUATION_TOKEN_HEADER = "x-ms-continuation";
}
