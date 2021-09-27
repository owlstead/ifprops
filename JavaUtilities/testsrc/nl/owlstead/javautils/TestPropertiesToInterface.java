package nl.owlstead.javautils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import nl.owlstead.javautils.PropertiesToInterface.Parser;

class TestPropertiesToInterface {

    @Test
    void test() {
        
        // create properties
        Properties props = new Properties();
        props.setProperty("parameters.testString", "Test world");
        props.setProperty("parameters.testInt", "1");
        props.setProperty("parameters.testBoolean", "true");
        props.setProperty("parameters.testOptionalBla", "Bla bla");
        props.setProperty("parameters.testReverse", "Reverse");
        props.setProperty("otherparameters.anotherConfigParameter", "anotherConfigParameter");
        
        // setup map of parsers specific to interface methods
        Map<String, Parser> parsers = new HashMap<>();
        // simple
        parsers.put("parameters.testReverse", s -> (new StringBuilder(s)).reverse().toString());
        // advanced: Optional and specific object
        parsers.put("parameters.testOptionalBla", s -> s == null ? Optional.empty() : Optional.of(new Bla() {
            @Override
            public String bla() {
                return s + " bla";
            }
        }));        
        
        Parameters testParams = PropertiesToInterface.propertiesToInterface(
                props, Parameters.class, parsers);        
        OtherParameters otherTestParams = PropertiesToInterface.propertiesToInterface(
                props, OtherParameters.class, Collections.emptyMap());
        
        assertEquals(testParams.testString(), "Test world");
        assertEquals(testParams.testInt(), 1);
        assertEquals(testParams.testBoolean(), true);
        assertEquals(testParams.testReverse(), "esreveR");
        assertEquals(testParams.testOptionalBla().get().bla(), "Bla bla bla");
        
        assertTrue(testParams.hashCode() == testParams.hashCode());
        assertTrue(testParams.equals(testParams));

        Parameters otherTestParamsInstance = PropertiesToInterface. propertiesToInterface(
                props, Parameters.class, parsers);
        assertFalse(testParams.equals(otherTestParamsInstance));
        assertEquals(otherTestParams.anotherConfigParameter(), "anotherConfigParameter");
    }
    
    @Test
    void testThingy() {
        PropertiesToInterface.createPropertiesTemplate(Parameters.class, System.out);
    }
}
