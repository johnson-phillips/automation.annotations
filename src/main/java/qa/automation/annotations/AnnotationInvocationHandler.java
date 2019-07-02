package qa.automation.annotations;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

/**
 * Created by johnson_phillips on 12/24/17.
 */
public  class AnnotationInvocationHandler implements InvocationHandler {

    private final Annotation customAnnotation;

    public AnnotationInvocationHandler(Annotation customAnnotation) {
        this.customAnnotation = customAnnotation;
    }

    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        //trying to call the method on the custom annotation, if it exists
        Method methodOnCustom = getMatchingMethodOnGivenAnnotation(method);
        if (methodOnCustom != null) {
            return  methodOnCustom.invoke(customAnnotation, args);
        } else {
            //otherwise getting the default value of the reference annotation method
            Object defaultValue = method.getDefaultValue();
            if (defaultValue != null) {
                return defaultValue;
            }
            throw new UnsupportedOperationException(
                    "The method \""
                            + method.getName()
                            + "\" does not exist in the custom annotation, and there is no default value for"
                            + " it in the reference annotation, please implement this method in your custom annotation.");
        }
    }

    private Method getMatchingMethodOnGivenAnnotation(Method method) {
        try {
            Method customMethod = customAnnotation.getClass().getDeclaredMethod(method.getName(), method.getParameterTypes());
            if (customMethod.getReturnType().isAssignableFrom(method.getReturnType())) {
                return customMethod;
            }
            return null;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    public static HashMap<String,String> createDatasetTemplate(String packagename) throws Exception
    {
        HashMap<String,String> params = new HashMap<String, String>();
        try {
            Class[] modules = getClasses(packagename);
            for(Class classname:modules)
            {
                Method[] methods = classname.getMethods();
                for (Method m : methods) {
                    Annotation[] annotations = m.getDeclaredAnnotations();

                    if (annotations.length > 0) {
                        InvocationHandler handler = new AnnotationInvocationHandler(annotations[0]);

                        Object obj = Proxy.newProxyInstance(Parameters.class.getClassLoader(), Parameters.class.getInterfaces(), handler);
                        try {
                            Object parameters = handler.invoke(obj, Parameters.class.getMethod("value"), null);
                            //info.parameters = (String[]) parameters;
                            for(String parameter:(String[]) parameters)
                            {
                                params.put(parameter,"");
                            }
                        } catch (Throwable ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
            return params;
        }

        catch(Exception ex)
        {
            ex.printStackTrace();
        }
        return params;
    }

    private static Class[] getClasses(String packageName) throws ClassNotFoundException, IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        assert classLoader != null;
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);
        List<File> dirs = new ArrayList<File>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            dirs.add(new File(resource.getFile()));
        }
        ArrayList<Class> classes = new ArrayList<Class>();
        for (File directory : dirs) {
            classes.addAll(findClasses(directory, packageName));
        }
        return classes.toArray(new Class[classes.size()]);
    }

    private static List<Class> findClasses(File directory, String packageName) throws ClassNotFoundException {
        List<Class> classes = new ArrayList<Class>();
        if (!directory.exists()) {
            return classes;
        }
        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                assert !file.getName().contains(".");
                classes.addAll(findClasses(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
            }
        }
        return classes;
    }
}
