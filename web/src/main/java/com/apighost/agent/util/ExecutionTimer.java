package com.apighost.agent.util;

import java.util.function.Supplier;

/**
 * Utility class for measuring the execution duration of a code block.
 * <p>
 * This class provides a method to time the execution of a {@link Supplier} operation,
 * returning both the result and timing metadata.
 * </p>
 *
 * @author kobenlys
 * @version BETA-0.0.1
 */
public class ExecutionTimer {

    /**
     * Executes the given supplier and measures the duration of its execution.
     *
     * @param supplier the operation to be executed and timed
     * @param <T>      the type of the result
     * @return a {@link DurationResult} containing the result and timing information
     */
    public static <T> DurationResult<T> execute(Supplier<T> supplier) {

        long startTime = System.currentTimeMillis();
        T result = supplier.get();
        long endTime = System.currentTimeMillis();
        return new DurationResult<>(result, startTime, endTime);
    }

    /**
     * A container for the result of an execution along with its timing data.
     *
     * @param <T> the type of the execution result
     */
    public static class DurationResult<T> {

        public final T result;
        public final long startTime;
        public final long endTime;
        public final long durationTime;

        public DurationResult(T result, long startTime, long endTime) {
            this.result = result;
            this.startTime = startTime;
            this.endTime = endTime;
            this.durationTime = endTime - startTime;
        }
    }
}
