package com.apighost.agent.sender;

import com.apighost.agent.model.EndPoint;
import com.apighost.agent.model.EndPointJson;
import com.apighost.agent.model.FieldMeta;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;

public class EndpointJsonBodyConverter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<EndPointJson> convert(List<EndPoint> endPoints) {
        List<EndPointJson> result = new ArrayList<>();
        for (EndPoint endPoint : endPoints) {
            String jsonBody = "{}";

            if (endPoint.getRequestSchema() != null) {
                ObjectNode root = objectMapper.createObjectNode();
                buildJsonFromSchema(root, endPoint.getRequestSchema().getFields());
                jsonBody = root.toString();
            }

            result.add(new EndPointJson(
                endPoint.getHttpMethod(),
                endPoint.getPath(),
                jsonBody
            ));
        }
        return result;
    }

    private void buildJsonFromSchema(ObjectNode node, List<FieldMeta> fields) {
        for (FieldMeta field : fields) {
            if (!field.getNestedFields().isEmpty()) {
                if (field.getType().startsWith("List")) {
                    ObjectNode itemNode = objectMapper.createObjectNode();
                    buildJsonFromSchema(itemNode, field.getNestedFields());
                    node.putArray(field.getName()).add(itemNode);
                } else {
                    ObjectNode nested = objectMapper.createObjectNode();
                    buildJsonFromSchema(nested, field.getNestedFields());
                    node.set(field.getName(), nested);
                }
            } else {
                if (field.getType().startsWith("List")) {
                    node.putArray(field.getName()).add("");
                } else {
                    node.put(field.getName(), "");
                }
            }
        }
    }

}
