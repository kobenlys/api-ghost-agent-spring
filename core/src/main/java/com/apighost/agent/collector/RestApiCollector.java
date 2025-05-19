package com.apighost.agent.collector;

import com.apighost.agent.collector.converter.JsonEndpointConverter;
import com.apighost.agent.collector.util.EndpointUtil;
import com.apighost.model.collector.Endpoint;
import com.apighost.model.collector.FieldMeta;
import com.apighost.model.collector.Parameter;
import com.apighost.model.scenario.step.HTTPMethod;
import com.apighost.model.scenario.step.ProtocolType;
import io.github.classgraph.MethodParameterInfo;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import io.github.classgraph.AnnotationInfo;
import io.github.classgraph.AnnotationInfoList;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.MethodInfo;
import io.github.classgraph.ScanResult;

/**
 * Collects API endpoint information from Spring controllers using classpath scanning.
 *
 * <p>This collector identifies REST endpoints annotated with Spring Web annotations
 * and extracts their metadata including paths, HTTP methods, parameters, and DTO schemas.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * ApiCollector collector = new ApiCollector("com.example", "https://api.example.com");
 * collector.scan();
 * List<EndPoint> endpoints = collector.getEndPointList();
 * </pre>
 *
 * @author oneweeek
 * @version BETA-0.0.1
 */
public class RestApiCollector implements Collector {

    private final String basePackage;
    private final String baseUrl;
    private final List<Endpoint> endpointList = new ArrayList<>();
    private final JsonEndpointConverter jsonEndpointConverter;
    private ClassLoader classLoader;

    /**
     * Constructs an ApiCollector for the specified base package and base URL.
     *
     * @param basePackage the root package to scan for controllers
     * @param baseUrl     the base URL for all collected endpoints
     */
    public RestApiCollector(String basePackage, String baseUrl) {
        this.basePackage = basePackage;
        this.baseUrl = baseUrl;
        this.jsonEndpointConverter = JsonEndpointConverter.getInstance();
    }

    /**
     * Returns the list of collected endpoints.
     *
     * @return immutable list of EndPoint objects
     */
    @Override
    public List<Endpoint> getEndpointList() {
        return endpointList;
    }

    /**
     * Scans the classpath for Spring controllers and collects endpoint information.
     *
     * <p>Processes all classes annotated with {@code @RestController} in the configured
     * base package and its subpackages.</p>
     */
    @Override
    public void scan() {

        ClassGraph classGraph = EndpointUtil.createClassGraph(basePackage);
        try (ScanResult scanResult = classGraph.scan()) {
            this.classLoader = scanResult.getClass().getClassLoader();
            for (ClassInfo classInfo : scanResult.getClassesWithAnnotation(
                "org.springframework.web.bind.annotation.RestController")) {

                String classPath = "";
                List<String> classProduces = Collections.emptyList();
                List<String> classConsumes = Collections.emptyList();

                AnnotationInfo classRequestMapping = classInfo.getAnnotationInfo(
                    "org.springframework.web.bind.annotation.RequestMapping");

                if (classRequestMapping != null) {
                    classPath = EndpointUtil.extractPath(classRequestMapping, "value", "path");
                    classProduces = EndpointUtil.extractStringArray(classRequestMapping,
                        "produces");
                    classConsumes = EndpointUtil.extractStringArray(classRequestMapping,
                        "consumes");
                }
                classPath = EndpointUtil.formatPath(classPath);

                for (MethodInfo methodInfo : classInfo.getDeclaredMethodInfo()) {
                    Endpoint endPoint = toEndpoint(methodInfo, classPath, classProduces,
                        classConsumes);
                    if (endPoint != null) {
                        endpointList.add(endPoint);
                    }
                }
            }
        }
    }

