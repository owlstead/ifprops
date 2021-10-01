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

import java.io.PrintStream;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

/**
 * A factory for creating a Java proxy that is used to read a specifically formatted
 * properties file for a configuration interface.
 * Both the factory and proxy will use reflection to accomplish this.
 * This allows a developer to simply define the configuration using an interface.
 * The factory and proxy frees the implementation from
 * being bound to any specific way of loading a configuration.
 * 
 * <p>
 * 
 * This class immediately loads and parses the required properties and is thus explicitly designed to be fail-fast.
 *
 * <p>
 * 
 * Parsers can be specified for specific return types.
 * By default the following return types are handled:
 * 
 * <table style="margin: 6pt;">
 * <tr>
 *    <td><code>String</code></td>
 *    <td>Directly returned, not parsed</td>
 * </tr>
 * <tr>
 *    <td><code>int<br>short<br>byte<br>double<br>float<br>boolean</code></td>
 *    <td>Returned according to <code>Integer.parseInt(String)</code>, <code>Short.parseShort(String)</code> etc.</td>
 *</tr>
 * <tr>
 *    <td><code>byte[]</code> (the <code>[B</code> type)</td>
 *    <td>Returned as an array consisting of any byte represented by a double hex digit</td>
 * <tr>
 * </table> 
 * 
 * 
 * <p>
 * 
 * For instance, if you have a single interface that represents a configuration:
 * <br><br>
 * <pre>
 * interface Configuration {
 *     String configurableMessage();
 *     int configurableMessageCount();
 * }
 * </pre>
 * 
 * Then this class can be used to create a proxy interface that reads a properties file with the following contents:
 * 
 * <pre>
 * configuration.configurableMessage: "Hello World"
 * configuration.configurableMessageCount: 1
 * </pre>

 * Because the interface type name is used as prefix it is possible to represent multiple configuration interfaces
 * using only one <code>Properties</code> instance.  
 *
 * <p>
 * 
 * @since 2021
 * @author maartenb
 */
public class PropertyRetrieverFactory {

    /**
     * Factory method that creates a property retriever for relatively simple classes that only operate on
     * base types and strings.
     * 
     * {@link PropertyRetrieverFactory#createPropertyRetriever(Properties, Class, Map)}
     */
    public static <I> I createPropertyRetriever(
            Properties props, Class<I> classOfInterface) {
        return createPropertyRetriever(props, classOfInterface, Collections.emptyMap());
    }
    
    @SuppressWarnings("unchecked")
    public static <I> I createPropertyRetriever(
            Properties props, Class<I> classOfInterface, Map<String, PropertyValueParser> parsers) {
        PropertyRetrieverProxy <I> retriever = new PropertyRetrieverProxy<I>(props, classOfInterface, parsers);
        return (I) Proxy.newProxyInstance(
                classOfInterface.getClassLoader(),
                new Class[] { classOfInterface },
                retriever);
    }
    
    /**
     * Helper method that creates a template for a properties file (or stream).
     * @param <I> the type of the interface that will be proxied
     * @param classOfInterface the class of the interface that will be proxied 
     * @param out the 
     */
    public static <I> void createPropertiesTemplate(Class<I> classOfInterface, PrintStream out) {
        String lowerCaseClassName = classOfInterface.getSimpleName().toLowerCase();
        out.format("# Auto-generated property file for test interface %s in package %s%n",
                classOfInterface.getSimpleName(), classOfInterface.getPackageName());
        
        Method[] declaredMethods = classOfInterface.getDeclaredMethods();
        for (Method method : declaredMethods) {
            String methodName = lowerCaseClassName + "." + method.getName();
            String canonicalNameOfType = method.getReturnType().getSimpleName();
            out.format("%s: <%s>%n", methodName, canonicalNameOfType);
        }
    }
}
