package nl.owlstead.javautils;

import java.io.PrintStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

public class PropertiesToInterface {

    
    private static class PropertyRetriever<I> implements InvocationHandler {

        private Class<I> classOfInterface;
        private HashMap<String, Object> returnValues = new HashMap<>();
        
        public PropertyRetriever(Properties properties, Class<I> classOfInterface, Map<String, Parser> parsers) {
            this.classOfInterface = classOfInterface;
            
            Method[] declaredMethods = classOfInterface.getDeclaredMethods();
            for (Method method : declaredMethods) {
                String methodName = method.getName();
                
                String lowerCaseClassName = classOfInterface.getSimpleName().toLowerCase();
                String propertyValue = properties.getProperty(
                        lowerCaseClassName + "." + methodName);
                
                String canonicalNameOfType = method.getReturnType().getCanonicalName();
                
                Parser parser = parsers.get(lowerCaseClassName + "." + methodName);
                if (parser != null) {
                    Object parsedObject = parser.parse(propertyValue);
                    if (!method.getReturnType().isAssignableFrom(parsedObject.getClass())) {
                        throw new RuntimeException("Parser for " + methodName + " didn't create the right class type");
                    }
                    returnValues.put(methodName, parsedObject);
                    continue;
                }
                
                switch (canonicalNameOfType) {
                case "java.lang.String":
                    returnValues.put(methodName, propertyValue);
                    continue;
                case "int":
                    returnValues.put(methodName, Integer.parseInt(propertyValue));
                    continue;
                case "boolean":
                    returnValues.put(methodName, Boolean.parseBoolean(propertyValue));
                    continue;
                default:
                    throw new RuntimeException("Sorry, don't know how to convert string property for "
                            + methodName + " to " + canonicalNameOfType);
                }
            }
        }
        
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // we'll use the proxy to return some valid, but possibly non-sensible values for the generic methods
            // if you call e.g. wait() on the object you're out of luck though
            if (!method.getDeclaringClass().equals(classOfInterface)) {
                switch (method.getName()) {
                case "equals":
                    return args[0] == proxy;
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "toString":
                    return returnValues.toString();
                case "getClass":
                    return proxy.getClass();
                default:
                    throw new RuntimeException("Unimplemented method for proxying interface");
                }
            }
            
            if (method.getParameterCount() != 0 || method.getReturnType().equals(Void.TYPE)) {
                throw new RuntimeException("Only interfaces without arguments and a single return type are allowed");
            }

            return returnValues.get(method.getName());
        }
    }
    
    public interface Parser {
        Object parse(String s);
    }
    
    @SuppressWarnings("unchecked")
    public static <I> I propertiesToInterface(
            Properties props, Class<I> classOfInterface, Map<String, Parser> parsers) {
        PropertyRetriever <I> retriever = new PropertyRetriever<I>(props, classOfInterface, parsers);
        return (I) Proxy.newProxyInstance(
                classOfInterface.getClassLoader(),
                new Class[] { classOfInterface },
                retriever);
    }
    
    public static <I> void createPropertiesTemplate(Class<I> classOfInterface, PrintStream out) {
        String lowerCaseClassName = classOfInterface.getSimpleName().toLowerCase();
        out.format("# Auto-generated property file for test interface %s in package %s%n",
                classOfInterface.getSimpleName(), classOfInterface.getPackageName());
        
        Method[] declaredMethods = classOfInterface.getDeclaredMethods();
        for (Method method : declaredMethods) {
            String methodName = lowerCaseClassName + "." + method.getName();
            String canonicalNameOfType = method.getReturnType().getCanonicalName();
            out.format("%s: <%s>%n", methodName, canonicalNameOfType);
        }
    }
    
}
