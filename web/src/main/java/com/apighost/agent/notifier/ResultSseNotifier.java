package com.apighost.agent.notifier;

import com.apighost.model.scenario.ScenarioResult;
import com.apighost.model.scenario.result.ResultStep;
import java.io.IOException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * {@link ScenarioResultNotifier} implementation that sends scenario execution updates
 * to the client using Server-Sent Events (SSE).
 * <p>
 * This class is responsible for streaming step-by-step results and final scenario completion
 * results to the connected client through an {@link SseEmitter}.
 * </p>
 * <p>
 * Events emitted:
 * <ul>
 *   <li><b>stepResult</b> — triggered for each step in the scenario execution</li>
 *   <li><b>complete</b> — triggered once the scenario execution is finished</li>
 * </ul>
 * </p>
 *
 * @author kobenlys
 * @version BETA-0.0.1
 */
public class ResultSseNotifier implements ScenarioResultNotifier {

    private final SseEmitter sseEmitter;

    /**
     * Constructs a new {@code ResultSseNotifier} with the given {@link SseEmitter}.
     *
     * @param sseEmitter the emitter used to stream data to the client
     */
    public ResultSseNotifier(SseEmitter sseEmitter) {
        this.sseEmitter = sseEmitter;
    }

    /**
     * Sends a step result event to the client.
     *
     * @param step the result of the current scenario step
     * @throws IllegalStateException if the SSE connection fails
     */
    @Override
    public void notifyStep(ResultStep step) {

        try {
            sseEmitter.send(SseEmitter.event().name("stepResult").data(step));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to SSE Connection");
        }
    }

    /**
     * Sends a scenario completion event to the client.
     *
     * @param result the final result of the scenario execution
     * @throws IllegalStateException if the SSE connection fails
     */
    @Override
    public void notifyCompletion(ScenarioResult result) {

        try {
            sseEmitter.send(SseEmitter.event().name("complete").data(result));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to SSE Connection");
        }
    }
}
