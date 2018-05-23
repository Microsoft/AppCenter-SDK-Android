package com.microsoft.appcenter.ingestion.models.one;

import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.json.DefaultLogSerializer;
import com.microsoft.appcenter.ingestion.models.json.LogSerializer;
import com.microsoft.appcenter.utils.UUIDUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@SuppressWarnings("unused")
public class CommonSchemaLogSerializerTest {

    @Test
    public void serializeAndDeserialize() throws JSONException {

        /* Init serializer for Common Schema common fields. */
        LogSerializer serializer = new DefaultLogSerializer();
        serializer.addLogFactory(MockCommonSchemaLog.TYPE, new MockCommonSchemaLogFactory());

        /* Prepare a log. */
        MockCommonSchemaLog log = new MockCommonSchemaLog();
        assertJsonFailure(serializer, log);
        log.setVer("3.0");
        assertJsonFailure(serializer, log);
        log.setName("test");
        assertJsonFailure(serializer, log);
        log.setTimestamp(new Date());

        /* All required fields are now set, check we can serialize/deserialize. */
        checkSerialization(serializer, log);

        /* Keep adding top level fields and test. */
        log.setPopSample(3.1415);
        checkSerialization(serializer, log);
        log.setIKey(UUIDUtils.randomUUID().toString());
        checkSerialization(serializer, log);
        log.setFlags(5L);
        checkSerialization(serializer, log);
        log.setCV("awXwfegr");

        /* Empty extensions. */
        log.setExt(new Extensions());
        checkSerialization(serializer, log);

        /* Add extension and fields 1 by 1. Start with protocol. */
        log.getExt().setProtocol(new ProtocolExtension());
        checkSerialization(serializer, log);
        log.getExt().getProtocol().setDevMake("Samsung");
        checkSerialization(serializer, log);
        log.getExt().getProtocol().setDevModel("S5");
        checkSerialization(serializer, log);

        /* User extension. */
        log.getExt().setUser(new UserExtension());
        checkSerialization(serializer, log);
        log.getExt().getUser().setLocale("en-US");
        checkSerialization(serializer, log);

        /* OS extension. */
        log.getExt().setOs(new OsExtension());
        checkSerialization(serializer, log);
        log.getExt().getOs().setName("Android");
        checkSerialization(serializer, log);
        log.getExt().getOs().setVer("8.1.0");
        checkSerialization(serializer, log);

        /* App extension. */
        log.getExt().setApp(new AppExtension());
        checkSerialization(serializer, log);
        log.getExt().getApp().setId("com.contoso.app");
        checkSerialization(serializer, log);
        log.getExt().getApp().setVer("1.2.3");
        checkSerialization(serializer, log);
        log.getExt().getApp().setLocale("fr-FR");
        checkSerialization(serializer, log);

        /* Net extension. */
        log.getExt().setNet(new NetExtension());
        checkSerialization(serializer, log);
        log.getExt().getNet().setProvider("AT&T");
        checkSerialization(serializer, log);

        /* SDK extension. */
        log.getExt().setSdk(new SdkExtension());
        checkSerialization(serializer, log);
        log.getExt().getSdk().setLibVer("appcenter.android-1.6.0");
        checkSerialization(serializer, log);
        log.getExt().getSdk().setEpoch(UUIDUtils.randomUUID().toString());
        checkSerialization(serializer, log);
        log.getExt().getSdk().setSeq(21L);
        checkSerialization(serializer, log);
        log.getExt().getSdk().setInstallId(UUIDUtils.randomUUID());
        checkSerialization(serializer, log);

        /* Location extension. */
        log.getExt().setLocation(new LocationExtension());
        checkSerialization(serializer, log);
        log.getExt().getLocation().setTimeZone("-08:00");
        checkSerialization(serializer, log);

        /* Data. */
        log.setData(new Data());
        checkSerialization(serializer, log);
        log.getData().getProperties().put("a", "b");
        checkSerialization(serializer, log);

        /* Check baseData and baseDataType from Part B not read into Part C. */
        log.getData().getProperties().put("baseDataType", "custom");
        log.getData().getProperties().put("baseData", new JSONObject());
        Log copy = serializer.deserializeLog(serializer.serializeLog(log));
        log.getData().getProperties().remove("baseData");
        log.getData().getProperties().remove("baseDataType");
        assertEquals(log, copy);
    }

    /**
     * Verify JSON error as long as required fields (required as per SDK) are missing.
     */
    private void assertJsonFailure(LogSerializer serializer, MockCommonSchemaLog log) {
        try {
            serializer.serializeLog(log);
            fail("Was supposed to fail with JSONException");
        } catch (JSONException ignore) {

            /* Expected. */
        }
    }

    private void checkSerialization(LogSerializer serializer, MockCommonSchemaLog log) throws JSONException {
        Log copy = serializer.deserializeLog(serializer.serializeLog(log));
        assertEquals(log, copy);
    }
}