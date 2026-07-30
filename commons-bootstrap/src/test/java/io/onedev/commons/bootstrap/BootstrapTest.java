package io.onedev.commons.bootstrap;

import static io.onedev.commons.bootstrap.Bootstrap.convertCidrToWildcards;
import static io.onedev.commons.bootstrap.Bootstrap.convertCidrsInNonProxyHosts;
import static io.onedev.commons.bootstrap.Bootstrap.convertNoProxyToNonProxyHosts;
import static io.onedev.commons.bootstrap.Bootstrap.normalizeNonProxyHost;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class BootstrapTest {

	@Test
	public void shouldConvertCidrAlignedToOctetBoundary() {
		assertEquals(List.of("10.*"), convertCidrToWildcards("10.0.0.0/8"));
		assertEquals(List.of("192.168.*"), convertCidrToWildcards("192.168.0.0/16"));
		assertEquals(List.of("192.168.0.*"), convertCidrToWildcards("192.168.0.0/24"));
		assertEquals(List.of("192.168.1.1"), convertCidrToWildcards("192.168.1.1/32"));

		// zero octets inside the prefix are significant and must be kept
		assertEquals(List.of("10.0.*"), convertCidrToWildcards("10.0.0.0/16"));
		assertEquals(List.of("10.0.0.*"), convertCidrToWildcards("10.0.0.0/24"));
		assertEquals(List.of("10.0.0.0"), convertCidrToWildcards("10.0.0.0/32"));

		// bits beyond the prefix are insignificant and must be dropped
		assertEquals(List.of("192.168.*"), convertCidrToWildcards("192.168.1.5/16"));

		assertEquals(List.of("*"), convertCidrToWildcards("0.0.0.0/0"));
		assertEquals(List.of("192.168.7.*"), convertCidrToWildcards("192.168.007.0/24"));
	}

	@Test
	public void shouldExpandCidrNotAlignedToOctetBoundary() {
		var expected = new ArrayList<String>();
		for (var i = 16; i <= 31; i++)
			expected.add("172." + i + ".*");
		assertEquals(expected, convertCidrToWildcards("172.16.0.0/12"));
		assertEquals(expected, convertCidrToWildcards("172.20.30.40/12"));

		expected = new ArrayList<>();
		for (var i = 192; i <= 255; i++)
			expected.add("10." + i + ".*");
		assertEquals(expected, convertCidrToWildcards("10.192.0.0/10"));

		assertEquals(List.of("192.168.1.0", "192.168.1.1"), convertCidrToWildcards("192.168.1.0/31"));

		var wildcards = convertCidrToWildcards("10.0.0.0/9");
		assertEquals(128, wildcards.size());
		assertEquals("10.0.*", wildcards.get(0));
		assertEquals("10.127.*", wildcards.get(127));

		wildcards = convertCidrToWildcards("240.0.0.0/4");
		assertEquals(16, wildcards.size());
		assertEquals("240.*", wildcards.get(0));
		assertEquals("255.*", wildcards.get(15));
	}

	@Test
	public void shouldNotConvertNonCidr() {
		assertNull(convertCidrToWildcards("example.com"));
		assertNull(convertCidrToWildcards("*.example.com"));
		assertNull(convertCidrToWildcards("10.0.0.0"));
		assertNull(convertCidrToWildcards("10.0.0/8"));
		assertNull(convertCidrToWildcards("10.0.0.0/8/16"));
		assertNull(convertCidrToWildcards("10.0.0.0/"));
		assertNull(convertCidrToWildcards("10.0.0.0/abc"));
		assertNull(convertCidrToWildcards("10.0.0.0/33"));
		assertNull(convertCidrToWildcards("10.0.0.0/-1"));
		assertNull(convertCidrToWildcards("10.0.0.256/8"));
		assertNull(convertCidrToWildcards("fd00::/8"));
		assertNull(convertCidrToWildcards("::1/128"));
	}

	@Test
	public void shouldNormalizeNonProxyHost() {
		assertEquals("localhost", normalizeNonProxyHost(" localhost "));
		assertEquals("*.example.com", normalizeNonProxyHost(".example.com"));
		assertEquals("example.com", normalizeNonProxyHost("example.com:8443"));
		assertEquals("*.example.com", normalizeNonProxyHost(" .example.com:443 "));
		assertEquals("10.0.0.0/24", normalizeNonProxyHost("10.0.0.0/24"));
		assertEquals("*", normalizeNonProxyHost("*"));

		// a port suffix must not be assumed for an ipv6 address
		assertEquals("::1", normalizeNonProxyHost("::1"));
		assertEquals("example.com:port", normalizeNonProxyHost("example.com:port"));

		assertNull(normalizeNonProxyHost(""));
		assertNull(normalizeNonProxyHost("   "));
		assertNull(normalizeNonProxyHost(":8443"));
	}

	@Test
	public void shouldConvertNoProxyToNonProxyHosts() {
		assertEquals("localhost|*.example.com|example.org|10.0.0.0/24|::1|*",
				convertNoProxyToNonProxyHosts("localhost, .example.com,example.org:8443 , 10.0.0.0/24,,::1,*"));
		assertNull(convertNoProxyToNonProxyHosts(""));
		assertNull(convertNoProxyToNonProxyHosts(" , "));
	}

	@Test
	public void shouldConvertCidrsInNonProxyHosts() {
		assertEquals("localhost|10.0.0.*|*.example.com|192.168.*",
				convertCidrsInNonProxyHosts("localhost|10.0.0.0/24||*.example.com| 192.168.0.0/16 "));
		assertEquals("172.16.*|172.17.*|172.18.*|172.19.*|172.20.*|172.21.*|172.22.*|172.23.*"
				+ "|172.24.*|172.25.*|172.26.*|172.27.*|172.28.*|172.29.*|172.30.*|172.31.*",
				convertCidrsInNonProxyHosts("172.16.0.0/12"));
		assertEquals("", convertCidrsInNonProxyHosts(""));
	}

}
