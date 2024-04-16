package io.onedev.commons.utils.command;

import java.io.Serializable;

import io.onedev.commons.utils.ExplicitException;

public class ExecutionResult implements Serializable {

	private static final long serialVersionUID = 1L;

	private int returnCode;
	
	private String errorMessage;
	
	private String commandDescription;

	public ExecutionResult(Commandline cmdline) {
		commandDescription = cmdline.toString();
	}
	
	public String getCommandDescription() {
		return commandDescription;
	}

	public void setCommandDescription(String commandDescription) {
		this.commandDescription = commandDescription;
	}

	public int getReturnCode() {
		return returnCode;
	}

	public void setReturnCode(int returnCode) {
		this.returnCode = returnCode;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
	
	/**
	 * Build an exception object with command description, stderr output and return code.
	 * @return
	 */
	public RuntimeException buildException() {
        String exceptionMessage = String.format("Failed to run command: %s, return code: %d",
        		commandDescription, returnCode);
    	if (errorMessage != null)
    		exceptionMessage += "\n" + errorMessage;
    	return new ExplicitException(exceptionMessage);
	}
	
	/**
	 * Check return code and throw exception if it does not equal to 0.
	 */
	public void checkReturnCode() {
		if (getReturnCode() != 0)
			throw buildException();
	}
}