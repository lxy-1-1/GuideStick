package com.example.guidesticks_android;

import static org.junit.Assert.assertTrue;

import com.example.guidesticks_android.model.SensorContextRequest;
import com.google.gson.Gson;

import org.junit.Test;

public class SensorContextRequestTest {
    @Test
    public void requestSerializesLocationForFusionEndpoint() {
        SensorContextRequest request = new SensorContextRequest(
                34.811927,
                113.499048,
                12.5f,
                "郑州市中原区",
                1784800000000L
        );

        String json = new Gson().toJson(request);

        assertTrue(json.contains("\"latitude\":34.811927"));
        assertTrue(json.contains("\"longitude\":113.499048"));
        assertTrue(json.contains("\"accuracy\":12.5"));
        assertTrue(json.contains("\"address\":\"郑州市中原区\""));
        assertTrue(json.contains("\"captured_at_ms\":1784800000000"));
    }
}
