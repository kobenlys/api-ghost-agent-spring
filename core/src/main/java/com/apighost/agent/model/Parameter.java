package com.apighost.agent.model;

public class Parameter {

    private final String type;
    private final String name;

    public Parameter(String type, String name) {
        this.type = type;
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }
}
