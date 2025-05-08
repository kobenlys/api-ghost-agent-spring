package com.apighost.agent.file;

import com.apighost.agent.model.ScenarioExportResponse;
import com.apighost.model.scenario.Scenario;
import com.apighost.model.scenario.ScenarioResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;

public class FileExporter {

    private final ObjectMapper objectMapper;

    public FileExporter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

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

    public ScenarioExportResponse safeExportFile(Object fileObject, String fileType, String exportPath) {
        try {
            exportFile(fileObject, fileType, exportPath);
            return new ScenarioExportResponse(true);
        }  catch (Exception e) {
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
