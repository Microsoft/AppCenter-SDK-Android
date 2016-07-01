package avalanche.base.ingestion.http;

import android.os.AsyncTask;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

import avalanche.base.ingestion.AvalancheIngestion;
import avalanche.base.ingestion.HttpException;
import avalanche.base.ingestion.ServiceCall;
import avalanche.base.ingestion.ServiceCallback;
import avalanche.base.ingestion.models.LogContainer;
import avalanche.base.ingestion.models.json.LogContainerSerializer;
import avalanche.base.utils.AvalancheLog;

import static java.lang.Math.max;


public class AvalancheIngestionHttp implements AvalancheIngestion {

    /**
     * API Path.
     */
    public static final String API_PATH = "/logs";

    /**
     * JSON Content-Type.
     */
    public static final String CONTENT_TYPE_JSON = "application/json";

    /**
     * Application identifier HTTP Header.
     */
    public static final String APP_ID = "App-ID";

    /**
     * Installation identifier HTTP Header.
     */
    public static final String INSTALL_ID = "Install-ID";

    /**
     * Default string builder capacity.
     */
    public static final int DEFAULT_STRING_BUILDER_CAPACITY = 16;

    /**
     * Log tag for POST payload.
     */
    private static final String LOG_TAG = "avalanche-http";

    /**
     * Content type header.
     */
    private static final String CONTENT_TYPE = "Content-Type";

    /**
     * Character encoding.
     */
    private static final String CHARSET_NAME = "UTF-8";

    /**
     * Read buffer size.
     */
    private static final int READ_BUFFER_SIZE = 1024;

    /**
     * HTTP connection timeout.
     */
    private static final int CONNECT_TIMEOUT = 60000;

    /**
     * HTTP read timeout.
     */
    private static final int READ_TIMEOUT = 20000;

    /**
     * API base URL (scheme + authority).
     */
    private String mBaseUrl;

    /**
     * Url connection factory.
     */
    private UrlConnectionFactory mUrlConnectionFactory;

    /**
     * Log container serializer.
     */
    private LogContainerSerializer mLogContainerSerializer;

    /**
     * Set the base url.
     *
     * @param baseUrl the base url.
     */
    public void setBaseUrl(String baseUrl) {
        mBaseUrl = baseUrl;
    }

    /**
     * Set URL connection factory.
     *
     * @param urlConnectionFactory URL connection factory.
     */
    public void setUrlConnectionFactory(UrlConnectionFactory urlConnectionFactory) {
        mUrlConnectionFactory = urlConnectionFactory;
    }

    /**
     * Set the log container serializer.
     * It is expected that the caller of this method is responsible for adding the factories to the serializer.
     *
     * @param logContainerSerializer log container serializer
     */
    public void setLogContainerSerializer(LogContainerSerializer logContainerSerializer) {
        mLogContainerSerializer = logContainerSerializer;
    }

    @Override
    public ServiceCall sendAsync(final String appId, final UUID installId, final LogContainer logContainer, final ServiceCallback serviceCallback) throws IllegalArgumentException {
        if (mBaseUrl == null)
            throw new IllegalStateException("baseUrl not configured");
        if (mUrlConnectionFactory == null)
            throw new IllegalStateException("urlConnectionFactory not configured");
        if (mLogContainerSerializer == null)
            throw new IllegalStateException("logContainerSerializer not configured");
        final AsyncTask<Void, Void, Exception> call = new AsyncTask<Void, Void, Exception>() {

            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    doCall(appId, installId, logContainer);
                } catch (Exception e) {
                    return e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Exception e) {
                if (e == null)
                    serviceCallback.success();
                else
                    serviceCallback.failure(e);
            }
        };
        call.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        return new ServiceCall() {

            @Override
            public void cancel() {
                if (!call.isCancelled())
                    call.cancel(true);
            }
        };
    }

    /**
     * Do the HTTP call now.
     *
     * @param appId        application identifier.
     * @param installId    install identifier.
     * @param logContainer payload.
     * @throws Exception if an error occurs.
     */
    private void doCall(String appId, UUID installId, LogContainer logContainer) throws Exception {

        /* HTTP session. */
        HttpURLConnection urlConnection = null;
        try {

            /* Init connection. */
            URL url = new URL(mBaseUrl + API_PATH);
            AvalancheLog.debug(LOG_TAG, "Calling " + url + " ...");
            urlConnection = mUrlConnectionFactory.openConnection(url);
            urlConnection.setConnectTimeout(CONNECT_TIMEOUT);
            urlConnection.setReadTimeout(READ_TIMEOUT);
            urlConnection.setRequestProperty(CONTENT_TYPE, CONTENT_TYPE_JSON);

            /* Set headers. */
            urlConnection.setRequestProperty(APP_ID, appId);
            urlConnection.setRequestProperty(INSTALL_ID, installId.toString());

            /* Serialize payload. */
            urlConnection.setDoOutput(true);
            String payload = mLogContainerSerializer.serialize(logContainer);
            AvalancheLog.debug(LOG_TAG, payload);

            /* Send payload through the wire. */
            byte[] binaryPayload = payload.getBytes(CHARSET_NAME);
            urlConnection.setFixedLengthStreamingMode(binaryPayload.length);
            OutputStream out = urlConnection.getOutputStream();
            out.write(binaryPayload);
            out.close();

            /* Read response. */
            int status = urlConnection.getResponseCode();
            String response = dump(urlConnection);
            AvalancheLog.debug(LOG_TAG, "HTTP response status=" + status + " payload=" + response);

            /* Generate exception on failure. */
            if (status != 200)
                throw new HttpException(status);
        } finally {

            /* Release connection. */
            if (urlConnection != null)
                urlConnection.disconnect();
        }
    }

    /**
     * Dump stream to string.
     *
     * @param urlConnection URL connection.
     * @return dumped string.
     * @throws IOException if an error occurred.
     */
    private String dump(HttpURLConnection urlConnection) throws IOException {

        /*
         * Though content length header value is less than actual payload length (gzip), we want to init
         * buffer with a reasonable start size to optimize (default is 16 and is way too low for this
         * use case).
         */
        StringBuilder builder = new StringBuilder(max(urlConnection.getContentLength(), DEFAULT_STRING_BUILDER_CAPACITY));
        InputStream stream;
        if (urlConnection.getResponseCode() < 400)
            stream = urlConnection.getInputStream();
        else
            stream = urlConnection.getErrorStream();
        InputStreamReader in = new InputStreamReader(stream, CHARSET_NAME);
        char[] buffer = new char[READ_BUFFER_SIZE];
        int len;
        while ((len = in.read(buffer)) > 0)
            builder.append(buffer, 0, len);
        return builder.toString();
    }
}
