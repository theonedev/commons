package io.onedev.commons.launcher.bootstrap;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.logging.Handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

public class Bootstrap {

	private static final Logger logger = LoggerFactory.getLogger(Bootstrap.class);

	public static final int SOCKET_CONNECT_TIMEOUT = 60000;

	public static final String APP_LOADER_PROPERTY_NAME = "appLoader";

	public static final String LOGBACK_CONFIG_FILE_PROPERTY_NAME = "logback.configurationFile";

	public static final String DEFAULT_APP_LOADER = "io.onedev.commons.launcher.loader.AppLoader";

	public static File installDir;
	
	private static File libCacheDir;

	public static boolean sandboxMode;

	public static boolean prodMode;
	
	public static Command command;

	public static void main(String[] args) {
		try {
			Locale.setDefault(Locale.US);
			/*
			 * Sandbox mode might be checked frequently so we cache the result here
			 * to avoid calling File.exists() frequently.
			 */
			sandboxMode = new File("target/sandbox").exists();
			prodMode = (System.getProperty("prod") != null);

			String path;
			try {
				path = Bootstrap.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
			} catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}
			File loadedFrom = new File(path);

			if (new File(loadedFrom.getParentFile(), "bootstrap.keys").exists())
				installDir = loadedFrom.getParentFile().getParentFile();
			else if (new File("target/sandbox").exists())
				installDir = new File("target/sandbox");
			else
				throw new RuntimeException("Unable to find product directory.");

			try {
				installDir = installDir.getCanonicalFile();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			if (args.length != 0) {
				String[] commandArgs = new String[args.length-1];
				System.arraycopy(args, 1, commandArgs, 0, commandArgs.length);
				command = new Command(args[0], commandArgs);
			} else {
				command = null;
			}

			configureLogging();
			try {
				logger.info("Launching application from '" + installDir.getAbsolutePath() + "'...");

		        File tempDir = getTempDir();
				if (tempDir.exists()) {
					logger.info("Cleaning temp directory...");
					Files.walk(tempDir.toPath()).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
				} 
				
				if (!tempDir.mkdirs())
					throw new RuntimeException("Can not create directory '" + tempDir.getAbsolutePath() + "'");
				
				System.setProperty("java.io.tmpdir", tempDir.getAbsolutePath());

				libCacheDir = createTempDir("libcache");
				
				List<File> libFiles = new ArrayList<>();

				File classpathFile = new File(installDir, "boot/system.classpath");
				if (classpathFile.exists()) {
					@SuppressWarnings("unchecked")
					Map<String, File> systemClasspath = (Map<String, File>) BootstrapUtils.readObject(classpathFile);
					@SuppressWarnings("unchecked")
					Set<String> bootstrapKeys = (Set<String>) BootstrapUtils.readObject(
							new File(Bootstrap.installDir, "boot/bootstrap.keys"));
					for (Map.Entry<String, File> entry : systemClasspath.entrySet()) {
						if (!bootstrapKeys.contains(entry.getKey())) 
							libFiles.add(entry.getValue());
					}					
				} else {
					libFiles.addAll(getLibFiles(getLibDir()));
					cacheLibFiles(getLibDir());
				}
				
				File siteLibDir = new File(getSiteDir(), "lib");
				libFiles.addAll(getLibFiles(siteLibDir));
				cacheLibFiles(siteLibDir);
				libFiles.addAll(getLibFiles(libCacheDir));

				List<URL> urls = new ArrayList<URL>();

				// load our jars first so that we can override classes in third party
				// jars if necessary.
				for (File file : libFiles) {
					if (isPriorityLib(file)) {
						try {
							urls.add(file.toURI().toURL());
						} catch (MalformedURLException e) {
							throw new RuntimeException(e);
						}
					}
				}
				for (File file : libFiles) {
					if (!isPriorityLib(file)) {
						try {
							urls.add(file.toURI().toURL());
						} catch (MalformedURLException e) {
							throw new RuntimeException(e);
						}
					}
				}

				ClassLoader appClassLoader = new URLClassLoader(urls.toArray(new URL[0]), 
						Bootstrap.class.getClassLoader());
				Thread.currentThread().setContextClassLoader(appClassLoader);
				
				String appLoaderClassName = System.getProperty(APP_LOADER_PROPERTY_NAME);
				if (appLoaderClassName == null)
					appLoaderClassName = DEFAULT_APP_LOADER;

				Startable appLoader;
				try {
					Class<?> appLoaderClass = appClassLoader.loadClass(appLoaderClassName);
					appLoader = (Startable) appLoaderClass.newInstance();
					appLoader.start();
				} catch (Exception e) {
					throw BootstrapUtils.unchecked(e);
				}

				Runtime.getRuntime().addShutdownHook(new Thread() {
					public void run() {
						try {
							appLoader.stop();
						} catch (Exception e) {
							throw BootstrapUtils.unchecked(e);
						}
					}
				});
			} catch (Exception e) {
				logger.error("Error booting application", e);
				System.exit(1);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private static boolean isPriorityLib(File lib) {
		String entryName = "META-INF/onedev-artifact.properties";
		if (lib.isDirectory()) {
			return new File(lib, entryName).exists();
		} else {
			try (JarFile jarFile = new JarFile(lib)) {
				return jarFile.getJarEntry(entryName) != null;
			} catch (IOException e) {
				throw new RuntimeException(lib.getAbsolutePath() + e);
			} 
		}
	}
	
    public static File createTempDir(String prefix) {
        File temp;

        try {
			temp = File.createTempFile(prefix, "");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

        if (!temp.delete())
            throw new RuntimeException("Could not delete temp file: " + temp.getAbsolutePath());

        if (!temp.mkdirs())
            throw new RuntimeException("Could not create temp directory: " + temp.getAbsolutePath());

        return temp;    
    }
    
    public static File createTempDir() {
    	return createTempDir("temp");
    }

	private static List<File> getLibFiles(File libDir) {
		List<File> libFiles = new ArrayList<>();
		for (File file : libDir.listFiles()) {
			if (file.getName().endsWith(".jar"))
				libFiles.add(file);
		}
		return libFiles;
	}
	
	private static void cacheLibFiles(File libDir) {
		for (File file: libDir.listFiles()) {
			if (file.getName().endsWith(".zip"))
				BootstrapUtils.unzip(file, libCacheDir);
		}
	}
	
	private static void configureLogging() {
		// Set system properties so that they can be used in logback
		// configuration file.
		if (command != null) {
			System.setProperty("logback.logFile", installDir.getAbsolutePath() + "/logs/" + command.getName() + ".log");
			System.setProperty("logback.fileLogPattern", "%-5level - %msg%n");			
			System.setProperty("logback.consoleLogPattern", "%-5level - %msg%n");
		} else {
			System.setProperty("logback.logFile", installDir.getAbsolutePath() + "/logs/server.log");
			System.setProperty("logback.consoleLogPattern", "%d{HH:mm:ss} %-5level %logger{36} - %msg%n");			
			System.setProperty("logback.fileLogPattern", "%date %-5level [%thread] %logger{36} %msg%n");
		}

		File configFile = new File(installDir, "conf/logback.xml");
		System.setProperty(LOGBACK_CONFIG_FILE_PROPERTY_NAME, configFile.getAbsolutePath());

		LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

		try {
			JoranConfigurator configurator = new JoranConfigurator();
			configurator.setContext(lc);
			lc.reset();
			configurator.doConfigure(configFile);
		} catch (JoranException je) {
			je.printStackTrace();
		}
		StatusPrinter.printInCaseOfErrorsOrWarnings(lc);

		// Redirect JDK logging to slf4j
		java.util.logging.Logger jdkLogger = java.util.logging.Logger.getLogger("");
		for (Handler handler : jdkLogger.getHandlers())
			jdkLogger.removeHandler(handler);
		SLF4JBridgeHandler.install();
	}

	public static File getBinDir() {
		return new File(installDir, "bin");
	}
	
	public static File getBootDir() {
		return new File(installDir, "boot");
	}
	
	public static File getStatusDir() {
		return new File(installDir, "status");
	}
	
	public static boolean isServerRunning(File installDir) {
		// status directory may contain multiple pid files, for instance, 
		// appname.pid, appname_backup.pid, etc. We only check for appname.pid
		// here and assumes that appname does not contain underscore
		for (File file: new File(installDir, "status").listFiles()) {
			if (file.getName().endsWith(".pid") && !file.getName().contains("_"))
				return true;
		}
		return false;
	}

	public static File getLibDir() {
		return new File(installDir, "lib");
	}
	
	public static File getTempDir() {
		if (command != null) 
			return new File(installDir, "temp/" + command.getName());
		else
			return new File(installDir, "temp/server");
	}
	
	public static File getConfDir() {
		return new File(installDir, "conf");
	}
	
	public static File getSiteDir() {
		return new File(installDir, "site");
	}
	
	public static boolean isInDocker() {
		return new File(installDir, "IN_DOCKER").exists();
	}
	
}
