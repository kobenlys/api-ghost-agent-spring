package com.apighost.agent.model;

import java.util.List;

public class DtoSchema {

    private List<FieldMeta> fields;

    public DtoSchema(List<FieldMeta> fields) {
        this.fields = fields;
    }

    public List<FieldMeta> getFields() {
        return fields;
    }

}
