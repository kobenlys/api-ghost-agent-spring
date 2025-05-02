package com.apighost.agent.file;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Component
public class YamlHandler {

    private static final String SAVE_DIR = "build/apighost/scenario";

    public void yamlImporter(MultipartFile file) {

        if (file.isEmpty()) {
            throw new RuntimeException("Not Found YAML file");
        }

        try {
            File directory = new File(SAVE_DIR);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            String fileName = StringUtils.cleanPath(file.getOriginalFilename());

            File saveFile = new File(directory, fileName);
            file.transferTo(saveFile);

        } catch (IOException e) {
            throw new RuntimeException("Faild import yaml");
        }

    }

    public void yamlExporter() {

    }

}