    /**
     * Converts a controller method to an EndPoint representation.
     *
     * @param methodInfo    the method metadata to convert
     * @param classPath     the base path from class-level @RequestMapping
     * @param classProduces the default produces media types from class
     * @param classConsumes the default consumes media types from class
     * @return EndPoint instance or null if not a valid endpoint
     */
    private Endpoint toEndpoint(MethodInfo methodInfo, String classPath, List<String> classProduces,
        List<String> classConsumes) {

        String methodName = methodInfo.getName();
        AnnotationInfoList methodAnnotations = methodInfo.getAnnotationInfo();

        HTTPMethod httpMethod = null;
        String path = "";
        List<String> produces = Collections.emptyList();
        List<String> consumes = Collections.emptyList();

        AnnotationInfo annotationInfo = null;
        String[] mappingAnnotations = {
            "org.springframework.web.bind.annotation.GetMapping",
            "org.springframework.web.bind.annotation.PostMapping",
            "org.springframework.web.bind.annotation.PutMapping",
            "org.springframework.web.bind.annotation.PatchMapping",
            "org.springframework.web.bind.annotation.DeleteMapping",
            "org.springframework.web.bind.annotation.RequestMapping"
        };
        for (String mapping : mappingAnnotations) {
            if (methodAnnotations.containsName(mapping)) {
                annotationInfo = methodAnnotations.get(mapping);
                if (mapping.endsWith("RequestMapping")) {
                    httpMethod = extractHttpMethod(annotationInfo);
                } else {
                    String methodStr = mapping.substring(mapping.lastIndexOf('.') + 1,
                        mapping.length() - 7);
                    httpMethod = HTTPMethod.valueOf(methodStr.toUpperCase());
                }
                path = EndpointUtil.extractPath(annotationInfo, "value", "path");
                produces = resolveConsumesOrProduces(classProduces, annotationInfo, "produces");
                consumes = resolveConsumesOrProduces(classConsumes, annotationInfo, "consumes");
                break;
            }
        }

        if (httpMethod == null) {
            return null;
        }
        String combinedPath = path.isEmpty() ? classPath : (classPath.isEmpty() ? path : classPath + path);
        String fullPath = EndpointUtil.formatPath(combinedPath);

        List<FieldMeta> responseSchema = extractResponseDtoSchema(methodInfo);
        List<FieldMeta> requestSchema = extractRequestDtoSchema(methodInfo, httpMethod);
        List<Parameter> headers = EndpointUtil.extractParameters(methodInfo,
            "org.springframework.web.bind.annotation.RequestHeader");
        List<Parameter> cookies = EndpointUtil.extractParameters(methodInfo,
            "org.springframework.web.bind.annotation.CookieValue");
        List<Parameter> requestParams = EndpointUtil.extractParameters(methodInfo,
            "org.springframework.web.bind.annotation.RequestParam");
        List<Parameter> pathVariables = EndpointUtil.extractParameters(methodInfo,
            "org.springframework.web.bind.annotation.PathVariable");

        return new Endpoint.Builder()
            .protocolType(ProtocolType.HTTP)
            .baseUrl(baseUrl)
            .methodName(methodName)
            .httpMethod(httpMethod)
            .path(fullPath)
            .produces(produces)
            .consumes(consumes)
            .requestSchema(jsonEndpointConverter.createJsonBodyFromEndpoint(requestSchema))
            .responseSchema(jsonEndpointConverter.createJsonBodyFromEndpoint(responseSchema))
            .headers(jsonEndpointConverter.convertParamsToJson(headers))
            .cookies(jsonEndpointConverter.convertParamsToJson(cookies))
            .requestParams(jsonEndpointConverter.convertParamsToJson(requestParams))
            .pathVariables(jsonEndpointConverter.convertParamsToJson(pathVariables))
            .build();
    }

