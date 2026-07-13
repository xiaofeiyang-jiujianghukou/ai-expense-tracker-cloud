package com.xiaofeiyang.expense.ai.client;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEventType;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.model.Model;
import io.agentscope.harness.agent.HarnessAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class LlmClient {

    private final Model model;

    private static final Path WORKSPACE = Path.of(System.getProperty("java.io.tmpdir"), "expense-agent-workspace");

    public String chat(String systemPrompt, String userMessage) {
        // Use streaming internally for faster first-byte and lower latency.
        // Non-streaming .call().block() waits for the full agent pipeline,
        // while streaming starts delivering deltas immediately.
        StringBuilder result = new StringBuilder();
        chatStream(systemPrompt, userMessage, result::append);
        String text = result.toString().trim();
        log.debug("LLM response: {}", text);
        if (text.isBlank()) {
            throw new RuntimeException("Empty response from LLM");
        }
        return text;
    }

    public void chatStream(String systemPrompt, String userMessage, Consumer<String> onChunk) {
        HarnessAgent agent = HarnessAgent.builder()
                .name("temp-" + UUID.randomUUID().toString().substring(0, 8))
                .sysPrompt(systemPrompt)
                .model(model)
                .workspace(WORKSPACE)
                .disableSubagents()
                .disableFilesystemTools()
                .disableShellTool()
                .build();

        // takeUntil(AGENT_END) stops the Flux immediately when the agent finishes,
        // avoiding AgentScope's internal cleanup delay (~12s on Windows).
        agent.streamEvents(new UserMessage(userMessage), RuntimeContext.empty())
                .takeUntil(event -> event.getType() == AgentEventType.AGENT_END)
                .doOnNext(event -> {
                    if (event.getType() == AgentEventType.TEXT_BLOCK_DELTA
                            && event instanceof TextBlockDeltaEvent deltaEvent) {
                        String text = deltaEvent.getDelta();
                        if (text != null && !text.isEmpty()) {
                            onChunk.accept(text);
                        }
                    }
                })
                .blockLast(Duration.ofSeconds(120));
    }
}
