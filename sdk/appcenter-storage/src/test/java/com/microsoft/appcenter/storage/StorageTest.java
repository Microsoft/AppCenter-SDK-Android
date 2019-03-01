package com.microsoft.appcenter.storage;

import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.HttpException;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.ingestion.Ingestion;
import com.microsoft.appcenter.ingestion.models.json.LogFactory;
import com.microsoft.appcenter.storage.client.CosmosDb;
import com.microsoft.appcenter.storage.client.TokenExchange;
import com.microsoft.appcenter.storage.models.Document;
import com.microsoft.appcenter.storage.models.Documents;
import com.microsoft.appcenter.utils.async.AppCenterFuture;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

import java.util.HashMap;
import java.util.Map;

import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_DELETE;
import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_GET;
import static com.microsoft.appcenter.http.DefaultHttpClient.METHOD_POST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.endsWith;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;

public class StorageTest extends AbstractStorageTest {

    private static final String DATABASE_NAME = "mbaas";
    private static final String COLLECTION_NAME = "appcenter";
    private static final String PARTITION = "custom-partition";
    private static final String DOCUMENT_ID = "document-id";
    private static final String TEST_FIELD_VALUE = "Test Value";
    private static String tokenExchangeResponsePayload = String.format("{\n" +
            "    \"tokens\": [\n" +
            "        {\n" +
            "            \"partition\": \"readonly\",\n" +
            "            \"dbAccount\": \"lemmings-01-8f37d78902\",\n" +
            "            \"dbName\": \"%s\",\n" +
            "            \"dbCollectionName\": \"%s\",\n" +
            "            \"token\": \"ha-ha-ha-ha\",\n" +
            "            \"status\": \"Succeed\"\n" +
            "        }\n" +
            "    ]\n" +
            "}", DATABASE_NAME, COLLECTION_NAME);

    private static String tokenExchangeFailedResponsePayload = "{\n" +
            "    \"tokens\": [\n" +
            "        {\n" +
            "            \"partition\": \"\",\n" +
            "            \"dbAccount\": \"\",\n" +
            "            \"dbName\": \"\",\n" +
            "            \"dbCollectionName\": \"\",\n" +
            "            \"token\": \"\",\n" +
            "            \"status\": \"Failed\"\n" +
            "        }\n" +
            "    ]\n" +
            "}";

    private static String cosmosDbDocumentResponsePayload = String.format("{\n" +
            "    \"document\": {\n" +
            "        \"test\": \"%s\"\n" +
            "    },\n" +
            "    \"id\": \"%s\",\n" +
            "    \"PartitionKey\": \"%s\",\n" +
            "    \"_rid\": \"mFBtAPPa528HAAAAAAAAAA==\",\n" +
            "    \"_self\": \"dbs/mFBtAA==/colls/mFBtAPPa528=/docs/mFBtAPPa528HAAAAAAAAAA==/\",\n" +
            "    \"_etag\": \"\\\"06000da6-0000-0000-0000-5c7093c30000\\\"\",\n" +
            "    \"_attachments\": \"attachments/\",\n" +
            "    \"_ts\": 1550881731\n" +
            "}", TEST_FIELD_VALUE, DOCUMENT_ID, PARTITION);
    @Captor
    private ArgumentCaptor<Map<String, String>> mHeadersCaptor;

    @Test
    public void singleton() {
        Assert.assertSame(Storage.getInstance(), Storage.getInstance());
    }

    @Test
    public void isAppSecretRequired() {
        assertTrue(Storage.getInstance().isAppSecretRequired());
    }

    @Test
    public void checkFactories() {
        Map<String, LogFactory> factories = Storage.getInstance().getLogFactories();
        assertNull(factories);
    }

    @Test
    public void setEnabled() {
        Storage.setEnabled(true);

        verify(channel).removeGroup(eq(storage.getGroupName()));
        verify(channel).addGroup(eq(storage.getGroupName()), anyInt(), anyLong(), anyInt(), isNull(Ingestion.class), any(Channel.GroupListener.class));

        /* Now we can see the service enabled. */
        assertTrue(Storage.isEnabled().get());

        /* Disable. Testing to wait setEnabled to finish while we are at it. */
        Storage.setEnabled(false).get();
        assertFalse(Storage.isEnabled().get());
    }

    @Test
    public void disablePersisted() {
        when(SharedPreferencesManager.getBoolean(STORAGE_ENABLED_KEY, true)).thenReturn(false);
        verify(channel, never()).removeListener(any(Channel.Listener.class));
        verify(channel, never()).addListener(any(Channel.Listener.class));
    }

