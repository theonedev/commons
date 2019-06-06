package io.onedev.commons.utils.license;

import java.io.File;
import java.io.Serializable;
import java.util.Date;
import java.util.Properties;

import com.verhas.licensor.License;

public class LicenseDetail implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final int FREE_LICENSE_USERS = 10;
	
	public static final int ABOUT_TO_EXPIRE_DAYS = 30;
	
	public enum FieldNames {ISSUE_ACCOUNT, ISSUE_DATE, EXPIRATION_DATE, LICENSED_USERS};
	
	private final String issueAccount;
	
	private final Date issueDate;
	
	private final Date expirationDate;
	
	private final int licensedUsers;
	
	public LicenseDetail(String issueAccount, Date issueDate, Date expirationDate, int licensedUsers) {
		this.issueAccount = issueAccount;
		this.issueDate = issueDate;
		this.expirationDate = expirationDate;
		this.licensedUsers = licensedUsers;
	}

	public String getIssueAccount() {
		return issueAccount;
	}

	public Date getIssueDate() {
		return issueDate;
	}

	public Date getExpirationDate() {
		return expirationDate;
	}
	
	public int getLicensedUsers() {
		return licensedUsers;
	}

	public int getRemainingDays() {
		return (int) ((getExpirationDate().getTime() - new Date().getTime())/1000/3600/24);
	}
	
	public double getRemainingCredits() {
		Date now = new Date();
		Date expirationDate = getExpirationDate();
		long remainingMillis = expirationDate.getTime() - now.getTime();
		if (remainingMillis <= 0) {
			return 0;
		} else {
			return remainingMillis/(1000.0*3600*24*365) * licensedUsers;
		}
	}

	public static LicenseDetail decode(String licenseKey) {
		License license = LicenseUtils.decode(licenseKey);
		if (license != null) {
			String accountUUID = license.getFeature(FieldNames.ISSUE_ACCOUNT.name());
			Date issueDate = new Date(Long.parseLong(license.getFeature(FieldNames.ISSUE_DATE.name())));
			Date expirationDate = new Date(Long.parseLong(license.getFeature(FieldNames.EXPIRATION_DATE.name())));
			int licensedUsers = Integer.parseInt(license.getFeature(FieldNames.LICENSED_USERS.name()));
			return new LicenseDetail(accountUUID, issueDate, expirationDate, licensedUsers);
		} else {
			return null;
		}
	}
	
	public String encode(File secringFile) {
		Properties licenseProps = new Properties();
		licenseProps.setProperty(FieldNames.ISSUE_ACCOUNT.name(), getIssueAccount());
		licenseProps.setProperty(FieldNames.ISSUE_DATE.name(), String.valueOf(getIssueDate().getTime()));
		licenseProps.setProperty(FieldNames.EXPIRATION_DATE.name(), String.valueOf(getExpirationDate().getTime()));
		licenseProps.setProperty(FieldNames.LICENSED_USERS.name(), String.valueOf(getLicensedUsers()));
		return LicenseUtils.encode(secringFile, licenseProps);
	}
	
}
