package com.apighost.agent.file;

import java.io.File;

/**
 * Utility class for initializing required directories under the project root.
 * <p>
 * This class ensures that specific directories such as {@code /apighost} and
 * {@code /apighost/result} exist when the application runs. If the directories do not exist, they
 * will be automatically created.
 *
 * @author kobenlys
 * @version BETA-0.0.1
 * </p>
 */
public class DirectoryInitializer {

    /**
     * Initializes all required directories under the {@code /apighost} path.
     */
    public static void initializeAllRequiredDirectories() {
        createDirectoryIfMissing("apighost");
        createDirectoryIfMissing("apighost/result");
    }

    /**
     * Initializes only the scenario directory under the project root.
     */
    public static void initializeScenarioDirectory() {
        createDirectoryIfMissing("apighost");
    }

    /**
     * Initializes only the result directory under the scenario directory.
     */
    public static void initializeResultDirectory() {
        createDirectoryIfMissing("apighost/result");
    }

    /**
     * Creates a directory at the given relative path under the project root if it does not exist.
     */
    private static void createDirectoryIfMissing(String path) {
        String projectRoot = System.getProperty("user.dir");
        File directoryPath = new File(projectRoot, path);
        if (!directoryPath.exists()) {
            directoryPath.mkdirs();
        }
    }
}