    /**
     * Extracts HTTP method from @RequestMapping annotation.
     *
     * @param annotationInfo the @RequestMapping annotation metadata
     * @return the HTTP method or GET if not specified
     */
    private HTTPMethod extractHttpMethod(AnnotationInfo annotationInfo) {
        if (annotationInfo == null) {
            return HTTPMethod.GET;
        }

        Object methodAttr = annotationInfo.getParameterValues().getValue("method");
        if (methodAttr == null) {
            return HTTPMethod.GET;
        }
        if (methodAttr.getClass().isArray()) {
            Object[] methods = (Object[]) methodAttr;
            if (methods.length > 0) {
                String methodStr = methods[0].toString();
                return parseHttpMethodFromString(methodStr);
            }
        } else {
            String methodStr = methodAttr.toString();
            return parseHttpMethodFromString(methodStr);
        }

        return HTTPMethod.GET;
    }

    /**
     * Parses HTTP method from string representation.
     *
     * @param methodStr the method string (e.g., "GET", "RequestMethod.POST")
     * @return corresponding HTTPMethod enum value
     */
    private HTTPMethod parseHttpMethodFromString(String methodStr) {

        if (methodStr.contains(".")) {
            methodStr = methodStr.substring(methodStr.lastIndexOf('.') + 1);
        }

        try {
            return HTTPMethod.valueOf(methodStr);
        } catch (IllegalArgumentException e) {
            return HTTPMethod.GET;
        }
    }

    /**
     * Resolves produces/consumes media types with method-level overriding class-level values.
     *
     * @param classLevel       the class-level media types
     * @param methodAnnotation the method annotation metadata
     * @param key              the attribute name ("produces" or "consumes")
     * @return effective media types
     */
    private List<String> resolveConsumesOrProduces(List<String> classLevel,
        AnnotationInfo methodAnnotation, String key) {
        List<String> methodLevel = EndpointUtil.extractStringArray(methodAnnotation, key);
        return !methodLevel.isEmpty() ? methodLevel : classLevel;
    }

    /**
     * Extracts request DTO schema from method parameters.
     *
     * @param methodInfo the method metadata
     * @param httpMethod the HTTP method of the endpoint
     * @return list of FieldMeta describing the request body or null if not applicable
     */
    private List<FieldMeta> extractRequestDtoSchema(MethodInfo methodInfo, HTTPMethod httpMethod) {
        if (httpMethod != HTTPMethod.POST && httpMethod != HTTPMethod.PUT
            && httpMethod != HTTPMethod.PATCH) {
            return Collections.emptyList();
        }

        for (MethodParameterInfo paramInfo : methodInfo.getParameterInfo()) {
            AnnotationInfoList paramAnnotations = paramInfo.getAnnotationInfo();

            if (paramAnnotations.containsName("org.springframework.web.bind.annotation.RequestBody")
                || paramAnnotations.containsName(
                "org.springframework.web.bind.annotation.ModelAttribute")) {

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

    /**
     * Extracts response DTO schema from method return type.
     *
     * @param methodInfo the method metadata
     * @return list of FieldMeta describing the response body or null if not applicable
     */
    private List<FieldMeta> extractResponseDtoSchema(MethodInfo methodInfo) {
        String returnTypeName = methodInfo.getTypeDescriptor().getResultType().toString();
        try {
            Class<?> returnClass = EndpointUtil.loadClass(returnTypeName, classLoader);
            if (returnClass == null || returnClass == void.class) {
                return Collections.emptyList();
            }

            Type genericReturnType = EndpointUtil.getGenericReturnType(methodInfo, classLoader);
            Class<?> effectiveReturnClass = returnClass;

            if ("org.springframework.http.ResponseEntity".equals(returnClass.getName())
                && genericReturnType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) genericReturnType;
                Type actualType = parameterizedType.getActualTypeArguments()[0];
                String actualTypeName = EndpointUtil.getTypeName(actualType);
                effectiveReturnClass = EndpointUtil.loadClass(actualTypeName, classLoader);
            } else if (Collection.class.isAssignableFrom(returnClass)
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
