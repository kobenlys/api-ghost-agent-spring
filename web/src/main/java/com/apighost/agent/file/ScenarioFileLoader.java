package com.apighost.agent.file;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ScenarioFileLoader {

    public File findScenarioFile(String scenarioFileName) throws IOException {

        Path rootPath = Paths.get("").toAbsolutePath();

        Path targetFile = rootPath.resolve("apighost").resolve(scenarioFileName);
        File yamlFile = targetFile.toFile();

        if (yamlFile.isFile()) {
            return yamlFile;
        }
        throw new FileNotFoundException("File not found: " + targetFile);
    }
}
