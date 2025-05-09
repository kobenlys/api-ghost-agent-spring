package com.apighost.agent.collector;

import com.apighost.model.collector.Endpoint;
import com.apighost.model.collector.FieldMeta;
import com.apighost.model.collector.Parameter;
import com.apighost.model.scenario.step.HTTPMethod;
import com.apighost.model.scenario.step.ProtocolType;
import io.github.classgraph.MethodParameterInfo;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
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
import java.util.Objects;
import java.util.Set;

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
public class ApiCollector {

    private final String basePackage;
    private final String baseUrl;
    private final List<Endpoint> endPointList = new ArrayList<>();
    private ClassLoader classLoader;

    /**
     * Constructs an ApiCollector for the specified base package and base URL.
     *
     * @param basePackage the root package to scan for controllers
     * @param baseUrl     the base URL for all collected endpoints
     */
    public ApiCollector(String basePackage, String baseUrl) {
        this.basePackage = basePackage;
        this.baseUrl = baseUrl;
    }

    /**
     * Returns the list of collected endpoints.
     *
     * @return immutable list of EndPoint objects
     */
    public List<Endpoint> getEndPointList() {
        return endPointList;
    }

    /**
     * Scans the classpath for Spring controllers and collects endpoint information.
     *
     * <p>Processes all classes annotated with {@code @RestController} in the configured
     * base package and its subpackages.</p>
     */
    public void scan() {

        ClassGraph classGraph = new ClassGraph().enableAllInfo();

        if (basePackage != null && !basePackage.isEmpty()) {
            classGraph.acceptPackages(basePackage);
        }

        try (ScanResult scanResult = classGraph.scan()) {

            this.classLoader = scanResult.getClass().getClassLoader();

            for (ClassInfo classInfo : scanResult.getClassesWithAnnotation(
                "org.springframework.web.bind.annotation.RestController")) {

                AnnotationInfoList classAnnotations = classInfo.getAnnotationInfo();

                String classPath = "";
                List<String> classProduces = Collections.emptyList();
                List<String> classConsumes = Collections.emptyList();

                AnnotationInfo classRequestMapping = classAnnotations.get(
                    "org.springframework.web.bind.annotation.RequestMapping");

                if (classRequestMapping != null) {
                    classPath = extractPath(classRequestMapping);
                    classProduces = extractStringArray(classRequestMapping, "produces");
                    classConsumes = extractStringArray(classRequestMapping, "consumes");
                }
                classPath = formatPath(classPath);

                for (MethodInfo methodInfo : classInfo.getDeclaredMethodInfo()) {
                    Endpoint endPoint = toEndpoint(methodInfo, classPath, classProduces,
                        classConsumes);
                    if (endPoint != null) {
                        endPointList.add(endPoint);
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

        if (methodAnnotations.containsName("org.springframework.web.bind.annotation.GetMapping")) {
            annotationInfo = methodAnnotations.get(
                "org.springframework.web.bind.annotation.GetMapping");
            httpMethod = HTTPMethod.GET;
            path = extractPath(annotationInfo);
            produces = resolveConsumesOrProduces(classProduces, annotationInfo, "produces");
            consumes = resolveConsumesOrProduces(classConsumes, annotationInfo, "consumes");
        } else if (methodAnnotations.containsName(
            "org.springframework.web.bind.annotation.PostMapping")) {
            annotationInfo = methodAnnotations.get(
                "org.springframework.web.bind.annotation.PostMapping");
            httpMethod = HTTPMethod.POST;
            path = extractPath(annotationInfo);
            produces = resolveConsumesOrProduces(classProduces, annotationInfo, "produces");
            consumes = resolveConsumesOrProduces(classConsumes, annotationInfo, "consumes");
        } else if (methodAnnotations.containsName(
            "org.springframework.web.bind.annotation.PutMapping")) {
            annotationInfo = methodAnnotations.get(
                "org.springframework.web.bind.annotation.PutMapping");
            httpMethod = HTTPMethod.PUT;
            path = extractPath(annotationInfo);
            produces = resolveConsumesOrProduces(classProduces, annotationInfo, "produces");
            consumes = resolveConsumesOrProduces(classConsumes, annotationInfo, "consumes");
        } else if (methodAnnotations.containsName(
            "org.springframework.web.bind.annotation.PatchMapping")) {
            annotationInfo = methodAnnotations.get(
                "org.springframework.web.bind.annotation.PatchMapping");
            httpMethod = HTTPMethod.PATCH;
            path = extractPath(annotationInfo);
            produces = resolveConsumesOrProduces(classProduces, annotationInfo, "produces");
            consumes = resolveConsumesOrProduces(classConsumes, annotationInfo, "consumes");
        } else if (methodAnnotations.containsName(
            "org.springframework.web.bind.annotation.DeleteMapping")) {
            annotationInfo = methodAnnotations.get(
                "org.springframework.web.bind.annotation.DeleteMapping");
            httpMethod = HTTPMethod.DELETE;
            path = extractPath(annotationInfo);
            produces = resolveConsumesOrProduces(classProduces, annotationInfo, "produces");
            consumes = resolveConsumesOrProduces(classConsumes, annotationInfo, "consumes");
        } else if (methodAnnotations.containsName(
            "org.springframework.web.bind.annotation.RequestMapping")) {
            annotationInfo = methodAnnotations.get(
                "org.springframework.web.bind.annotation.RequestMapping");
            path = extractPath(annotationInfo);
            httpMethod = extractHttpMethod(annotationInfo);
            produces = resolveConsumesOrProduces(classProduces, annotationInfo, "produces");
            consumes = resolveConsumesOrProduces(classConsumes, annotationInfo, "consumes");
        }

        if (httpMethod == null || path.isEmpty()) {
            return null;
        }
        String fullPath = classPath + formatPath(path);
        List<FieldMeta> responseSchema = extractResponseDtoSchema(methodInfo);
        List<FieldMeta> requestSchema = extractRequestDtoSchema(methodInfo, httpMethod);
        List<Parameter> headers = extractParameters(methodInfo,
            "org.springframework.web.bind.annotation.RequestHeader");
        List<Parameter> cookies = extractParameters(methodInfo,
            "org.springframework.web.bind.annotation.CookieValue");
        List<Parameter> requestParams = extractParameters(methodInfo,
            "org.springframework.web.bind.annotation.RequestParam");
        List<Parameter> pathVariables = extractParameters(methodInfo,
            "org.springframework.web.bind.annotation.PathVariable");

        return new Endpoint.Builder()
            .protocolType(ProtocolType.HTTP)
            .baseUrl(baseUrl)
            .methodName(methodName)
            .httpMethod(httpMethod)
            .path(fullPath)
            .produces(produces)
            .consumes(consumes)
            .requestSchema(requestSchema)
            .responseSchema(responseSchema)
            .headers(headers)
            .cookies(cookies)
            .requestParams(requestParams)
            .pathVariables(pathVariables)
            .build();
    }

    /**
     * Extracts the path value from a Spring mapping annotation.
     *
     * @param annotationInfo the annotation metadata
     * @return the first path value or empty string if not found
     */
    private String extractPath(AnnotationInfo annotationInfo) {
        if (annotationInfo == null) {
            return "";
        }

        Object value = annotationInfo.getParameterValues().getValue("value");
        if (value == null) {
            value = annotationInfo.getParameterValues().getValue("path");
        }

        if (value instanceof String) {
            return (String) value;
        } else if (value instanceof String[] && ((String[]) value).length > 0) {
            return ((String[]) value)[0];
        }

        return "";
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
     * Normalizes a path string by ensuring leading slash and no trailing slash.
     *
     * @param path the raw path string
     * @return normalized path
     */
    private String formatPath(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    /**
     * Extracts string array values from annotation attributes.
     *
     * @param annotationInfo the annotation metadata
     * @param key            the attribute name to extract
     * @return list of string values or empty list if not found
     */
    private List<String> extractStringArray(AnnotationInfo annotationInfo, String key) {
        if (annotationInfo == null) {
            return Collections.emptyList();
        }

        Object value = annotationInfo.getParameterValues().getValue(key);
        if (value instanceof String) {
            return Collections.singletonList((String) value);
        } else if (value instanceof String[]) {
            return Arrays.asList((String[]) value);
        }

        return Collections.emptyList();
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
        List<String> methodLevel = extractStringArray(methodAnnotation, key);
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
                    Class<?> paramClass = loadClass(typeName);
                    if (paramClass != null && isDTO(paramClass)) {
                        return analyzeDto(paramClass, new HashSet<>());
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
            Class<?> returnClass = loadClass(returnTypeName);
            if (returnClass == null || returnClass == void.class) {
                return Collections.emptyList();
            }

            Type genericReturnType = getGenericReturnType(methodInfo);
            Class<?> effectiveReturnClass = returnClass;

            if ("org.springframework.http.ResponseEntity".equals(returnClass.getName())
                && genericReturnType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) genericReturnType;
                Type actualType = parameterizedType.getActualTypeArguments()[0];
                String actualTypeName = getTypeName(actualType);
                effectiveReturnClass = loadClass(actualTypeName);
            } else if (Collection.class.isAssignableFrom(returnClass)
                && genericReturnType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) genericReturnType;
                Type actualType = parameterizedType.getActualTypeArguments()[0];
                String actualTypeName = getTypeName(actualType);
                effectiveReturnClass = loadClass(actualTypeName);
            }

            if (effectiveReturnClass != null && isDTO(effectiveReturnClass)) {
                return analyzeDto(effectiveReturnClass, new HashSet<>());
            }

        } catch (ClassNotFoundException e) {
            return Collections.emptyList();
        }
        return Collections.emptyList();
    }

    /**
     * Extracts parameters of a specific annotation type from method parameters.
     *
     * @param methodInfo     the method metadata
     * @param annotationName the fully-qualified annotation class name
     * @return list of Parameter objects or null if none found
     */
    private List<Parameter> extractParameters(MethodInfo methodInfo, String annotationName) {
        List<Parameter> parameters = new ArrayList<>();

        for (MethodParameterInfo paramInfo : methodInfo.getParameterInfo()) {
            AnnotationInfoList paramAnnotations = paramInfo.getAnnotationInfo();
            AnnotationInfo annotation = paramAnnotations.get(annotationName);
            if (annotation != null) {
                String paramName = extractAnnotationValue(annotation, "value", "name");
                if (paramName.isEmpty()) {
                    paramName = paramInfo.getName();
                }
                String paramType = paramInfo.getTypeDescriptor().toString();
                parameters.add(new Parameter(paramType, paramName));
            }
        }

        return parameters.isEmpty() ? Collections.emptyList() : parameters;
    }

    /**
     * Extracts a single value from annotation attributes.
     *
     * @param annotation the annotation metadata
     * @param keys       the attribute names to try (in order)
     * @return the first found value or empty string
     */
    private String extractAnnotationValue(AnnotationInfo annotation, String... keys) {
        if (annotation == null) {
            return "";
        }
        return Arrays.stream(keys)
            .map(key -> annotation.getParameterValues().getValue(key))
            .filter(Objects::nonNull)
            .map(value -> value instanceof String[] ? ((String[]) value)[0] : value.toString())
            .findFirst()
            .orElse("");
    }

    /**
     * Gets the generic return type of a method using reflection.
     *
     * @param methodInfo the method metadata
     * @return the generic return type or null if not available
     */
    private Type getGenericReturnType(MethodInfo methodInfo) {
        try {
            Class<?> clazz = loadClass(methodInfo.getClassInfo().getName());
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.getName().equals(methodInfo.getName()) &&
                    method.getParameterCount() == methodInfo.getParameterInfo().length) {
                    return method.getGenericReturnType();
                }
            }
        } catch (ClassNotFoundException e) {
            return null;
        }
        return null;
    }

    /**
     * Analyzes a DTO class and its fields recursively.
     *
     * @param dtoClass the class to analyze
     * @param visited  set of already visited classes to prevent cycles
     * @return list of FieldMeta describing the DTO structure
     */
    private List<FieldMeta> analyzeDto(Class<?> dtoClass, Set<Class<?>> visited) {

        if (dtoClass == null || visited.contains(dtoClass)) {
            return new ArrayList<>();
        }
        visited.add(dtoClass);

        List<FieldMeta> fields = new ArrayList<>();
        for (Field field : dtoClass.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(
                field.getModifiers())) {
                continue;
            }

            String fieldName = field.getName();
            String fieldType = field.getType().getSimpleName();
            List<FieldMeta> nestedFields = new ArrayList<>();

            Type genericType = field.getGenericType();
            if (genericType instanceof ParameterizedType && Collection.class.isAssignableFrom(
                field.getType())) {
                ParameterizedType parameterizedType = (ParameterizedType) genericType;
                Type collectionType = parameterizedType.getActualTypeArguments()[0];
                fieldType = getTypeName(collectionType);

                if (collectionType instanceof Class<?> && isDTO((Class<?>) collectionType)) {
                    List<FieldMeta> nestedSchema = analyzeDto((Class<?>) collectionType, visited);
                    nestedFields.addAll(nestedSchema);
                }
            } else if (isDTO(field.getType())) {
                List<FieldMeta> nestedSchema = analyzeDto(field.getType(), visited);
                nestedFields.addAll(nestedSchema);
            }

            fields.add(new FieldMeta(fieldName, fieldType, nestedFields));
        }
        return new ArrayList<>(fields);
    }

    /**
     * Gets the type name with full qualification.
     *
     * @param type the type to get name for
     * @return fully-qualified type name
     */
    private String getTypeName(Type type) {
        if (type instanceof Class<?>) {
            return ((Class<?>) type).getName();
        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type rawType = parameterizedType.getRawType();
            if (rawType instanceof Class<?>) {
                return ((Class<?>) rawType).getName();
            } else {
                return rawType.getTypeName();
            }
        }
        return type.toString();
    }

    /**
     * Loads a class by name using the scanner's classloader.
     *
     * @param className the class name to load
     * @return the loaded Class object
     * @throws ClassNotFoundException if the class cannot be loaded
     */
    private Class<?> loadClass(String className) throws ClassNotFoundException {
        if (className == null) {
            return null;
        }

        String normalizedClassName = className
            .replaceAll("\\[.*\\]", "")
            .replaceAll("<.*>", "")
            .trim();
        switch (normalizedClassName) {
            case "boolean":
                return boolean.class;
            case "byte":
                return byte.class;
            case "char":
                return char.class;
            case "double":
                return double.class;
            case "float":
                return float.class;
            case "int":
                return int.class;
            case "long":
                return long.class;
            case "short":
                return short.class;
            case "void":
                return void.class;
        }

        return classLoader != null ? classLoader.loadClass(normalizedClassName)
            : Class.forName(normalizedClassName);
    }

    /**
     * Determines if a class is a DTO (Data Transfer Object).
     *
     * @param clazz the class to check
     * @return true if the class meets DTO criteria
     */
    private boolean isDTO(Class<?> clazz) {
        if (clazz.isPrimitive() || clazz.getName().startsWith("java.")) {
            return false;
        }

        int getterCount = 0;
        for (Method method : clazz.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers())) {
                String name = method.getName();
                if ((name.startsWith("get") || (name.startsWith("is")
                    && method.getReturnType() == boolean.class))
                    && method.getParameterCount() == 0) {
                    getterCount++;
                }
            }
        }
        return getterCount > 0;
    }
}