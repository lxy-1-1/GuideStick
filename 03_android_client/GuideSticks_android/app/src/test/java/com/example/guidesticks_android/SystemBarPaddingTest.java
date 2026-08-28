package com.example.guidesticks_android;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SystemBarPaddingTest {
    @Test
    public void resolveAddsSystemInsetsToOriginalPadding() {
        SystemBarPadding.Values result = SystemBarPadding.resolve(
                4, 8, 12, 16, 1, 24, 3, 30);

        assertEquals(5, result.left);
        assertEquals(32, result.top);
        assertEquals(15, result.right);
        assertEquals(46, result.bottom);
    }
}
