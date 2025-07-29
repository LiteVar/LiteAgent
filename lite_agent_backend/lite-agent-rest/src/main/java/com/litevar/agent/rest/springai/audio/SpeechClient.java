package com.litevar.agent.rest.springai.audio;

import com.litevar.agent.base.entity.LlmModel;
import com.litevar.agent.base.enums.AiProvider;
import reactor.core.publisher.Flux;

public interface SpeechClient {

    AiProvider getProvider();

    byte[] speech(LlmModel model, String text);

    Flux<byte[]> speechStream(LlmModel model, String text);
}
