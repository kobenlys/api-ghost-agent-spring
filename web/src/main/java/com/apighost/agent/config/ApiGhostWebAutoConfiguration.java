package com.apighost.agent.config;

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
        String basePackage = environment.getProperty("api.ghost.base-package");
        properties.setBasePackage(basePackage);
        return properties;
    }

    @Bean
    public EndPointProvider endPointProvider(ApiGhostProperties apiGhostProperties) {
        // need core method
        return new EndPointProvider();
    }

    @Bean
    public ScenarioGUIController scenarioGUIController() {
        return new ScenarioGUIController();
    }


}
