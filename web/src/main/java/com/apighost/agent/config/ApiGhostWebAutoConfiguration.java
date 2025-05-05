package com.apighost.agent.config;

import com.apighost.agent.collector.ApiCollector;
import com.apighost.agent.controller.EndPointProvider;
import com.apighost.agent.controller.EngineController;
import com.apighost.agent.controller.ScenarioGUIController;
import com.apighost.agent.executor.ScenarioTestExecutor;
import com.apighost.agent.loader.ScenarioFileLoader;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Configuration
public class ApiGhostWebAutoConfiguration {

    @Bean
    public ApiGhostProperties apiGhostProperties(Environment environment) {
        ApiGhostProperties properties = new ApiGhostProperties();
        String basePackage = environment.getProperty("apighost.lib.basePackage");
        properties.setBasePackage(basePackage);
        return properties;
    }

    @Bean
    public EndPointProvider endPointProvider(ApiCollector apiCollector) {
        // need core method
        return new EndPointProvider(apiCollector);
    }

    @Bean
    public ScenarioGUIController scenarioGUIController() {
        return new ScenarioGUIController();
    }

    @Bean
    public EngineController engineController(ScenarioTestExecutor scenarioTestExecutor) {
        return new EngineController(scenarioTestExecutor);
    }

    @Bean
    public ApiCollector apiCollector(ApiGhostProperties apiGhostProperties) {
        return new ApiCollector(apiGhostProperties.getBasePackage());
    }

    @Bean
    public RestTemplate restTemplate(ApplicationContext applicationContext) {
        Map<String, RestTemplate> restTemplateBeans = applicationContext.getBeansOfType(
            RestTemplate.class);
        if (restTemplateBeans.isEmpty()) {
            return new RestTemplate();
        }
        return restTemplateBeans.values().iterator().next();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public ScenarioFileLoader scenarioFileLoader(){
        return new ScenarioFileLoader();
    }

    @Bean
    public ScenarioTestExecutor scenarioTestExecutor(RestTemplate restTemplate,
        ObjectMapper objectMapper, ScenarioFileLoader scenarioFileLoader, Environment env) {
        String filePath = env.getProperty("apighost.base.filepath");
        return new ScenarioTestExecutor(restTemplate, objectMapper, scenarioFileLoader, filePath);
    }

}
