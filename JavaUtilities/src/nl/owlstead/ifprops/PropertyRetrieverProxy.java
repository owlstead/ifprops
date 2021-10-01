/*
 * Copyright (c) 2021 Maarten Bodewes
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package nl.owlstead.ifprops;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents the methods defined by an interface as well as the base Object class, using properties to proxy the
 * defined "getter" methods therein.
 * 
 * A getter method is defined as a method with a return type and no parameters.
 * 
 * @author maartenb
 *
 * @param <I> the interface to represent
 */
class PropertyRetrieverProxy<I> implements InvocationHandler {
    
    private static final Pattern HEX_BYTE_PATTERN = Pattern.compile("\\p{XDigit}{2}");

    private Class<I> classOfInterface;
    private HashMap<String, Object> returnValues = new HashMap<>();
    
    public PropertyRetrieverProxy(Properties properties, Class<I> classOfInterface, Map<String, PropertyValueParser> parsers) {
        this.classOfInterface = classOfInterface;
        
        Method[] declaredMethods = classOfInterface.getDeclaredMethods();
        for (Method method : declaredMethods) {
            String methodName = method.getName();
            
            if (method.getParameterCount() != 0) {
                throw new RuntimeException("Not a configuration interface, method " + methodName + " has parameters");
            }
            
            String lowerCaseClassName = classOfInterface.getSimpleName().toLowerCase();
            String propertyValue = properties.getProperty(
                    lowerCaseClassName + "." + methodName);
            
            String canonicalNameOfType = method.getReturnType().getCanonicalName();
            
            PropertyValueParser parser = parsers.get(lowerCaseClassName + "." + methodName);
            if (parser != null) {
                Object parsedObject = parser.parse(propertyValue);
                if (!method.getReturnType().isAssignableFrom(parsedObject.getClass())) {
                    throw new RuntimeException("Parser for " + methodName + " didn't create the right class type");
                }
                returnValues.put(methodName, parsedObject);
                continue;
            }
            
            // TODO extend parsing of integer types to hexadecimal values
            switch (canonicalNameOfType) {
            case "java.lang.String":
                returnValues.put(methodName, propertyValue);
                continue;
            case "int":
                returnValues.put(methodName, Integer.parseInt(propertyValue));
                continue;
            case "short":
                returnValues.put(methodName, Short.parseShort(propertyValue));
                continue;
            case "byte":
                returnValues.put(methodName, Byte.parseByte(propertyValue));
                continue;
            case "boolean":
                returnValues.put(methodName, Boolean.parseBoolean(propertyValue));
                continue;
            case "float":
                returnValues.put(methodName, Float.parseFloat(propertyValue));
                continue;
            case "double":
                returnValues.put(methodName, Double.parseDouble(propertyValue));
                continue;
            case "char":
                if (propertyValue.length() != 1) {
                    throw new RuntimeException("Expecting a single character, found zero or more characters");
                }
                returnValues.put(methodName, propertyValue.charAt(0));
                continue;
            case "[B":
                returnValues.put(methodName, parseByteArray(propertyValue));
                continue;
            default:
                throw new RuntimeException("Sorry, don't know how to convert string property for "
                        + methodName + " to " + canonicalNameOfType);
            }
        }
    }
        
    private byte[] parseByteArray(String propertyValue) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(propertyValue.length() / 2)) {
            Matcher hexByteMatcher = HEX_BYTE_PATTERN.matcher(propertyValue);
            while (hexByteMatcher.find()) {
                baos.write(Byte.parseByte(hexByteMatcher.group(), 16));
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Unexpected I/O error for ByteArrayOutputStream", e);
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