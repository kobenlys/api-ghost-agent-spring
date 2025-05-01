package com.apighost.agent.config;

import com.apighost.agent.collector.ApiCollector;
import com.apighost.agent.controller.EndPointProvider;
import com.apighost.agent.controller.ScenarioGUIController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

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
    public ApiCollector apiCollector(ApiGhostProperties apiGhostProperties) {
        return new ApiCollector(apiGhostProperties.getBasePackage());
    }


}
