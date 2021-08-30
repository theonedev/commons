package io.onedev.commons.bootstrap;

public class Command {

	private final String name;
	
	private final String[] args;
	
	public Command(String name, String[] args) {
		this.name = name;
		this.args = args;
	}

	public String getName() {
		return name;
	}

	public String[] getArgs() {
		return args;
	}
	
	public String getScript() {
		return getScript(name);
	}
	
	public static String getScript(String commandName) {
		String osName = System.getProperty("os.name");
		if (osName != null && osName.startsWith("Windows"))		
			return commandName + ".bat";
		else
			return commandName + ".sh";
	}
	
}
