package com.apighost.agent.collector.util;

import com.apighost.agent.collector.RestApiCollector;
import com.apighost.model.collector.FieldMeta;
import com.apighost.model.collector.Parameter;
import io.github.classgraph.AnnotationInfo;
import io.github.classgraph.AnnotationInfoList;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.MethodInfo;
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
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class EndpointUtil {

    /**
     * Extracts the path value from an annotation.
     *
     * @param annotationInfo the annotation metadata
     * @param pathKeys       keys to look for path (e.g., "value", "path", "destination")
     * @return the first path value or empty string if not found
     */
    public static String extractPath(AnnotationInfo annotationInfo, String... pathKeys) {
        if (annotationInfo == null) {
            return "";
        }
        for (String key : pathKeys) {
            Object value = annotationInfo.getParameterValues().getValue(key);
            if (value != null) {
                if (value instanceof String[]) {
                    String[] array = (String[]) value;
                    if (array.length > 0) {
                        return array[0];
                    }
                } else if (value instanceof String) {
                    return (String) value;
                }
            }
        }
        return "";
    }

    /**
     * Creates a ClassGraph instance configured for endpoint scanning.
     *
     * @param basePackage the root package to scan
     * @return configured ClassGraph instance
     */
    public static ClassGraph createClassGraph(String basePackage) {
        ClassGraph classGraph = new ClassGraph()
            .enableAllInfo()
            .enableMethodInfo()
            .enableAnnotationInfo();

        boolean isBootJar = EndpointUtil.class.getResource("").getProtocol().equals("jar");
        if (isBootJar) {
            classGraph.enableSystemJarsAndModules();
        }

        if (basePackage != null && !basePackage.isEmpty()) {
            classGraph.acceptPackages(basePackage);
        }
        return classGraph;
    }

    /**
     * Analyzes a DTO class and its fields recursively.
     *
     * @param dtoClass the class to analyze
     * @param visited  set of already visited classes to prevent cycles
     * @return list of FieldMeta describing the DTO structure
     */
    public static List<FieldMeta> analyzeDto(Class<?> dtoClass, Set<Class<?>> visited) {

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
     * Determines if a class is a DTO (Data Transfer Object).
     *
     * @param clazz the class to check
     * @return true if the class meets DTO criteria
     */
    public static boolean isDTO(Class<?> clazz) {
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

    /**
     * Gets the type name with full qualification.
     *
     * @param type the type to get name for
     * @return fully-qualified type name
     */
    public static String getTypeName(Type type) {
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
    public static Class<?> loadClass(String className, ClassLoader classLoader)
        throws ClassNotFoundException {
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
     * Normalizes a path string by ensuring leading slash and no trailing slash.
     *
     * @param path the raw path string
     * @return normalized path
     */
    public static String formatPath(String path) {
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
     * Extracts parameters of a specific annotation type from method parameters.
     *
     * @param methodInfo     the method metadata
     * @param annotationName the fully-qualified annotation class name
     * @return list of Parameter objects or null if none found
     */
    public static List<Parameter> extractParameters(MethodInfo methodInfo, String annotationName) {
        List<Parameter> parameters = new ArrayList<>();

        for (MethodParameterInfo paramInfo : methodInfo.getParameterInfo()) {
            AnnotationInfoList paramAnnotations = paramInfo.getAnnotationInfo();
            AnnotationInfo annotation = paramAnnotations.get(annotationName);
            if (annotation != null) {
                String paramName = extractAnnotationValue(annotation, "value", "name");
                if (paramName.isEmpty()) {
                    paramName = paramInfo.getName();
                }
                String paramType = paramInfo.getTypeDescriptor().toStringWithSimpleNames();
                parameters.add(new Parameter(paramType, paramName));
            }
        }

        return parameters.isEmpty() ? Collections.emptyList() : parameters;
    }

    /**
     * Extracts string array values from annotation attributes.
     *
     * @param annotationInfo the annotation metadata
     * @param keys           the attribute name to extract
     * @return list of string values or empty list if not found
     */
    public static List<String> extractStringArray(AnnotationInfo annotationInfo, String... keys) {
        if (annotationInfo == null) {
            return Collections.emptyList();
        }

        for (String key : keys) {
            Object value = annotationInfo.getParameterValues().getValue(key);
            if (value instanceof String) {
                return Collections.singletonList((String) value);
            } else if (value instanceof String[]) {
                String[] array = (String[]) value;
                return array.length > 0 ? Arrays.asList(array) : Collections.emptyList();
            }
        }

        return Collections.emptyList();
    }

    /**
     * Extracts a single value from annotation attributes.
     *
     * @param annotation the annotation metadata
     * @param keys       the attribute names to try (in order)
     * @return the first found value or empty string
     */
    private static String extractAnnotationValue(AnnotationInfo annotation, String... keys) {
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
    public static Type getGenericReturnType(MethodInfo methodInfo, ClassLoader classLoader) {
        try {
            Class<?> clazz = loadClass(methodInfo.getClassInfo().getName(), classLoader);
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
}
