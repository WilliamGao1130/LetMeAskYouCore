package org.bluepowerrobotics.letmeaskyou.cli;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AskCLITest {

    @Test
    void fetchUrlSuccessIsSummarized() {
        StringBuilder html = new StringBuilder();
        for (int i = 0; i < 5000; i++) {
            html.append('x');
        }
        String result = "{\"ok\":true,\"html\":\"" + html + "\"}";

        String summary = AskCLI.summarizeToolResult(result);

        assertTrue(summary.startsWith("成功"), summary);
        assertTrue(summary.contains("5000"), summary);
        assertTrue(summary.length() < 100);
    }

    @Test
    void shortOkJsonIsShownAsIs() {
        String result = "{\"ok\":true,\"timezone\":\"Asia/Shanghai\",\"time\":\"15:20:31\"}";

        String summary = AskCLI.summarizeToolResult(result);

        assertEquals(result, summary);
    }

    @Test
    void fetchUrlFailureShowsError() {
        String result = "{\"ok\":false,\"error\":\"请求失败: timeout\"}";

        String summary = AskCLI.summarizeToolResult(result);

        assertEquals("失败: 请求失败: timeout", summary);
    }

    @Test
    void longPlainTextIsTruncated() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 300; i++) {
            sb.append('a');
        }

        String summary = AskCLI.summarizeToolResult(sb.toString());

        assertTrue(summary.length() < 300);
        assertTrue(summary.contains("共 300 字符"));
    }
}
