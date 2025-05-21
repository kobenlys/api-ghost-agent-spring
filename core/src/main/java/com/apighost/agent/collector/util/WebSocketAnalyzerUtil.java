package com.apighost.agent.collector.util;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.MethodInfo;
import io.github.classgraph.MethodInfoList;
import io.github.classgraph.ScanResult;
import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.io.InputStream;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class WebSocketAnalyzerUtil {

    public static WebSocketConfigInfo analyze(ClassGraph classGraph) {
        try (ScanResult scanResult = classGraph.scan()) {
            for (ClassInfo classInfo : scanResult.getClassesWithAnnotation(
                "org.springframework.context.annotation.Configuration")) {
                if (classInfo.hasAnnotation(
                    "org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker")) {
                    return extractConfigFromClass(classInfo, scanResult.getClass().getClassLoader());
                }
            }
        }
        return new WebSocketConfigInfo("", "", "");
    }

    private static WebSocketConfigInfo extractConfigFromClass(ClassInfo classInfo, ClassLoader classLoader) {
        String appPrefix = "";
        String brokerPrefix = "";
        String stompEndpoint = "";

        try (InputStream is = classLoader.getResourceAsStream(classInfo.getName().replace('.', '/') + ".class")) {
            if (is == null) {
                throw new ClassNotFoundException("Class not found: " + classInfo.getName());
            }

            ClassReader reader = new ClassReader(is);
            ClassNode classNode = new ClassNode();
            reader.accept(classNode, 0);

            MethodInfoList methodInfos = classInfo.getMethodInfo();
            for (MethodInfo methodInfo : methodInfos) {
                String methodName = methodInfo.getName();
                if (methodName.equals("configureMessageBroker")) {
                    appPrefix = extractStringLiteral(classNode, methodName, "setApplicationDestinationPrefixes");
                    brokerPrefix = extractStringLiteral(classNode, methodName, "enableSimpleBroker");
                } else if (methodName.equals("registerStompEndpoints")) {
                    stompEndpoint = extractStringLiteral(classNode, methodName, "addEndpoint");
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Failed to analyze WebSocket configuration class: " + classInfo.getName(), e);
        }

        return new WebSocketConfigInfo(appPrefix, brokerPrefix, stompEndpoint);
    }

    private static String extractStringLiteral(ClassNode classNode, String methodName, String targetMethodCall) {
        for (MethodNode method : classNode.methods) {
            if (method.name.equals(methodName) && method.instructions != null) {
                for (AbstractInsnNode insn : method.instructions) {
                    if (insn instanceof MethodInsnNode methodInsn && methodInsn.name.equals(targetMethodCall)) {
                        AbstractInsnNode prev = insn.getPrevious();
                        while (prev != null) {
                            if (prev instanceof LdcInsnNode ldc && ldc.cst instanceof String literal) {
                                return literal;
                            }
                            prev = prev.getPrevious();
                        }
                    }
                }
            }
        }
        return "";
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
