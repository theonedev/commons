package io.onedev.commons.utils.license;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.util.Enumeration;
import java.util.Properties;

import javax.annotation.Nullable;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.util.encoders.DecoderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.verhas.licensor.License;

import io.onedev.commons.utils.StringUtils;

public class LicenseUtils {
	
	private static final Logger logger = LoggerFactory.getLogger(LicenseUtils.class);
	
	private static final int KEY_WIDTH = 60;
	
	private static final byte[] PUBRING_DIGEST = new byte[] {
			(byte)0x57, 
			(byte)0xC7, (byte)0x7E, (byte)0xD5, (byte)0xE2, (byte)0x68, (byte)0x83, (byte)0xEA, (byte)0x6A, 
			(byte)0xAA, (byte)0xBD, (byte)0x14, (byte)0xD4, (byte)0x4C, (byte)0x30, (byte)0xDA, (byte)0x34, 
			(byte)0x69, (byte)0x84, (byte)0xA3, (byte)0x7F, (byte)0xE4, (byte)0xC4, (byte)0xF8, (byte)0x30, 
			(byte)0x56, (byte)0x09, (byte)0x9D, (byte)0x4A, (byte)0x1E, (byte)0x18, (byte)0xFB, (byte)0x0F, 
			(byte)0xDE, (byte)0xFC, (byte)0x01, (byte)0x90, (byte)0x65, (byte)0xA3, (byte)0x18, (byte)0xAB, 
			(byte)0xE2, (byte)0xB7, (byte)0x29, (byte)0xB1, (byte)0x2F, (byte)0xDA, (byte)0x21, (byte)0x90, 
			(byte)0x51, (byte)0x2E, (byte)0x43, (byte)0x49, (byte)0x4D, (byte)0x4F, (byte)0x43, (byte)0xAA, 
			(byte)0x55, (byte)0x1D, (byte)0xE4, (byte)0x17, (byte)0x99, (byte)0x56, (byte)0x57, 
			};
	
	private static String getPubringResource() {
		return LicenseUtils.class.getPackage().getName().replace(".", "/") + "/pubring.gpg";
	}
	
	@Nullable
	public static License decode(String licenseKey) {
		License license = new License();
		String pubringResource = getPubringResource();
		try {
			license.loadKeyRingFromResource(pubringResource, PUBRING_DIGEST);
			license.setLicenseEncoded(licenseKey);
			if (license.isVerified()) {
				return license;
			} else {
				return null;
			}
		} catch (IOException | PGPException | DecoderException e) {
			logger.error("Error decoding license key", e);
			return null;
		}
	}
	
	public static String encode(File secringFile, Properties licenseProps) {
		License license = new License();
		try {
			StringBuilder builder = new StringBuilder();
			Enumeration<?> propNames = licenseProps.propertyNames();
			while (propNames.hasMoreElements()) {
				Object propName = propNames.nextElement();
				builder.append(propName).append("=").append(licenseProps.get(propName)).append("\n");
			}
			license.setLicense(builder.toString());
			license.loadKey(secringFile, "OneDev");
			String encoded = license.encodeLicense("").trim();
			String eol = System.lineSeparator();
			encoded = StringUtils.substringAfter(encoded, eol+eol).trim();
			encoded = StringUtils.substringBeforeLast(encoded, eol).trim();
			encoded = StringUtils.deleteWhitespace(encoded);
			StringBuilder formatted = new StringBuilder();
			for (int i=0; i<encoded.length()/KEY_WIDTH; i++) {
				formatted.append(encoded.substring(i*KEY_WIDTH, (i+1)*KEY_WIDTH)).append(eol);
			}
			if (encoded.length() % KEY_WIDTH != 0)
				formatted.append(encoded.substring(encoded.length() - encoded.length()%KEY_WIDTH));
			return formatted.toString().trim();
		} catch (NoSuchAlgorithmException | NoSuchProviderException | SignatureException | IOException
				| PGPException e) {
			throw new RuntimeException(e);
		}
	}

}
