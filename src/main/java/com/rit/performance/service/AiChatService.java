package com.rit.performance.service;

import com.rit.performance.dto.AiChatRequest;
import com.rit.performance.dto.AiChatResponse;

public interface AiChatService {
    AiChatResponse chat(AiChatRequest request);
}
