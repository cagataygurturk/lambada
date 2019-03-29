package org.lambadaframework.jaxrs;


import org.lambadaframework.jaxrs.model.Resource;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JAXRSParser {

    private String packageName;
    private String jarUrl;
    private Class clazz;


    public JAXRSParser() {

    }

    public JAXRSParser withJarFile(String jarUrl) {
        this.jarUrl = jarUrl;
        return this;
    }

    public JAXRSParser withJarFile(String jarUrl, String packageName) {
        this.jarUrl = jarUrl;
        this.packageName = packageName;
        return this;
    }

    public JAXRSParser withPackageName(String packageName, Class clazz) {
        this.packageName = packageName;
        this.clazz = clazz;
        return this;
    }

    private List<Class<? extends Object>> getClassesInJarFile(String jarUrl)
            throws IOException {

        URL[] urls = {new URL("jar:file:" + jarUrl + "!/")};
        JarFile jarFile = new JarFile(jarUrl);
        URLClassLoader cl = new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());


        List<Class<? extends Object>> classes = new LinkedList<>();


        Enumeration e = jarFile.entries();
        while (e.hasMoreElements()) {
            JarEntry je = (JarEntry) e.nextElement();
            if (je.isDirectory() || !je.getName().endsWith(".class")) {
                continue;
            }
            // -6 because of .class
            String className = je.getName().substring(0, je.getName().length() - 6);
            className = className.replace('/', '.');

            if (this.packageName != null && !className.startsWith(packageName)) {
                continue;
            }

            try {
                Class c = Class.forName(className, false, cl);
                classes.add(c);
            } catch (ClassNotFoundException | NoClassDefFoundError exception) {
                System.out.printf("Can't load class: " + exception.getMessage());
            }

        }
        
        jarFile.close();
        cl.close();
        
        return classes;
    }


    private List<Class<? extends Object>> getClassesInPackage(String packageName) {
        return getClassesInPackage(packageName, JAXRSParser.class);
    }


    /**
     * Get classes in the specified package.
     *
     * @param packageName Package Name to search
     * @param clazz       Class Name
     * @return
     */
    private List<Class<? extends Object>> getClassesInPackage(String packageName, Class clazz) {
        List<Class<? extends Object>> classes = new LinkedList<>();

        try {
            final String classExtension = ".class";
            final String blank = "";
            final String classSeperator = ".";


            final URL location = clazz.getProtectionDomain().getCodeSource().getLocation();
			final String jarPath = new File(location.toURI()).getAbsolutePath();

            if (jarPath.endsWith(".jar")) {
                /**
                 * This class is in JAR File, we can scan it
                 */
                return getClassesInJarFile(jarPath);
            }

            final String packagePath = jarPath + File.separator + packageName.replace(classSeperator, File.separator);

            Files.walkFileTree(Paths.get(packagePath), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                        throws IOException {

                    String fileName = file.toString();

                    if (fileName.endsWith(classExtension)) {
                        String className = fileName.replace(jarPath + File.separator, blank).replace(File.separator, classSeperator);
                        className = className.substring(0, className.length() - classExtension.length());

                        try {
                            classes.add(
                                    Class.forName(className)
                            );
                        } catch (ClassNotFoundException | NoClassDefFoundError e) {
                            return FileVisitResult.CONTINUE;
                        }

                    }


                    return FileVisitResult.CONTINUE;
                }
            });

        } catch (URISyntaxException | IOException e) {
            return classes;
        }

        return classes;
    }


    /**
     * Scans package for JAX-RS Annotations
     *
     * @return Found resources
     */
    public List<Resource> scan() {
        List<Resource> foundResources = new LinkedList<>();

        List<Class<? extends Object>> classes;

        try {
            if (jarUrl == null) {
                if (this.clazz != null) {
                    classes = getClassesInPackage(this.packageName, this.clazz);
                } else {
                    classes = getClassesInPackage(this.packageName);
                }
            } else {
                classes = getClassesInJarFile(jarUrl);
            }
        } catch (IOException e) {
            return foundResources;
        }


        for (Class clazz : classes) {
            foundResources.addAll(getResourcesFromClassRecursive(clazz));
        }

        return foundResources;
    }


    private List<Resource> getResourcesFromClassRecursive(Class clazz) {
        List<Resource> foundResources = new LinkedList<>();

        try {
            org.glassfish.jersey.server.model.Resource jerseyResource = org.glassfish.jersey.server.model.Resource.from(clazz, true);

            if (jerseyResource == null) {
                return foundResources;
            }

            Resource resource = new Resource(jerseyResource);
            foundResources.add(resource);
            return getResourcesFromClassRecursive(resource, foundResources);
        } catch (Exception e) {
            return foundResources;
        }
    }

    private List<Resource> getResourcesFromClassRecursive(Resource resource, List<Resource> foundResources) {

        if (resource.getChildResources().size() > 0) {
            for (Resource subResource : resource.getChildResources()) {
                foundResources = getResourcesFromClassRecursive(subResource, foundResources);
            }
        } else {
            foundResources.add(resource);
        }

        return foundResources;
    }


}