    @Test
    public void listEndToEnd() {
        AppCenterFuture<Documents<TestDocument>> docs = Storage.list(PARTITION, TestDocument.class);
    }

    @Test
    public void replaceEndToEnd() {
        AppCenterFuture<Document<TestDocument>> doc = Storage.replace(PARTITION, DOCUMENT_ID, new TestDocument(TEST_FIELD_VALUE), TestDocument.class);
    }

    @Test
    public void readEndToEnd() throws Exception {
        AppCenterFuture<Document<TestDocument>> doc = Storage.read(PARTITION, DOCUMENT_ID, TestDocument.class);

        ArgumentCaptor<HttpClient.CallTemplate> tokenExchangeCallTemplateCallbackArgumentCaptor =
                ArgumentCaptor.forClass(HttpClient.CallTemplate.class);
        ArgumentCaptor<TokenExchange.TokenExchangeServiceCallback> tokenExchangeServiceCallbackArgumentCaptor =
                ArgumentCaptor.forClass(TokenExchange.TokenExchangeServiceCallback.class);
        verify(httpClient).callAsync(
                endsWith(TokenExchange.GET_TOKEN_PATH_FORMAT),
                eq(METHOD_POST),
                anyMapOf(String.class, String.class),
                tokenExchangeCallTemplateCallbackArgumentCaptor.capture(),
                tokenExchangeServiceCallbackArgumentCaptor.capture());
        tokenExchangeCallTemplateCallbackArgumentCaptor.getValue().buildRequestBody();
        tokenExchangeCallTemplateCallbackArgumentCaptor.getValue().onBeforeCalling(null, new HashMap<String, String>());
        TokenExchange.TokenExchangeServiceCallback tokenExchangeServiceCallback = tokenExchangeServiceCallbackArgumentCaptor.getValue();
        assertNotNull(tokenExchangeServiceCallback);
        tokenExchangeServiceCallback.onCallSucceeded(tokenExchangeResponsePayload, new HashMap<String, String>());

        ArgumentCaptor<HttpClient.CallTemplate> cosmosDbCallTemplateCallbackArgumentCaptor =
                ArgumentCaptor.forClass(HttpClient.CallTemplate.class);
        ArgumentCaptor<ServiceCallback> cosmosDbServiceCallbackArgumentCaptor =
                ArgumentCaptor.forClass(ServiceCallback.class);
        verify(httpClient).callAsync(
                endsWith(CosmosDb.getDocumentBaseUrl(DATABASE_NAME, COLLECTION_NAME, DOCUMENT_ID)),
                eq(METHOD_GET),
                anyMapOf(String.class, String.class),
                cosmosDbCallTemplateCallbackArgumentCaptor.capture(),
                cosmosDbServiceCallbackArgumentCaptor.capture());
        cosmosDbCallTemplateCallbackArgumentCaptor.getValue().buildRequestBody();
        cosmosDbCallTemplateCallbackArgumentCaptor.getValue().onBeforeCalling(null, new HashMap<String, String>());
        ServiceCallback cosmosDbServiceCallback = cosmosDbServiceCallbackArgumentCaptor.getValue();
        assertNotNull(cosmosDbServiceCallback);
        cosmosDbServiceCallback.onCallSucceeded(cosmosDbDocumentResponsePayload, new HashMap<String, String>());

        assertNotNull(doc);

        Document<TestDocument> testCosmosDocument = doc.get();
        assertNotNull(testCosmosDocument);
        assertEquals(PARTITION, testCosmosDocument.getPartition());
        assertEquals(DOCUMENT_ID, testCosmosDocument.getId());
        assertNull(testCosmosDocument.getError());
        assertNotNull(testCosmosDocument.getEtag());
        assertNotNull(testCosmosDocument.getTimestamp());

        TestDocument testDocument = testCosmosDocument.getDocument();
        assertNotNull(testDocument);
        assertEquals(TEST_FIELD_VALUE, testDocument.test);
    }

