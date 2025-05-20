package com.apighost.agent.collector;

import com.apighost.agent.collector.converter.JsonEndpointConverter;
import com.apighost.agent.collector.util.EndpointUtil;
import com.apighost.agent.collector.util.WebSocketAnalyzerUtil;
import com.apighost.model.collector.Endpoint;
import com.apighost.model.collector.FieldMeta;
import com.apighost.model.collector.Parameter;
import com.apighost.model.scenario.step.HTTPMethod;
import com.apighost.model.scenario.step.ProtocolType;
import io.github.classgraph.AnnotationInfo;
import io.github.classgraph.AnnotationInfoList;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.MethodInfo;
import io.github.classgraph.MethodParameterInfo;
import io.github.classgraph.ScanResult;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class WebSocketCollector implements Collector {

    private final String basePackage;
    private final String baseUrl;
    private final List<Endpoint> endpointList = new ArrayList<>();
    private ClassLoader classLoader;
    private String appDestinationPrefix;
    private String brokerDestinationPrefix;
    private String stompEndpoint;
    private final JsonEndpointConverter jsonEndpointConverter;

    public WebSocketCollector(String basePackage, String baseUrl) {
        this.basePackage = basePackage;
        this.baseUrl = convertToWebSocketUrl(baseUrl);
        this.jsonEndpointConverter = JsonEndpointConverter.getInstance();
    }

    @Override
    public List<Endpoint> getEndpointList() {
        return endpointList;
    }

    @Override
    public void scan() {
        ClassGraph classGraph = EndpointUtil.createClassGraph(basePackage);

        try (ScanResult scanResult = classGraph.scan()) {
            this.classLoader = scanResult.getClass().getClassLoader();

            WebSocketAnalyzerUtil.WebSocketConfigInfo configInfo =
                WebSocketAnalyzerUtil.analyze(basePackage);

            appDestinationPrefix = configInfo.appPrefix;
            brokerDestinationPrefix = configInfo.brokerPrefix;
            stompEndpoint = configInfo.stompEndpoint;

            if (!appDestinationPrefix.isEmpty() &&
                !brokerDestinationPrefix.isEmpty() &&
                !stompEndpoint.isEmpty()) {
                addConnectionEndpoints();
            }

            for (ClassInfo classInfo : scanResult.getClassesWithAnnotation(
                "org.springframework.stereotype.Controller")) {
                String classDestinationPrefix = EndpointUtil.extractPath(
                    classInfo.getAnnotationInfo(
                        "org.springframework.messaging.handler.annotation.MessageMapping"),
                    "value", "destination");
                for (MethodInfo methodInfo : classInfo.getDeclaredMethodInfo()) {
                    endpointList.addAll(toEndpoints(methodInfo, classDestinationPrefix));
                }
            }
        }
    }

    private String convertToWebSocketUrl(String baseUrl) {
        if (baseUrl.startsWith("https")) {
            return baseUrl.replaceFirst("https", "wss");
        } else if (baseUrl.startsWith("http")) {
            return baseUrl.replaceFirst("http", "ws");
        }
        return baseUrl;
    }

    private void addConnectionEndpoints() {
        String path = EndpointUtil.formatPath(stompEndpoint);

        endpointList.add(new Endpoint.Builder()
            .protocolType(ProtocolType.WEBSOCKET)
            .baseUrl(baseUrl)
            .methodName("connect")
            .httpMethod(HTTPMethod.CONNECT)
            .path(path)
            .produces(Collections.singletonList("application/json"))
            .consumes(Collections.singletonList("application/json"))
            .requestSchema(
                jsonEndpointConverter.createJsonBodyFromEndpoint(Collections.emptyList()))
            .responseSchema(
                jsonEndpointConverter.createJsonBodyFromEndpoint(Collections.emptyList()))
            .headers(jsonEndpointConverter.convertParamsToJson(Collections.emptyList()))
            .cookies(jsonEndpointConverter.convertParamsToJson(Collections.emptyList()))
            .requestParams(jsonEndpointConverter.convertParamsToJson(Collections.emptyList()))
            .pathVariables(jsonEndpointConverter.convertParamsToJson(Collections.emptyList()))
            .build());

        // DISCONNECT endpoint
        endpointList.add(new Endpoint.Builder()
            .protocolType(ProtocolType.WEBSOCKET)
            .baseUrl(baseUrl)
            .methodName("disconnect")
            .httpMethod(HTTPMethod.DISCONNECT)
            .path(path)
            .produces(Collections.singletonList("application/json"))
            .consumes(Collections.singletonList("application/json"))
            .requestSchema(
                jsonEndpointConverter.createJsonBodyFromEndpoint(Collections.emptyList()))
            .responseSchema(
                jsonEndpointConverter.createJsonBodyFromEndpoint(Collections.emptyList()))
            .headers(jsonEndpointConverter.convertParamsToJson(Collections.emptyList()))
            .cookies(jsonEndpointConverter.convertParamsToJson(Collections.emptyList()))
            .requestParams(jsonEndpointConverter.convertParamsToJson(Collections.emptyList()))
            .pathVariables(jsonEndpointConverter.convertParamsToJson(Collections.emptyList()))
            .build());
    }

    private List<Endpoint> toEndpoints(MethodInfo methodInfo, String classDestinationPrefix) {
        List<Endpoint> endpoints = new ArrayList<>();
        AnnotationInfoList methodAnnotations = methodInfo.getAnnotationInfo();
        String methodName = methodInfo.getName();

        AnnotationInfo messageMapping = methodAnnotations.get(
            "org.springframework.messaging.handler.annotation.MessageMapping");
        if (messageMapping != null) {
            String destination = EndpointUtil.extractPath(messageMapping, "value", "destination");
            if (!destination.isEmpty()) {
                Endpoint sendEndpoint = buildEndpoint(methodInfo, classDestinationPrefix,
                    destination,
                    HTTPMethod.SEND, methodName);
                endpoints.add(sendEndpoint);
            }
            AnnotationInfo sendTo = methodAnnotations.get(
                "org.springframework.messaging.handler.annotation.SendTo");
            if (sendTo != null) {
                List<String> sendPaths = EndpointUtil.extractStringArray(sendTo, "value",
                    "destination");
                for (String sendPath : sendPaths) {
                    Endpoint subscribeEndpoint = buildEndpoint(methodInfo, classDestinationPrefix,
                        sendPath, HTTPMethod.SUBSCRIBE, methodName);
                    endpoints.add(subscribeEndpoint);
                    Endpoint unsubscribeEndpoint = buildEndpoint(methodInfo, classDestinationPrefix,
                        sendPath,
                        HTTPMethod.UNSUBSCRIBE, methodName);
                    endpoints.add(unsubscribeEndpoint);
                }
            }

            AnnotationInfo sendToUser = methodAnnotations.get(
                "org.springframework.messaging.simp.annotation.SendToUser");
            if (sendToUser != null) {
                List<String> sendPaths = EndpointUtil.extractStringArray(sendToUser, "value",
                    "destination");
                for (String sendPath : sendPaths) {
                    Endpoint subscribeEndpoint = buildEndpoint(methodInfo, classDestinationPrefix,
                        sendPath,
                        HTTPMethod.SUBSCRIBE, methodName);
                    endpoints.add(subscribeEndpoint);
                    Endpoint unsubscribeEndpoint = buildEndpoint(methodInfo, classDestinationPrefix,
                        sendPath,
                        HTTPMethod.UNSUBSCRIBE, methodName);
                    endpoints.add(unsubscribeEndpoint);
                }
            }
        }

        return endpoints;
    }

    private Endpoint buildEndpoint(MethodInfo methodInfo, String classDestinationPrefix,
        String destination,
        HTTPMethod httpMethod, String methodName) {
        String fullDestination = EndpointUtil.formatPath(
            appDestinationPrefix + classDestinationPrefix + EndpointUtil.formatPath(destination));
        List<FieldMeta> requestSchemaList = extractRequestDtoSchema(methodInfo);
        List<FieldMeta> responseSchemaList = extractResponseDtoSchema(methodInfo);
        List<Parameter> headersList = EndpointUtil.extractParameters(methodInfo,
            "org.springframework.messaging.handler.annotation.Header");

        String responseDestination = "";

        AnnotationInfo sendTo = methodInfo.getAnnotationInfo(
            "org.springframework.messaging.handler.annotation.SendTo");
        if (sendTo != null) {
            responseDestination = EndpointUtil.extractPath(sendTo, "value");
        } else {
            AnnotationInfo sendToUser = methodInfo.getAnnotationInfo(
                "org.springframework.messaging.handler.annotation.SendToUser");
            if (sendToUser != null) {
                responseDestination = EndpointUtil.extractPath(sendToUser, "destinations");
                if (responseDestination.isEmpty()) {
                    responseDestination = "/user/{userId}" + destination;
                }
            } else {
                responseDestination = brokerDestinationPrefix + destination;
            }
        }

        return new Endpoint.Builder()
            .protocolType(ProtocolType.WEBSOCKET)
            .baseUrl(baseUrl)
            .methodName(methodName)
            .httpMethod(httpMethod)
            .path(fullDestination)
            .produces(Collections.singletonList("application/json"))
            .consumes(Collections.singletonList("application/json"))
            .requestSchema(jsonEndpointConverter.createJsonBodyFromEndpoint(requestSchemaList))
            .responseSchema(jsonEndpointConverter.createJsonBodyFromEndpoint(responseSchemaList))
            .headers(jsonEndpointConverter.convertParamsToJson(headersList))
            .cookies("")
            .requestParams("")
            .pathVariables("")
            .build();
    }

    private List<FieldMeta> extractRequestDtoSchema(MethodInfo methodInfo) {
        for (MethodParameterInfo paramInfo : methodInfo.getParameterInfo()) {
            if (paramInfo.getAnnotationInfo(
                "org.springframework.messaging.handler.annotation.Payload") != null) {
                String typeName = paramInfo.getTypeDescriptor().toString();
                try {
                    Class<?> paramClass = EndpointUtil.loadClass(typeName, classLoader);
                    if (paramClass != null && EndpointUtil.isDTO(paramClass)) {
                        return EndpointUtil.analyzeDto(paramClass, new HashSet<>());
                    }
                } catch (ClassNotFoundException e) {
                    return Collections.emptyList();
                }
            }
        }
        return Collections.emptyList();
    }

    private List<FieldMeta> extractResponseDtoSchema(MethodInfo methodInfo) {
        String returnTypeName = methodInfo.getTypeDescriptor().getResultType().toString();
        try {
            Class<?> returnClass = EndpointUtil.loadClass(returnTypeName, classLoader);
            if (returnClass == null || returnClass == void.class) {
                return Collections.emptyList();
            }
            Type genericReturnType = EndpointUtil.getGenericReturnType(methodInfo, classLoader);
            Class<?> effectiveReturnClass = returnClass;
            if (Collection.class.isAssignableFrom(returnClass)
                && genericReturnType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) genericReturnType;
                Type actualType = parameterizedType.getActualTypeArguments()[0];
                String actualTypeName = EndpointUtil.getTypeName(actualType);
                effectiveReturnClass = EndpointUtil.loadClass(actualTypeName, classLoader);
            }
            if (effectiveReturnClass != null && EndpointUtil.isDTO(effectiveReturnClass)) {
                return EndpointUtil.analyzeDto(effectiveReturnClass, new HashSet<>());
            }
        } catch (ClassNotFoundException e) {
            return Collections.emptyList();
        }
        return Collections.emptyList();
    }

}