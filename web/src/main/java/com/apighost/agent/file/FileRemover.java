package com.apighost.agent.file;

import com.apighost.agent.config.ApiGhostSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility class for deleting files under the {@code apighost} directory located at the root of the
 * project.
 *
 * @author kobenlys
 * @version BETA-0.0.1
 */
public class FileRemover {

    private final ApiGhostSetting apiGhostSetting;
    private static final Logger log = LoggerFactory.getLogger(FileRemover.class);

    /**
     * Constructs a new {@code FileRemover} with the specified configuration.
     *
     * @param apiGhostSetting configuration class that provides file format and path settings
     */
    public FileRemover(ApiGhostSetting apiGhostSetting) {
        this.apiGhostSetting = apiGhostSetting;
    }

    /**
     * Deletes a file located in the {@code apighost} directory.
     *
     * @param fileName the name of the file to delete (relative to {@code apighost/} directory)
     * @return {@code true} if the file was successfully deleted; {@code false} otherwise
     */
    public boolean remove(String fileName) {

        fileName = buildFilePath(fileName);

        if (fileName == null) {
            return false;
        }

        try {
            Path rootPath = Paths.get("").toAbsolutePath();
            Path targetPath = rootPath.resolve(fileName);
            File file = targetPath.toFile();

            if (file.isFile()) {
                Files.delete(targetPath);
                return true;
            } else {
                log.error("Target path is not a file: {}", fileName);
                return false;
            }
        } catch (IOException e) {
            log.error("An error occurred while deleting the file: {}", fileName, e);
            return false;
        }
    }

    /**
     * Builds a relative file path based on the configured directories and file format.
     *
     * @param fileName the original file name
     * @return full relative path to the file under {@code apighost}, or {@code null} if unsupported
     * format
     */
    private String buildFilePath(String fileName) {

        if (fileName == null || fileName.isEmpty()) {
            return null;
        }

        if (fileName.endsWith(apiGhostSetting.getFormatYaml()) || fileName.endsWith(
            apiGhostSetting.getFormatYml())) {
            return apiGhostSetting.getScenarioPath() + "/" + fileName;
        } else if (fileName.endsWith(apiGhostSetting.getFormatJson())) {
            return apiGhostSetting.getResultPath() + "/" + fileName;
        }
        return null;
    }
}
