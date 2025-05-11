package com.apighost.agent.util;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Provides a thread-safe singleton instance of {@link ObjectMapper}.
 * <p>
 * This utility class ensures a single shared {@code ObjectMapper} instance
 * across the application using the Bill Pugh Singleton pattern.
 * </p>
 * @author kobenlys
 * @version BETA-0.0.1
 */
public class ObjectMapperHolder {

    /**
     * Holder class for lazy-loaded singleton instance of {@code ObjectMapper}.
     * <p>
     * The instance is created only when {@link ObjectMapperHolder#getInstance()} is called.
     * </p>
     */
    private static class SingletonHolder {

        private static final ObjectMapper objectMapper = new ObjectMapper();
    }

    /**
     * Returns the singleton {@code ObjectMapper} instance.
     *
     * @return the shared {@code ObjectMapper}
     */
    public static ObjectMapper getInstance() {

        return SingletonHolder.objectMapper;
    }
}
