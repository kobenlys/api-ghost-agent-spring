package com.apighost.agent.model;

public class ScenarioResultBrief {

    private String fileName;
    private boolean testSummary;
    private String timeStamp;

    public ScenarioResultBrief(String fileName, boolean testSummary, String timeStamp) {
        this.fileName = fileName;
        this.testSummary = testSummary;
        this.timeStamp = timeStamp;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public boolean getTestSummary() {
        return testSummary;
    }

    public void setTestSummary(boolean testSummary) {
        this.testSummary = testSummary;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }
}
