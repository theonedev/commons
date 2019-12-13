package io.onedev.commons.jsyntax;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import com.google.common.io.Resources;

public class CodeMirrorResource {

	public static String readAsString(String resourcePath) {
		try {
			byte[] bytes = Resources.toByteArray(Resources.getResource("META-INF/maven/org.webjars.npm/codemirror/pom.properties"));
			Properties cmProps = new Properties();
			cmProps.load(new ByteArrayInputStream(bytes));
			String cmResPrefix = "META-INF/resources/webjars/codemirror/" + cmProps.getProperty("version") + "/";
			return Resources.toString(Resources.getResource(cmResPrefix + resourcePath), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
}
