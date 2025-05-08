package com.apighost.agent.model;
/**
 * Represents a parameter with a type and a name.
 *
 * <p>This class is used to describe method or endpoint parameters.</p>
 *
 * @author oneweeek
 * @version BETA-0.0.1
 */
public class Parameter {

    private final String type;
    private final String name;
    /**
     * Constructs a new {@code Parameter} instance with the specified type and name.
     *
     * @param type the parameter type
     * @param name the parameter name
     */
    public Parameter(String type, String name) {
        this.type = type;
        this.name = name;
    }
    /**
     * Returns the parameter type.
     *
     * @return the type as a string
     */
    public String getType() {
        return type;
    }
    /**
     * Returns the parameter name.
     *
     * @return the name as a string
     */
    public String getName() {
        return name;
    }
}
