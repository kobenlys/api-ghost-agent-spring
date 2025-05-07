package com.apighost.agent.model;

/**
 * Represents a brief summary of a scenario test result.
 * <p>
 * This model includes basic metadata such as the file name, test success status, and timestamp for
 * the result.
 * </p>
 *
 * @author kobenlys
 * @version BETA-0.0.1
 */
public class ScenarioResultBrief {

    private String fileName;
    private boolean testSummary;
    private String timeStamp;

    /**
     * Constructs a new {@code ScenarioResultBrief} with the specified parameters.
     *
     * @param fileName    the name of the result file
     * @param testSummary true if the scenario test was successful; false otherwise
     * @param timeStamp   the timestamp of when the scenario was executed
     */
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
