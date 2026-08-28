package com.example.guidesticks_android.api;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ApiErrorParserTest {
    @Test
    public void parseReturnsFirstFieldError() {
        String body = "{\"username\":[\"该用户名已存在\"],\"email\":[\"邮箱已存在\"]}";

        assertEquals("该用户名已存在", ApiErrorParser.parse(body, "提交失败"));
    }

    @Test
    public void parseReturnsNonFieldError() {
        String body = "{\"non_field_errors\":[\"两次输入的密码不一致\"]}";

        assertEquals("两次输入的密码不一致", ApiErrorParser.parse(body, "提交失败"));
    }

    @Test
    public void parseUsesDetailMessage() {
        assertEquals("请求过于频繁", ApiErrorParser.parse("{\"detail\":\"请求过于频繁\"}", "提交失败"));
    }

    @Test
    public void parseFallsBackForMalformedBody() {
        assertEquals("提交失败", ApiErrorParser.parse("not-json", "提交失败"));
    }
}
