package nl.owlstead.javautils;

import java.util.Optional;

interface Parameters {
    String testString();
    Optional<Bla> testOptionalBla();
    String testReverse();
    boolean testBoolean();
    int testInt();
}