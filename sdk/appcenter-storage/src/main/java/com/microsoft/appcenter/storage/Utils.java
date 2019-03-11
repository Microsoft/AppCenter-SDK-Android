// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.appcenter.storage;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.appcenter.http.HttpException;
import com.microsoft.appcenter.http.HttpUtils;
import com.microsoft.appcenter.storage.models.Document;
import com.microsoft.appcenter.storage.models.Page;
import com.microsoft.appcenter.utils.AppCenterLog;

import java.util.ArrayList;
import java.util.List;

public final class Utils {

    private static final Gson S_GSON = new Gson();

    private static final JsonParser S_PARSER = new JsonParser();

    static <T> Document<T> parseDocument(String cosmosDbPayload, Class<T> documentType) {
        return parseDocument(S_PARSER.parse(cosmosDbPayload).getAsJsonObject(), documentType);
    }

    private static <T> Document<T> parseDocument(JsonObject obj, Class<T> documentType) {
        T document = S_GSON.fromJson(obj.get(Constants.DOCUMENT_FIELD_NAME), documentType);
        try {
            return new Document<T>(
                    document,
                    obj.get(Constants.PARTITION_KEY_FIELD_NAME).getAsString(),
                    obj.get(Constants.ID_FIELD_NAME).getAsString(),
                    obj.has(Constants.ETAG_FIELD_NAME) ? obj.get(Constants.ETAG_FIELD_NAME).getAsString() : "",
                    obj.get(Constants.TIMESTAMP_FIELD_NAME).getAsLong());
        } catch (Exception exception) {
            return new Document<T>(exception);
        }
    }

    @SuppressWarnings("SameParameterValue")
    static <T> T fromJson(String doc, Class<T> type) {
        return S_GSON.fromJson(doc, type);
    }

    public static <T> Page<T> parseDocuments(String cosmosDbPayload, Class<T> documentType) {
        JsonObject objects = S_PARSER.parse(cosmosDbPayload).getAsJsonObject();
        JsonArray array = objects.get(Constants.DOCUMENTS_FILED_NAME).getAsJsonArray();
        List<Document<T>> documents = new ArrayList<>();
        for (JsonElement object : array) {
            documents.add(parseDocument(object.getAsJsonObject(), documentType));
        }
        return new Page<T>().withDocuments(documents);
    }

    /**
     * Handle API call failure.
     *
     * @param e Exception to display in the log.
     */
    public static synchronized void handleApiCallFailure(Exception e) {
        AppCenterLog.error(Constants.LOG_TAG, "Failed to call App Center APIs", e);
        if (!HttpUtils.isRecoverableError(e)) {
            if (e instanceof HttpException) {
                HttpException httpException = (HttpException) e;
                AppCenterLog.error(Constants.LOG_TAG, "Exception", httpException);
            }
        }
    }

    public static Gson getGson() {
        return S_GSON;
    }
}
