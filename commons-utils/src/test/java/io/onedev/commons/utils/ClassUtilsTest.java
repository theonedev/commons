package io.onedev.commons.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ClassUtilsTest {
	
	@Test
	public void testGetResourceAsStream() {
		String packageName = ClassUtils.class.getPackage().getName();
		Assertions.assertNotNull(ClassUtils.getResourceAsStream(
				null, packageName.replace('.', '/') + "/ClassUtils.class"));
		Assertions.assertNotNull(ClassUtils.getResourceAsStream(
				ClassUtils.class, "ClassUtils.class"));
	}
	
}
