package org.bluepowerrobotics.lmau.core.adapter;

import org.bluepowerrobotics.lmau.converter.core.ChatModel;
import org.bluepowerrobotics.lmau.core.config.ModelConfig;

/**
 * 提供商适配器：把一个 {@link ModelConfig} 变成可调用的 {@link ChatModel}。
 * 所有内置提供商都通过 AIAPIConverter 实现，自定义提供商可自行注册。
 */
@FunctionalInterface
public interface ApiAdapter {
    ChatModel createChatModel(ModelConfig modelConfig);
}
