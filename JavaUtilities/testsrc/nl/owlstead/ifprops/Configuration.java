package nl.owlstead.ifprops;

import java.util.Optional;

/**
 * Example configuration interface.
 * 
 * @author maartenb
 */
interface Configuration {
    String testString();
    Optional<Bla> testOptionalBla();
    String testReverse();
    boolean testBoolean();
    int testInt();
}