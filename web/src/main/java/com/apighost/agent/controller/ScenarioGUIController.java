package com.apighost.agent.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller responsible for serving the web-based scenario testing UI.
 * <p>
 * This controller forwards requests to the HTML frontend for the API Ghost scenario test GUI. It
 * acts as an entry point to access the UI through a predefined path.
 * </p>
 *
 * @author kobenlys
 * @version BETA-0.0.1
 */
@Controller
public class ScenarioGUIController {

    /**
     * Handles HTTP GET requests to "/apighost-ui" and forwards them to the static HTML page.
     * <p>
     * This method allows users to access the API Ghost UI from the browser. The frontend should be
     * located at {@code /apighost-ui.html} in the static resources.
     * </p>
     *
     * @return a forward directive to the scenario testing HTML UI
     */
    @GetMapping("/apighost-ui")
    public String showGUI() {
        return "forward:/apighost-ui.html";
    }
}
