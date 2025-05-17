package com.apighost.agent.collector.converter;

import com.apighost.model.collector.FieldMeta;
import com.apighost.model.collector.Parameter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;

public class EndpointJsonBodyConverter {

    private static final EndpointJsonBodyConverter INSTANCE = new EndpointJsonBodyConverter();
    private final ObjectMapper objectMapper;

    private EndpointJsonBodyConverter() {
        this.objectMapper = new ObjectMapper();
    }

    public static EndpointJsonBodyConverter getInstance() {
        return INSTANCE;
    }

    public String createJsonBodyFromEndpoint(List<FieldMeta> fields) {
        String defaultJson = "{}";

        if (fields == null || fields.isEmpty()) {
            return defaultJson;
        }

        try {
            ObjectNode root = objectMapper.createObjectNode();
            buildJsonFromSchema(root, fields);
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            return defaultJson;
        }
    }

    public String convertParamsToJson(List<Parameter> params) {
        String defaultJson = "{}";

        if (params == null || params.isEmpty()) {
            return defaultJson;
        }

        try {
            ObjectNode root = objectMapper.createObjectNode();
            for (Parameter param : params) {
                if (param == null || param.getName() == null) continue;
                root.put(param.getName(), "");
            }
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            return defaultJson;
        }
    }

    private void buildJsonFromSchema(ObjectNode node, List<FieldMeta> fields) {
        if (fields == null) {
            return;
        }
        for (FieldMeta field : fields) {
            if (field == null || field.getName() == null) {
                continue;
            }

            if (hasNestedFields(field)) {
                processNestedFields(node, field);
            } else {
                addLeafField(node, field);
            }
        }
    }

    private boolean hasNestedFields(FieldMeta field) {
        return field.getNestedFields() != null && !field.getNestedFields().isEmpty();
    }

    private void processNestedFields(ObjectNode node, FieldMeta field) {
        String fieldType = field.getType();

        if (fieldType != null && fieldType.startsWith("List")) {
            processListField(node, field);
        } else {
            processObjectField(node, field);
        }
    }

    private void processListField(ObjectNode node, FieldMeta field) {
        ArrayNode arrayNode = node.putArray(field.getName());
        ObjectNode itemNode = objectMapper.createObjectNode();
        buildJsonFromSchema(itemNode, field.getNestedFields());
        arrayNode.add(itemNode);
    }

    private void processObjectField(ObjectNode node, FieldMeta field) {
        ObjectNode nestedNode = objectMapper.createObjectNode();
        buildJsonFromSchema(nestedNode, field.getNestedFields());
        node.set(field.getName(), nestedNode);
    }

    private void addLeafField(ObjectNode node, FieldMeta field) {
        String fieldType = field.getType();

        if (fieldType != null && fieldType.startsWith("List")) {
            node.putArray(field.getName()).add("");
        } else {
            node.put(field.getName(), "");
        }
    }

}