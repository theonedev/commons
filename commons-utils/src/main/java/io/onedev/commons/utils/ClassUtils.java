package io.onedev.commons.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.jspecify.annotations.Nullable;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;

public class ClassUtils extends org.apache.commons.lang3.ClassUtils {
	
	public static @Nullable InputStream getResourceAsStream(@Nullable Class<?> locator, String path) {
		path = StringUtils.stripStart(path, "/");
		if (locator == null)
			return ClassUtils.class.getClassLoader().getResourceAsStream(path);
		else
			return locator.getClassLoader().getResourceAsStream(locator.getPackage().getName().replace(".", "/") + "/" + path);
	}

	public static boolean isSystemType(Class<?> type) {
		return type.getName().startsWith("java") 
			|| type.getName().startsWith("javax") 
			|| type.isArray()
			|| type.isPrimitive()
			|| type.isEnum();
	}
	
	/**
	 * Find all sub classes inheriting from (or implementing) specified super class (or interface) 
	 * recursively in package containing the package class.
	 * 
	 * @param superClass 
	 * 			super class (or interface) to match. 
	 * @param packageScope 
	 * 			find sub classes in the same package as this class. Package will be searched recursively.
	 * @return collection of sub classes (not include the super class)
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<Class<? extends T>> findSubClasses(Class<T> superClass, Class<?> packageScope) {
		Preconditions.checkNotNull(superClass);
		Preconditions.checkNotNull(packageScope);
		
		List<Class<? extends T>> classes = new ArrayList<Class<? extends T>>();
		
		String path;
		try {
			path = packageScope.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
		
		File location = new File(path);
		
		if (location.isFile()) {
			String packagePath = packageScope.getPackage().getName().replace('.', '/') + "/";
			JarFile jarFile = null;
			try {
				jarFile = new JarFile(location);
				Enumeration<JarEntry> entries = jarFile.entries();
				while (entries.hasMoreElements()) {
					JarEntry entry = entries.nextElement();
					if (entry.getName().startsWith(packagePath) && entry.getName().endsWith(".class")) {
						String className = entry.getName().replace('/', '.');
						className = StringUtils.substringBeforeLast(className, ".");
						Class<T> clazz;
						try {
							clazz = (Class<T>) packageScope.getClassLoader().loadClass(className);
						} catch (ClassNotFoundException e) {
							throw new RuntimeException(e);
						}
						if (superClass.isAssignableFrom(clazz) && clazz != superClass)
							classes.add(clazz);
					}
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			} finally {
				if (jarFile != null) {
					try {
						jarFile.close();
					} catch (IOException e) {
					}
				}
			}
		} else {
			String packagePath = packageScope.getPackage().getName().replace('.', File.separatorChar);
			File packageDir = new File(location, packagePath);
			if (packageDir.exists()) {
				for (File file: FileUtils.listFiles(packageDir, new String[]{"class"}, true)) {
					Class<T> clazz;
					try {
						String relativePath = PathUtils.parseRelative(file.getAbsolutePath(), 
								packageDir.getAbsolutePath());
						String className = packageScope.getPackage().getName() + 
								StringUtils.substringBeforeLast(relativePath.replace('/', '.'), ".");
						ClassLoader classLoader = superClass.getClassLoader();
						if (classLoader == null)
							classLoader = packageScope.getClassLoader();
						Preconditions.checkNotNull(classLoader);
						clazz = (Class<T>) classLoader.loadClass(className);
					} catch (ClassNotFoundException e) {
						throw new RuntimeException(e);
					}
					if (superClass.isAssignableFrom(clazz) && clazz != superClass)
						classes.add(clazz);
				}
			} else {
				throw new IllegalStateException("Unable to find package directory: " + 
						packageDir.getAbsolutePath());
			}
		}
		classes.sort(Comparator.comparing(Class::getName));
		return classes;
	}
	
	public static boolean isConcrete(Class<?> clazz) {
		return !clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers());		
	}
	
	public static <T> List<Class<? extends T>> findImplementations(Class<T> abstractClass, Class<?> packageScope) {
		List<Class<? extends T>> implementations = new ArrayList<Class<? extends T>>();
		
		for (Class<? extends T> each: findSubClasses(abstractClass, packageScope)) {
			if (isConcrete(each))
				implementations.add(each);
		}
		return implementations;
	}
	
}
