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

/**
 * Converts {@link EndPoint} metadata into {@link EndPointJson} with mock JSON request bodies.
 *
 * <p>Uses {@link FieldMeta} structure to recursively generate default JSON bodies for testing or preview.</p>
 *
 * @author oneweeek
 * @version BETA-0.0.1
 */
public class EndpointJsonBodyConverter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Converts a list of {@link EndPoint} objects to {@link EndPointJson} format,
     * embedding mock request JSON bodies generated from schema.
     *
     * @param endPoints list of API endpoint metadata
     * @return list of endpoints with mock JSON bodies
     */
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

    /**
     * Creates a mock JSON body from an endpoint's request schema.
     *
     * @param endPoint the endpoint to process
     * @return generated JSON string or "{}" if invalid
     */
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

    /**
     * Recursively constructs JSON nodes from a field schema.
     *
     * @param node   parent JSON node
     * @param fields list of field metadata
     */
    private void buildJsonFromSchema(ObjectNode node, List<FieldMeta> fields) {
        if (fields == null) return;

        for (FieldMeta field : fields) {
            if (field == null || field.getName() == null) continue;

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
