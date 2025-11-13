/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Represents the configuration for a module in the trace link analysis system.
 * This class manages module-specific settings and arguments, providing type-safe
 * access to configuration values and ensuring all arguments are properly retrieved
 * before serialization.
 */
public final class ModuleConfiguration {
    /**
     * Error message thrown when attempting to access arguments after finalization.
     */
    public static final String ALREADY_FINALIZED_FOR_SERIALIZATION =
            "Configuration already finalized for serialization";

    /**
     * The name of the module.
     */
    @JsonProperty("name")
    private final String name;

    /**
     * The arguments for the module, stored as key-value pairs.
     */
    @JsonProperty("args")
    private final Map<String, String> arguments;

    /**
     * Stores the retrieved arguments for serialization.
     * This ensures that only arguments that were actually used are included
     * in the serialized configuration.
     */
    @JsonIgnore
    private final Map<String, String> retrievedArguments = new LinkedHashMap<>();

    /**
     * Flag indicating whether this configuration has been finalized for serialization.
     */
    @JsonIgnore
    private boolean finalized = false;

    /**
     * Creates a new module configuration with the specified name and arguments.
     *
     * @param name      The name of the module
     * @param arguments The arguments for the module
     */
    @JsonCreator
    public ModuleConfiguration(@JsonProperty("name") String name, @JsonProperty("args") Map<String, String> arguments) {
        this.name = name;
        this.arguments = arguments;
    }

    /**
     * Returns the name of the module.
     *
     * @return The module name
     */
    public String name() {
        return name;
    }

    /**
     * Checks if an argument with the specified key exists.
     *
     * @param key The key to check
     * @return true if the argument exists, false otherwise
     */
    public boolean hasArgument(String key) {
        return this.arguments.get(key) != null;
    }

    /**
     * Retrieves an argument as a string.
     * Throws an exception if the argument is not found or if the configuration
     * has been finalized for serialization.
     *
     * @param key The key of the argument to retrieve
     * @return The argument value as a string
     * @throws IllegalStateException    If the configuration has been finalized
     * @throws IllegalArgumentException If the argument is not found
     */
    public String argumentAsString(String key) {
        if (finalized) {
            throw new IllegalStateException(ALREADY_FINALIZED_FOR_SERIALIZATION);
        }

        String argument = arguments.get(key);
        if (argument == null) {
            throw new IllegalArgumentException("Argument with key " + key + " not found in configuration " + this);
        }
        retrievedArguments.put(key, argument);
        return argument;
    }

    /**
     * Retrieves an argument as a string, using a default value if not found.
     * Throws an exception if the configuration has been finalized for serialization
     * or if the default value conflicts with a previously retrieved value.
     *
     * @param key          The key of the argument to retrieve
     * @param defaultValue The default value to use if the argument is not found
     * @return The argument value as a string, or the default value
     * @throws IllegalStateException    If the configuration has been finalized
     * @throws IllegalArgumentException If the default value conflicts with a previously retrieved value
     */
    public String argumentAsString(String key, String defaultValue) {
        if (finalized) {
            throw new IllegalStateException(ALREADY_FINALIZED_FOR_SERIALIZATION);
        }

        String argument = arguments.getOrDefault(key, defaultValue);
        String retrievedArgument = retrievedArguments.put(key, argument);
        if (retrievedArgument != null && !retrievedArgument.equals(argument)) {
            throw new IllegalArgumentException("Default argument for key " + key + " already set to "
                    + retrievedArgument + " and cannot be changed to " + defaultValue);
        }
        return argument;
    }

    /**
     * Retrieves an argument as an integer.
     *
     * @param key The key of the argument to retrieve
     * @return The argument value as an integer
     * @throws NumberFormatException If the argument cannot be parsed as an integer
     */
    public int argumentAsInt(String key) {
        return Integer.parseInt(argumentAsString(key));
    }

