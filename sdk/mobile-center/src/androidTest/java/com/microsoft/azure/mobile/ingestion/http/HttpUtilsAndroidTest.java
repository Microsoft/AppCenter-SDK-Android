/*
 * Copyright © Microsoft Corporation. All rights reserved.
 */

package com.microsoft.azure.mobile.ingestion.http;

import org.junit.Test;

import java.io.EOFException;
import java.io.InterruptedIOException;
import java.net.MalformedURLException;
import java.net.PortUnreachableException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import static com.microsoft.azure.mobile.ingestion.http.HttpUtils.isRecoverableError;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@SuppressWarnings("unused")
public class HttpUtilsAndroidTest {

    @Test
    public void utilsCoverage() {
        new HttpUtils();
    }

    @Test
    public void isRecoverableErrorTest() {
        assertTrue(isRecoverableError(new EOFException()));
        assertTrue(isRecoverableError(new InterruptedIOException()));
        assertTrue(isRecoverableError(new SocketTimeoutException()));
        assertTrue(isRecoverableError(new SocketException()));
        assertTrue(isRecoverableError(new PortUnreachableException()));
        assertTrue(isRecoverableError(new UnknownHostException()));
        assertFalse(isRecoverableError(new MalformedURLException()));
        for (int i = 0; i <= 4; i++)
            assertTrue(isRecoverableError(new HttpException(500 + i)));
        for (int i = 2; i <= 6; i++)
            assertFalse(isRecoverableError(new HttpException(400 + i)));
        assertTrue(isRecoverableError(new HttpException(408)));
        assertFalse(isRecoverableError(new HttpException(413)));
        assertTrue(isRecoverableError(new HttpException(429)));
        assertTrue(isRecoverableError(new HttpException(401)));
    }
}
