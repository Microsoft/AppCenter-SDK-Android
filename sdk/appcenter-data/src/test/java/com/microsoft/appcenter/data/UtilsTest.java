/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.data;

import com.google.gson.JsonParseException;
import com.microsoft.appcenter.data.client.CosmosDb;
import com.microsoft.appcenter.data.client.TokenExchange;
import com.microsoft.appcenter.data.models.DocumentWrapper;
import com.microsoft.appcenter.data.models.Page;
import com.microsoft.appcenter.data.models.TokenResult;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


public class UtilsTest {

    @Test
    public void parseDocumentCanHandleInvalidJson() {
        DocumentWrapper<TestDocument> document = Utils.parseDocument("{}", TestDocument.class);
        assertNotNull(document.getError());
    }

    @Test
    public void parseDocumentsCanHandleInvalidJson() {
        Page<TestDocument> page = Utils.parseDocuments("", TestDocument.class);
        assertNotNull(page.getError());
    }

    @Test
    public void canParseWhenDocumentNull() {
        DocumentWrapper<TestDocument> document = Utils.parseDocument(null, TestDocument.class);
        assertNotNull(document.getError());
    }

    @Test
    public void canParseWhenPassedWrongType() {
        TestDocument testDoc = new TestDocument("test-value");
        DocumentWrapper<TestDocument> doc = new DocumentWrapper<>(testDoc, "partition", "id");
        DocumentWrapper<String> document = Utils.parseDocument(doc.toString(), String.class);
        assertNotNull(document.getError());
    }

    @Test
    public void jsonValueIsUserDocument() {
        TestDocument testDoc = new TestDocument("test-value");
        DocumentWrapper<TestDocument> doc = new DocumentWrapper<>(testDoc, "partition", "id");
        assertNotNull(doc.getJsonValue());
        assertEquals(doc.getJsonValue(), Utils.getGson().toJson(testDoc));
        assertNotEquals(doc.getJsonValue(), doc.toString());
    }

    @Test
    public void getETag() {
        assertNull(Utils.getEtag(null));
        assertNull(Utils.getEtag(""));
        assertNull(Utils.getEtag("{a:1}"));
    }

    @Test
    public void doesNotAlterReadOnlyPartitionName() {
        assertEquals(DefaultPartitions.APP_DOCUMENTS, Utils.removeAccountIdFromPartitionName(DefaultPartitions.APP_DOCUMENTS));
    }

    @Test
    public void removeAccountIdFromPartitionName() {
        String partition = "user";
        String partitionNameWithAccountId = partition + "-" + "bd45f90e-6eb1-4c47-817e-e59b82b5c03d";
        assertEquals(partition, Utils.removeAccountIdFromPartitionName(partitionNameWithAccountId));
    }

    @Test
    public void checkETagNullByDefault() {
        DocumentWrapper<Void> document = new DocumentWrapper<>(null, "readonly", "id");
        document = Utils.parseDocument(Utils.getGson().toJson(document), Void.class);
        assertNotNull(document);
        assertNull(document.getError());
        assertNull(document.getETag());
    }

    @Test
    public void checkIsoDate() {

        /* Serialize a document with a date. */
        DateDocument dateDocument = new DateDocument();
        dateDocument.date = new Date(123153214234L);
        DocumentWrapper<DateDocument> doc = new DocumentWrapper<>(dateDocument, "partition", "id");
        String payload = Utils.getGson().toJson(doc);

        /* Check ISO format. */
        String expectedDate = "1973-11-26T09:13:34.234Z";
        assertTrue(payload.contains(expectedDate));

        /* Check we can parse back. */
        DocumentWrapper<DateDocument> document = Utils.parseDocument(payload, DateDocument.class);
        assertNull(document.getError());
        assertNotNull(document.getDeserializedValue());
        assertEquals(dateDocument.date, document.getDeserializedValue().date);
    }

    @Test
    public void parseInvalidDate() {

        /* Corrupt date format after serialization. */
        DateDocument dateDocument = new DateDocument();
        dateDocument.date = new Date(123153214234L);
        String expectedDate = "1973-11-26T09:13:34.234Z";
        DocumentWrapper<DateDocument> doc = new DocumentWrapper<>(dateDocument, "partition", "id");
        String payload = Utils.getGson().toJson(doc);
        assertTrue(payload.contains(expectedDate));
        payload = payload.replace(expectedDate, "1973/11/26 09:13:34");

        /* Check parsing error. */
        DocumentWrapper<DateDocument> document = Utils.parseDocument(payload, DateDocument.class);
        assertNotNull(document.getError());
        assertTrue(document.getError().getCause() instanceof JsonParseException);
        assertNull(document.getDeserializedValue());
    }


    @Test
    public void setAccountId() {
        TokenResult result = new TokenResult();
        result.setAccountId("someId");
        assertEquals("someId", result.getAccountId());
    }

    @Test
    public void constructors() {
        new Constants();
        new TokenExchange();
        new CosmosDb();
        new Utils();
        new TimeToLive();
        new DefaultPartitions();
    }

    private class DateDocument {

        Date date;
    }
}
