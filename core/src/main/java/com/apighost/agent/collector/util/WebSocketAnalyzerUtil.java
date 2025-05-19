package com.apighost.agent.collector.util;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class WebSocketAnalyzerUtil {

    public static WebSocketConfigInfo analyze(String basePackage) {
        try (ScanResult scanResult = new ClassGraph()
            .enableAllInfo()
            .acceptPackages(basePackage)
            .scan()) {

            for (ClassInfo classInfo : scanResult.getClassesWithAnnotation(
                "org.springframework.context.annotation.Configuration")) {
                if (classInfo.hasAnnotation(
                    "org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker")) {
                    String className = classInfo.getName();
                    File javaFile = resolveJavaSourceFile(className);
                    if (javaFile != null && javaFile.exists()) {
                        return extractConfigFromJavaFile(javaFile);
                    }
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to analyze WebSocket configuration", e);
        }

        // default fallback
        return new WebSocketConfigInfo("","","");
    }

    private static WebSocketConfigInfo extractConfigFromJavaFile(File javaFile) throws IOException {
        CompilationUnit cu = new JavaParser().parse(new FileInputStream(javaFile))
            .getResult()
            .orElseThrow(() -> new RuntimeException(
                "Failed to parse Java file: " + javaFile.getAbsolutePath()));

        String appPrefix = "";
        String brokerPrefix = "";
        String stompEndpoint = "";

        for (MethodDeclaration method : cu.findAll(MethodDeclaration.class)) {
            if (method.getNameAsString().equals("configureMessageBroker")) {
                for (MethodCallExpr call : method.findAll(MethodCallExpr.class)) {
                    if (call.getNameAsString().equals("setApplicationDestinationPrefixes")) {
                        appPrefix = call.getArgument(0).asStringLiteralExpr().asString();
                    } else if (call.getNameAsString().equals("enableSimpleBroker")) {
                        brokerPrefix = call.getArgument(0).asStringLiteralExpr().asString();
                    }
                }
            } else if (method.getNameAsString().equals("registerStompEndpoints")) {
                for (MethodCallExpr call : method.findAll(MethodCallExpr.class)) {
                    if (call.getNameAsString().equals("addEndpoint")) {
                        stompEndpoint = call.getArgument(0).asStringLiteralExpr().asString();
                    }
                }
            }
        }

        return new WebSocketConfigInfo(appPrefix, brokerPrefix, stompEndpoint);
    }

    private static File resolveJavaSourceFile(String className) {
        String path = "src/main/java/" + className.replace('.', '/') + ".java";
        File file = new File(path);
        return file.exists() ? file : null;
    }

    public static class WebSocketConfigInfo {

        public final String appPrefix;
        public final String brokerPrefix;
        public final String stompEndpoint;

        public WebSocketConfigInfo(String appPrefix, String brokerPrefix, String stompEndpoint) {
            this.appPrefix = appPrefix;
            this.brokerPrefix = brokerPrefix;
            this.stompEndpoint = stompEndpoint;
        }

    }
}
