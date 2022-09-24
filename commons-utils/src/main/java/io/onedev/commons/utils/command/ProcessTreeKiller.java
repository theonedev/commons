package io.onedev.commons.utils.command;

import java.util.HashMap;
import java.util.Map;

public class ProcessTreeKiller implements ProcessKiller {

	@Override
	public void kill(Process process, String executionId) {
		Map<String, String> envs = new HashMap<>();
		envs.put(Commandline.EXECUTION_ID_ENV, executionId);
		ProcessTree.get().killAll(process, envs);
	}

}
