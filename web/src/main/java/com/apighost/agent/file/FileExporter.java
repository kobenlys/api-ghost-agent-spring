package com.apighost.agent.file;

import com.apighost.agent.model.ScenarioExportResponse;
import com.apighost.model.scenario.Scenario;
import com.apighost.model.scenario.ScenarioResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;

/**
 * Utility class responsible for exporting {@link Scenario} and {@link ScenarioResult} objects to
 * files in a structured format (e.g., JSON or YAML).
 * <p>
 * This class uses Jackson's {@link ObjectMapper} to serialize objects to disk with indentation for
 * readability.
 * </p>
 *
 * @author kobenlys
 * @version BETA-0.0.1
 */
public class FileExporter {

    private final ObjectMapper objectMapper;

    /**
     * Constructs a {@code FileExporter} with a configured {@link ObjectMapper}. Enables
     * pretty-printing for output files.
     *
     * @param objectMapper the object mapper used to serialize objects to file
     */
    public FileExporter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Exports the provided {@link Scenario} or {@link ScenarioResult} object to a file.
     * <p>
     * The file is created at the specified path and named according to the scenario name with the
     * given file type (e.g., ".json", ".yaml").
     * </p>
     *
     * @param fileObject the object to be exported (must be {@link Scenario} or
     *                   {@link ScenarioResult})
     * @param fileType   the file extension (e.g., ".json", ".yaml")
     * @param exportPath the directory path where the file should be saved
     * @throws IllegalArgumentException if any argument is invalid or if export fails
     */
    public void exportFile(Object fileObject, String fileType, String exportPath) {

        if (fileObject == null) {
            throw new IllegalArgumentException("File must not be null.");
        }
        if (isEmptyOrNull(fileType)) {
            throw new IllegalArgumentException("FileType must not be empty or null.");
        }
        if (isEmptyOrNull(exportPath)) {
            throw new IllegalArgumentException("ExportPath must not be empty or null.");
        }

        if (fileObject instanceof Scenario scenario) {

            String scenarioName = scenario.getName();
            exportPath = exportPath + "/" + scenarioName + fileType;

            if (!exportFileExecutor(scenario, exportPath)) {
                throw new IllegalArgumentException("Failed Scenario export :" + exportPath);
            }

        } else if (fileObject instanceof ScenarioResult scenarioResult) {

            String scenarioName = scenarioResult.getName();
            exportPath = exportPath + "/" + scenarioName + fileType;

            if (!exportFileExecutor(scenarioResult, exportPath)) {
                throw new IllegalArgumentException("Failed ScenarioResult export :" + exportPath);
            }

        } else {
            throw new IllegalArgumentException("Not supported file object");
        }
    }

    /**
     * Safely exports the given object to file and wraps the result in a response object.
     * <p>
     * Returns a {@link ScenarioExportResponse} indicating success or failure without throwing an
     * exception.
     * </p>
     *
     * @param fileObject the scenario or result object to export
     * @param fileType   the desired file extension
     * @param exportPath the path where the file should be exported
     * @return a {@link ScenarioExportResponse} indicating whether the export succeeded
     */
    public ScenarioExportResponse safeExportFile(Object fileObject, String fileType,
        String exportPath) {
        try {
            exportFile(fileObject, fileType, exportPath);
            return new ScenarioExportResponse(true);
        } catch (Exception e) {
            return new ScenarioExportResponse(false);
        }
    }

    private boolean isEmptyOrNull(String targetString) {
        return targetString == null || targetString.isEmpty();
    }

    private boolean exportFileExecutor(Object object, String exportPath) {
        try {
            objectMapper.writeValue(new File(exportPath), object);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

}
