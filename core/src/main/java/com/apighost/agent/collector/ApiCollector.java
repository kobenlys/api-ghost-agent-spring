package com.apighost.agent.collector;

import java.util.ArrayList;
import java.util.List;

import com.apighost.agent.model.EndPoint;

import io.github.classgraph.AnnotationInfo;
import io.github.classgraph.AnnotationInfoList;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.MethodInfo;
import io.github.classgraph.ScanResult;

public class ApiCollector {

  private final String basePackage;
  private final List<EndPoint> endPointList = new ArrayList<>();

  public ApiCollector(String basePackage) {
    this.basePackage = basePackage;
  }

  public List<EndPoint> getEndPointList() {
    return endPointList;
  }

  public void scan() {
    try (ScanResult scanResult =
        new ClassGraph().enableAllInfo().acceptPackages(basePackage).scan()) {

      for (ClassInfo classInfo :
          scanResult.getClassesWithAnnotation(
              "org.springframework.web.bind.annotation.RestController")) {
        String classPath = "";
        AnnotationInfoList classAnnotations = classInfo.getAnnotationInfo();

        if (classAnnotations.containsName(
            "org.springframework.web.bind.annotation.RequestMapping")) {
          classPath =
              extractPath(
                  classAnnotations.get("org.springframework.web.bind.annotation.RequestMapping"));
        }
        classPath = formatPath(classPath);

        for (MethodInfo methodInfo : classInfo.getDeclaredMethodInfo()) {
          EndPoint endPoint = toEndpoint(methodInfo, classPath);
          if (endPoint != null) {
            endPointList.add(endPoint);
          }
        }
      }
    }
  }

  private EndPoint toEndpoint(MethodInfo methodInfo, String classPath) {

    AnnotationInfoList methodAnnotations = methodInfo.getAnnotationInfo();

    String httpMethod = "";
    String path = "";

    if (methodAnnotations.containsName("org.springframework.web.bind.annotation.GetMapping")) {
      httpMethod = "GET";
      path =
          extractPath(methodAnnotations.get("org.springframework.web.bind.annotation.GetMapping"));
    } else if (methodAnnotations.containsName(
        "org.springframework.web.bind.annotation.PostMapping")) {
      httpMethod = "POST";
      path =
          extractPath(methodAnnotations.get("org.springframework.web.bind.annotation.PostMapping"));
    } else if (methodAnnotations.containsName(
        "org.springframework.web.bind.annotation.PutMapping")) {
      httpMethod = "PUT";
      path =
          extractPath(methodAnnotations.get("org.springframework.web.bind.annotation.PutMapping"));
    } else if (methodAnnotations.containsName(
        "org.springframework.web.bind.annotation.PatchMapping")) {
      httpMethod = "PATCH";
      path =
          extractPath(
              methodAnnotations.get("org.springframework.web.bind.annotation.PatchMapping"));
    } else if (methodAnnotations.containsName(
        "org.springframework.web.bind.annotation.DeleteMapping")) {
      httpMethod = "DELETE";
      path =
          extractPath(
              methodAnnotations.get("org.springframework.web.bind.annotation.DeleteMapping"));
    } else if (methodAnnotations.containsName(
        "org.springframework.web.bind.annotation.RequestMapping")) {
      path =
          extractPath(
              methodAnnotations.get("org.springframework.web.bind.annotation.RequestMapping"));
      httpMethod =
          extractHttpMethod(
              methodAnnotations.get("org.springframework.web.bind.annotation.RequestMapping"));
    }

    if (httpMethod.isEmpty() || path == null) {
      return null;
    }

    String fullPath = classPath + formatPath(path);

    return new EndPoint(httpMethod, fullPath,null, null, null);
  }

  private String extractPath(AnnotationInfo annotationInfo) {
    if (annotationInfo == null) return "";

    Object value = annotationInfo.getParameterValues().getValue("value");
    if (value == null) {
      value = annotationInfo.getParameterValues().getValue("path");
    }

    if (value instanceof String) {
      return (String) value;
    } else if (value instanceof String[] strings && strings.length > 0) {
      return strings[0];
    }

    return "";
  }

  private String extractHttpMethod(AnnotationInfo annotationInfo) {
    if (annotationInfo == null) {
      return "GET";
    }

    Object methodAttr = annotationInfo.getParameterValues().getValue("method");
    if (methodAttr == null) {
      return "GET";
    }

    if (methodAttr.getClass().isArray()
        && methodAttr
            .getClass()
            .getComponentType()
            .getName()
            .equals("org.springframework.web.bind.annotation.RequestMethod")) {
      Object[] methods = (Object[]) methodAttr;
      if (methods.length > 0) {
        String methodStr = methods[0].toString();
        if (methodStr.contains(".")) {
          String[] parts = methodStr.split("\\.");
          return parts[parts.length - 1];
        }
        return methodStr;
      }
    } else if (methodAttr.getClass().isArray()) {
      Object[] methods = (Object[]) methodAttr;
      if (methods.length > 0) {
        String methodStr = methods[0].toString();
        if (methodStr.contains(".")) {
          String[] parts = methodStr.split("\\.");
          return parts[parts.length - 1];
        }
        return methodStr;
      }
    } else {
      String methodStr = methodAttr.toString();
      if (methodStr.contains(".")) {
        String[] parts = methodStr.split("\\.");
        return parts[parts.length - 1];
      }
      return methodStr;
    }

    return "GET";
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
}
