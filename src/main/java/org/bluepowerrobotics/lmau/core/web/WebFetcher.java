package org.bluepowerrobotics.lmau.core.web;

import org.htmlunit.WebClient;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 网页内容获取器，支持 JavaScript 渲染。
 * <p>
 * 基于 HtmlUnit 实现，可执行页面中的 JS、AJAX 请求，获取最终的 DOM 树对应的 HTML。
 * </p>
 */
public class WebFetcher {

    /**
     * 根据是否启用 JS 创建并配置 WebClient。
     *
     * @param javaScriptEnabled true 启用 JS，false 完全禁用
     * @return 配置好的 WebClient（已关闭日志等）
     */
    public static WebClient createWebClient(boolean javaScriptEnabled) {
        WebClient webClient = new WebClient();
        // 设置 JS 开关
        webClient.getOptions().setJavaScriptEnabled(javaScriptEnabled);
        // 为提高速度，关闭 CSS（布局不影响 DOM 结构）
        webClient.getOptions().setCssEnabled(false);
        // 超时设置
        webClient.getOptions().setTimeout(30000);          // 页面加载超时 30 秒
        webClient.setJavaScriptTimeout(15000);             // JS 执行超时 15 秒（仅在启用 JS 时生效）
        // 忽略图片等资源
        webClient.getOptions().setDownloadImages(false);
        webClient.getOptions().setGeolocationEnabled(false);
        webClient.getOptions().setAppletEnabled(false);
        // SSL 容错（可选）
        webClient.getOptions().setUseInsecureSSL(true);
        // 关闭 HtmlUnit 的冗长日志
        Logger.getLogger("org.htmlunit").setLevel(Level.OFF);
        // 模拟真实浏览器 User-Agent（避免某些网站拦截）
        webClient.addRequestHeader("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) \nAppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6.1 \nChrome/114.0.0.0 \nSafari/537.36");
        return webClient;
    }
}