/*
 * Copyright © Microsoft Corporation. All rights reserved.
 */

package com.microsoft.azure.mobile.crashes.model;

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("unused")
public class TestCrashExceptionTest {

    @Test
    public void newInstance() {
        TestCrashException e = new TestCrashException();
        Assert.assertEquals(TestCrashException.CRASH_MESSAGE, e.getMessage());
    }
}
