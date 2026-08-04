package org.bluepowerrobotics.letmeaskyou.core.toolcall;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlPage;

import java.io.IOException;
import java.util.Map;

import static org.bluepowerrobotics.letmeaskyou.core.web.WebFetcher.createWebClient;

public class FetchUrl implements Tool{
    private static final ObjectMapper JSON = new ObjectMapper();

    @Override
    public String getName() {
        return "FetchUrl";
    }

    @Override
    public String getDescription() {
        return "获取指定 URL 经过 JavaScript 渲染后的完整 HTML 内容。"
                + "参数：url (string, 必需)；waitMs (int,JS 异步等待毫秒数，0表示禁用 JS)。";
    }

    @Override
    public String getParametersJson() {
        return "{"
                + "\"type\":\"object\","
                + "\"properties\":{"
                + "\"url\":{\"type\":\"string\",\"description\":\"要抓取的页面 URL，必须完整包含协议\"},"
                + "\"waitMs\":{\"type\":\"integer\",\"description\":\"JS 异步等待毫秒数，0 表示禁用 JS（默认 0）\"}"
                + "},"
                + "\"required\":[\"url\"]"
                + "}";
    }

    @Override
    public String execute(Map<String, Object> args) {
        Object urlObj = args.get("url");
        String url = urlObj == null ? null : String.valueOf(urlObj);
        if (url == null || url.trim().isEmpty()) {
            return errorJson("缺少必需参数: url");
        }

        // 解析 waitMs，默认为 0
        int waitMs = 0;
        Object waitObj = args.get("waitMs");
        if (waitObj != null) {
            if (waitObj instanceof Number) {
                waitMs = ((Number) waitObj).intValue();
            } else {
                try {
                    waitMs = Integer.parseInt(String.valueOf(waitObj).trim());
                } catch (NumberFormatException e) {
                    return errorJson("waitMs 必须是整数: " + waitObj);
                }
            }
        }

        // 根据 waitMs 决定是否启用 JS
        boolean enableJs = waitMs > 0;
        String html;
        try (WebClient webClient = createWebClient(enableJs)) {
            // 请求页面（同步 JS 会自动执行）
            HtmlPage page = webClient.getPage(url);

            // 如果启用了 JS，额外等待异步任务
            if (enableJs) {
                webClient.waitForBackgroundJavaScript(waitMs);
            }

            // 获取最终渲染后的 HTML
            html = page.asXml();
        } catch (IOException e) {
            return errorJson("请求失败: " + e.getMessage());
        }

        ObjectNode node = JSON.createObjectNode();
        node.put("ok", true);
        node.put("html", html);
        return node.toString();
    }

    private String errorJson(String errorMsg) {
        ObjectNode node = JSON.createObjectNode();
        node.put("ok", false);
        node.put("error", errorMsg);
        return node.toString();
    }
}
