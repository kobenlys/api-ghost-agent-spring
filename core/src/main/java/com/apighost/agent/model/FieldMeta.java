package com.apighost.agent.model;

import java.util.ArrayList;
import java.util.List;

public class FieldMeta {

    private final String type;
    private final String name;
    private final List<FieldMeta> nestedFields;

    public FieldMeta(String type, String name) {
        this.type = type;
        this.name = name;
        this.nestedFields = new ArrayList<>();
    }

    public FieldMeta(String name, String type, List<FieldMeta> nestedFields) {
        this.name = name;
        this.type = type;
        this.nestedFields = nestedFields != null ? nestedFields : new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public List<FieldMeta> getNestedFields() {
        return nestedFields;
    }

}