    @Test
    public void readFailTokenExchangeReturnsFailedTokenResultPayload() {
        AppCenterFuture<Document<TestDocument>> doc = Storage.read(PARTITION, DOCUMENT_ID, TestDocument.class);

        ArgumentCaptor<TokenExchange.TokenExchangeServiceCallback> tokenExchangeServiceCallbackArgumentCaptor =
                ArgumentCaptor.forClass(TokenExchange.TokenExchangeServiceCallback.class);
        verify(httpClient).callAsync(
                endsWith(TokenExchange.GET_TOKEN_PATH_FORMAT),
                eq(METHOD_POST),
                anyMapOf(String.class, String.class),
                any(HttpClient.CallTemplate.class),
                tokenExchangeServiceCallbackArgumentCaptor.capture());
        TokenExchange.TokenExchangeServiceCallback tokenExchangeServiceCallback = tokenExchangeServiceCallbackArgumentCaptor.getValue();
        assertNotNull(tokenExchangeServiceCallback);
        tokenExchangeServiceCallback.onCallSucceeded(tokenExchangeFailedResponsePayload, new HashMap<String, String>());

        /*
         *  No retries and Cosmos DB does not get called
         */
        verifyNoMoreInteractions(httpClient);
        assertNotNull(doc);
        assertNotNull(doc.get());
        assertNull(doc.get().getDocument());
        assertNotNull(doc.get().getError());
        assertThat(
                doc.get().getError().getError().getMessage(),
                CoreMatchers.containsString(tokenExchangeFailedResponsePayload));
    }

    @Test
    public void readTokenExchangeCallFails() {
        AppCenterFuture<Document<TestDocument>> doc = Storage.read(PARTITION, DOCUMENT_ID, TestDocument.class);

        ArgumentCaptor<TokenExchange.TokenExchangeServiceCallback> tokenExchangeServiceCallbackArgumentCaptor =
                ArgumentCaptor.forClass(TokenExchange.TokenExchangeServiceCallback.class);
        verify(httpClient).callAsync(
                endsWith(TokenExchange.GET_TOKEN_PATH_FORMAT),
                eq(METHOD_POST),
                anyMapOf(String.class, String.class),
                any(HttpClient.CallTemplate.class),
                tokenExchangeServiceCallbackArgumentCaptor.capture());
        TokenExchange.TokenExchangeServiceCallback tokenExchangeServiceCallback = tokenExchangeServiceCallbackArgumentCaptor.getValue();
        assertNotNull(tokenExchangeServiceCallback);
        String exceptionMessage = "Call to token exchange failed for whatever reason";
        tokenExchangeServiceCallback.onCallFailed(new Exception(exceptionMessage));

        /*
         *  No retries and Cosmos DB does not get called
         */
        verifyNoMoreInteractions(httpClient);
        assertNotNull(doc);
        assertNotNull(doc.get());
        assertNull(doc.get().getDocument());
        assertNotNull(doc.get().getError());
        assertThat(
                doc.get().getError().getError().getMessage(),
                CoreMatchers.containsString(exceptionMessage));
    }

    @Test
    public void createEndToEnd() {
        AppCenterFuture<Document<TestDocument>> doc = Storage.create(PARTITION, DOCUMENT_ID, new TestDocument(TEST_FIELD_VALUE), TestDocument.class);

        ArgumentCaptor<TokenExchange.TokenExchangeServiceCallback> tokenExchangeServiceCallbackArgumentCaptor =
                ArgumentCaptor.forClass(TokenExchange.TokenExchangeServiceCallback.class);
        verify(httpClient).callAsync(
                endsWith(TokenExchange.GET_TOKEN_PATH_FORMAT),
                eq(METHOD_POST),
                anyMapOf(String.class, String.class),
                any(HttpClient.CallTemplate.class),
                tokenExchangeServiceCallbackArgumentCaptor.capture());
        TokenExchange.TokenExchangeServiceCallback tokenExchangeServiceCallback = tokenExchangeServiceCallbackArgumentCaptor.getValue();
        assertNotNull(tokenExchangeServiceCallback);
        tokenExchangeServiceCallback.onCallSucceeded(tokenExchangeResponsePayload, new HashMap<String, String>());

        ArgumentCaptor<ServiceCallback> cosmosDbServiceCallbackArgumentCaptor =
                ArgumentCaptor.forClass(ServiceCallback.class);
        verify(httpClient).callAsync(
                endsWith(CosmosDb.getDocumentBaseUrl(DATABASE_NAME, COLLECTION_NAME, null)),
                eq(METHOD_POST),
                anyMapOf(String.class, String.class),
                any(HttpClient.CallTemplate.class),
                cosmosDbServiceCallbackArgumentCaptor.capture());
        ServiceCallback cosmosDbServiceCallback = cosmosDbServiceCallbackArgumentCaptor.getValue();
        assertNotNull(cosmosDbServiceCallback);
        cosmosDbServiceCallback.onCallSucceeded(cosmosDbDocumentResponsePayload, new HashMap<String, String>());

        assertNotNull(doc);

        Document<TestDocument> testCosmosDocument = doc.get();
        assertNotNull(testCosmosDocument);
        assertEquals(PARTITION, testCosmosDocument.getPartition());
        assertEquals(DOCUMENT_ID, testCosmosDocument.getId());
        assertNull(testCosmosDocument.getError());
        assertNotNull(testCosmosDocument.getEtag());
        assertNotNull(testCosmosDocument.getTimestamp());

        TestDocument testDocument = testCosmosDocument.getDocument();
        assertNotNull(testDocument);
        assertEquals(TEST_FIELD_VALUE, testDocument.test);
    }

