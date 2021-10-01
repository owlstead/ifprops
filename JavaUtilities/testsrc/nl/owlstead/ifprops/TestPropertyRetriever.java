package nl.owlstead.ifprops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.junit.jupiter.api.Test;

class TestPropertyRetriever {

    @Test
    void test() {
        
        // create properties
        Properties props = new Properties();
        props.setProperty("configuration.testString", "Test world");
        props.setProperty("configuration.testInt", "1");
        props.setProperty("configuration.testBoolean", "true");
        props.setProperty("configuration.testOptionalBla", "Bla bla");
        props.setProperty("configuration.testReverse", "Reverse");
        props.setProperty("otherconfiguration.anotherConfigParameter", "anotherConfigParameter");
        
        // setup map of parsers specific to interface methods
        Map<String, PropertyValueParser> parsers = new HashMap<>();
        // simple
        parsers.put("configuration.testReverse", s -> (new StringBuilder(s)).reverse().toString());
        // advanced: Optional and specific object
        parsers.put("configuration.testOptionalBla", s -> s == null ? Optional.empty() : Optional.of(new Bla() {
            @Override
            public String bla() {
                return s + " bla";
            }
        }));        
        
        Configuration configuration = PropertyRetrieverFactory.createPropertyRetriever(
                props, Configuration.class, parsers);        
        OtherConfiguration otherConfiguration = PropertyRetrieverFactory.createPropertyRetriever(
                props, OtherConfiguration.class, Collections.emptyMap());
        
        assertEquals(configuration.testString(), "Test world");
        assertEquals(configuration.testInt(), 1);
        assertEquals(configuration.testBoolean(), true);
        assertEquals(configuration.testReverse(), "esreveR");
        assertEquals(configuration.testOptionalBla().get().bla(), "Bla bla bla");
        
        assertTrue(configuration.hashCode() == configuration.hashCode());
        assertTrue(configuration.equals(configuration));

        Configuration configurationDupe = PropertyRetrieverFactory. createPropertyRetriever(
                props, Configuration.class, parsers);
        assertFalse(configuration.equals(configurationDupe));
        assertEquals(otherConfiguration.anotherConfigParameter(), "anotherConfigParameter");
    }
    
    @Test
    void testThingy() {
        PropertyRetrieverFactory.createPropertiesTemplate(Configuration.class, System.out);
    }
}
