package com.apighost.agent.sender;

import com.apighost.agent.model.EndPoint;
import com.apighost.agent.model.EndPointJson;
import com.apighost.agent.model.FieldMeta;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EndpointJsonBodyConverter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<EndPointJson> convert(List<EndPoint> endPoints) {

        if (endPoints == null) {
            return Collections.emptyList();
        }

        List<EndPointJson> result = new ArrayList<>();

        for (EndPoint endPoint : endPoints) {

            String jsonBody = createJsonBodyFromEndpoint(endPoint);
            result.add(new EndPointJson(
                endPoint.getHttpMethod(),
                endPoint.getPath(),
                jsonBody
            ));

        }
        return result;
    }

    private String createJsonBodyFromEndpoint(EndPoint endPoint) {
        String defaultJson = "{}";

        if (endPoint == null || endPoint.getRequestSchema() == null) {
            return defaultJson;
        }

        try {
            ObjectNode root = objectMapper.createObjectNode();
            List<FieldMeta> fields = endPoint.getRequestSchema();

            if (fields != null && !fields.isEmpty()) {
                buildJsonFromSchema(root, fields);
                return objectMapper.writeValueAsString(root);
            }
        } catch (JsonProcessingException e) {
            return defaultJson;
        }

        return defaultJson;
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