    /**
     * Retrieves an argument as an integer, using a default value if not found.
     *
     * @param key          The key of the argument to retrieve
     * @param defaultValue The default value to use if the argument is not found
     * @return The argument value as an integer, or the default value
     * @throws NumberFormatException If the argument cannot be parsed as an integer
     */
    public int argumentAsInt(String key, int defaultValue) {
        return Integer.parseInt(argumentAsString(key, String.valueOf(defaultValue)));
    }

    /**
     * Retrieves an argument as a boolean.
     *
     * @param key The key of the argument to retrieve
     * @return The argument value as a boolean
     */
    public boolean argumentAsBoolean(String key) {
        return Boolean.parseBoolean(argumentAsString(key));
    }

    /**
     * Retrieves an argument as a boolean, using a default value if not found.
     *
     * @param key          The key of the argument to retrieve
     * @param defaultValue The default value to use if the argument is not found
     * @return The argument value as a boolean, or the default value
     */
    public boolean argumentAsBoolean(String key, boolean defaultValue) {
        return Boolean.parseBoolean(argumentAsString(key, String.valueOf(defaultValue)));
    }

    /**
     * Retrieves an argument as a string by enum index.
     * The argument can be either a numeric index into the enum array or the
     * transformed string value itself.
     *
     * @param <E>          The enum type
     * @param key          The key of the argument to retrieve
     * @param defaultIndex The default index to use if the argument is not found
     * @param values       The array of enum values
     * @param transform    Function to transform enum values to strings
     * @return The transformed string value
     * @throws IllegalStateException    If the configuration has been finalized
     * @throws IllegalArgumentException If the index is out of bounds or if the default value conflicts
     */
    public <E extends Enum<E>> String argumentAsStringByEnumIndex(
            String key, int defaultIndex, E[] values, Function<E, String> transform) {
        if (finalized) {
            throw new IllegalStateException(ALREADY_FINALIZED_FOR_SERIALIZATION);
        }

        String value = arguments.getOrDefault(key, String.valueOf(defaultIndex));
        // If not a number, it can be the text itself
        try {
            int index = Integer.parseInt(value);
            if (index < 0 || index >= values.length) {
                throw new IllegalArgumentException(
                        "Index " + index + " out of bounds for enum " + Arrays.toString(values));
            }
            value = transform.apply(values[index]);
        } catch (NumberFormatException e) {
            // It's not a number, so it's the text itself
        }

        String retrievedArgument = retrievedArguments.put(key, value);
        if (retrievedArgument != null && !retrievedArgument.equals(value)) {
            throw new IllegalArgumentException("Default argument for key " + key + " already set to "
                    + retrievedArgument + " and cannot be changed to " + value);
        }
        return value;
    }

    /**
     * Finalizes this configuration for serialization.
     * This method ensures that all arguments have been retrieved and prepares
     * the configuration for serialization.
     *
     * @throws IllegalStateException If any arguments have not been retrieved
     */
    void finalizeForSerialization() {
        if (finalized) {
            return;
        }

        finalized = true;
        arguments.putAll(retrievedArguments);

        for (var argumentKey : arguments.keySet()) {
            if (!retrievedArguments.containsKey(argumentKey)) {
                throw new IllegalStateException(
                        "Argument with key " + argumentKey + " not retrieved from configuration " + this);
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ModuleConfiguration) obj;
        return Objects.equals(this.name, that.name) && Objects.equals(this.arguments, that.arguments);
    }

    /**
     * Returns a hash code for this ModuleConfiguration, based on its name and arguments.
     *
     * @return Hash code for this configuration
     */
    @Override
    public int hashCode() {
        return Objects.hash(name, arguments);
    }

    /**
     * Returns a string representation of this ModuleConfiguration,
     * including the module name and all argument key-value pairs.
     *
     * @return String representation of this ModuleConfiguration
     */
    @Override
    public String toString() {
        return "ModuleConfiguration[name=" + name + ", arguments=" + arguments + ']';
    }

    /**
     * Returns the argument map for this ModuleConfiguration.
     *
     * @return A map of argument names to values
     */
    public Map<String, String> arguments() {
        return arguments;
    }
}
