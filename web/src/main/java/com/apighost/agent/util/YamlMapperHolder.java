package com.apighost.agent.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Provides a thread-safe singleton instance of {@link ObjectMapper} configured with
 * {@link YAMLFactory}.
 * <p>
 * This utility class ensures a single shared {@code ObjectMapper} for parsing and generating YAML
 * across the application using the Bill Pugh Singleton pattern.
 * </p>
 *
 * @author kobenlys
 * @version BETA-0.0.1
 */
public class YamlMapperHolder {

    /**
     * Holder class for lazy-loaded singleton instance of {@code ObjectMapper} configured for YAML.
     * <p>
     * The instance is created only when {@link YamlMapperHolder#getInstance()} is called.
     * </p>
     */
    private static class SingletonHolder {

        private static final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
    }

    /**
     * Returns the singleton {@code ObjectMapper} instance configured for YAML.
     *
     * @return the shared YAML-capable {@code ObjectMapper}
     */
    public static ObjectMapper getInstance() {

        return YamlMapperHolder.SingletonHolder.objectMapper;
    }
}
