package com.apighost.agent.collector;

import com.apighost.agent.model.FieldMeta;
import com.apighost.agent.model.Parameter;
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

import com.apighost.agent.model.EndPoint;

import io.github.classgraph.AnnotationInfo;
import io.github.classgraph.AnnotationInfoList;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.MethodInfo;
import io.github.classgraph.ScanResult;
import java.util.Objects;
import java.util.Set;

public class ApiCollector {

    private final String basePackage;
    private final String baseUrl;
    private final List<EndPoint> endPointList = new ArrayList<>();
    private ClassLoader classLoader;

    public ApiCollector(String basePackage, String baseUrl) {
        this.basePackage = basePackage;
        this.baseUrl = baseUrl;
    }

    public List<EndPoint> getEndPointList() {
        return endPointList;
    }

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
                    EndPoint endPoint = toEndpoint(methodInfo, classPath, classProduces,
                        classConsumes);
                    if (endPoint != null) {
                        endPointList.add(endPoint);
                    }
                }
            }
        }
    }

    private EndPoint toEndpoint(MethodInfo methodInfo, String classPath, List<String> classProduces,
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

        return new EndPoint.Builder()
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

    private List<String> resolveConsumesOrProduces(List<String> classLevel,
        AnnotationInfo methodAnnotation, String key) {
        List<String> methodLevel = extractStringArray(methodAnnotation, key);
        return !methodLevel.isEmpty() ? methodLevel : classLevel;
    }

    private List<FieldMeta> extractRequestDtoSchema(MethodInfo methodInfo, HTTPMethod httpMethod) {
        if (httpMethod != HTTPMethod.POST && httpMethod != HTTPMethod.PUT
            && httpMethod != HTTPMethod.PATCH) {
            return null;
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
                    return null;
                }
            }
        }
        return null;
    }

    private List<FieldMeta> extractResponseDtoSchema(MethodInfo methodInfo) {
        String returnTypeName = methodInfo.getTypeDescriptor().getResultType().toString();
        try {
            Class<?> returnClass = loadClass(returnTypeName);
            if (returnClass == null || returnClass == void.class) {
                return null;
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
            return null;
        }
        return null;
    }

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

        return parameters.isEmpty() ? null : parameters;
    }

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