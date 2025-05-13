package com.apighost.agent.model;

public class GenericFileDetailResponse {
    private final String fileName;
    private final Object file;

    public GenericFileDetailResponse(String fileName, Object file) {
        this.fileName = fileName;
        this.file = file;
    }

    public String getFileName() {
        return fileName;
    }

    public Object getFile() {
        return file;
    }
}