    @Test
    public void deleteEndToEnd() {
        AppCenterFuture<Document<Void>> doc = Storage.delete(PARTITION, DOCUMENT_ID);

        ArgumentCaptor<TokenExchange.TokenExchangeServiceCallback> tokenExchangeServiceCallbackArgumentCaptor =
                ArgumentCaptor.forClass(TokenExchange.TokenExchangeServiceCallback.class);
        verify(httpClient).callAsync(
                endsWith(TokenExchange.GET_TOKEN_PATH_FORMAT),
                eq(METHOD_POST),
                anyMapOf(String.class, String.class),
                any(HttpClient.CallTemplate.class),
                tokenExchangeServiceCallbackArgumentCaptor.capture());
        TokenExchange.TokenExchangeServiceCallback tokenExchangeServiceCallback = tokenExchangeServiceCallbackArgumentCaptor.getValue();
        assertNotNull(tokenExchangeServiceCallback);
        tokenExchangeServiceCallback.onCallSucceeded(tokenExchangeResponsePayload, new HashMap<String, String>());

        ArgumentCaptor<ServiceCallback> cosmosDbServiceCallbackArgumentCaptor =
                ArgumentCaptor.forClass(ServiceCallback.class);
        verify(httpClient).callAsync(
                endsWith(CosmosDb.getDocumentBaseUrl(DATABASE_NAME, COLLECTION_NAME, DOCUMENT_ID)),
                eq(METHOD_DELETE),
                anyMapOf(String.class, String.class),
                any(HttpClient.CallTemplate.class),
                cosmosDbServiceCallbackArgumentCaptor.capture());
        ServiceCallback cosmosDbServiceCallback = cosmosDbServiceCallbackArgumentCaptor.getValue();
        assertNotNull(cosmosDbServiceCallback);
        cosmosDbServiceCallback.onCallSucceeded(null, new HashMap<String, String>());

        // TODO: assert error is null and document is null
    }

    @Test
    public void buildAppCenterGetDbTokenBodyPayload() {
        final String expectedPayload = "{\"partitions\":[\"test\"]}";
        String payload = TokenExchange.buildAppCenterGetDbTokenBodyPayload("test");
        assertEquals(expectedPayload, payload);

        // This is for code coverage.
        // These constructors must be called even though these classes are not going to be instantiated.
        TokenExchange te = new TokenExchange();
        CosmosDb cdb = new CosmosDb();
        Utils utils = new Utils();
        Utils.handleApiCallFailure(new HttpException(200, "b"));
    }

    @Test
    public void documentDeserialization() {
        Document<TestDocument> d =
                Utils.parseDocument(cosmosDbDocumentResponsePayload, TestDocument.class);
        assertEquals(DOCUMENT_ID, d.getId());
        assertEquals(PARTITION, d.getPartition());
        assertEquals(TEST_FIELD_VALUE, d.getDocument().test);
    }

    @Test
    public void documentSerialization() {
        String jsonDocument = String.format("{\"test\": \"%s\"\n" + "}", TEST_FIELD_VALUE);
        TestDocument deserializedDocument = Utils.fromJson(jsonDocument, TestDocument.class);
        assertEquals(TEST_FIELD_VALUE, deserializedDocument.test);
    }

    @Test
    public void generateHeaders() {
        Map<String, String> headers = CosmosDb.addRequiredHeaders(new HashMap<String, String>(), PARTITION, "token");
        assertEquals(5, headers.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void urlEncogingThrowsNonExistingEncoding() {
        CosmosDb.urlEncode("a string to encode", "An encoding that doesn't exist");
    }
}