package io.onedev.commons.utils.command;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pty4j.PtyProcessBuilder;

import io.onedev.commons.utils.StringUtils;

public class Commandline implements Serializable {
	
	private static final long serialVersionUID = 1L;

	public static final String EXECUTION_ID_ENV = "ONEDEV_COMMAND_EXECUTION_UUID";
	
    static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();
    
	private static final Logger logger = LoggerFactory.getLogger(Commandline.class);
	
    private String executable;
    
    private List<String> arguments = new ArrayList<String>();
    
    private File workingDir;
    
    private long timeout; // timeout in seconds
    
    private Map<String, String> environments = new HashMap<String, String>();
    
    public Commandline(String executable) {
        this.executable = executable.replace('/', File.separatorChar).replace('\\', File.separatorChar);
    }
    
    public Commandline arguments(List<String> arguments) {
    	this.arguments.clear();
    	this.arguments.addAll(arguments);
    	return this;
    }
    
    public Commandline addArgs(String... args) {
    	for (String each: args)
    		arguments.add(each);
    	return this;
    }
    
    public Commandline workingDir(File workingDir) {
    	this.workingDir = workingDir;
    	return this;
    }
    
    public File workingDir() {
    	return workingDir;
    }
    
    public Commandline timeout(long timeout) {
    	this.timeout = timeout;
    	return this;
    }
    
    public long timeout() {
    	return timeout;
    }
    
    public String executable() {
    	return executable;
    }
    
    public List<String> arguments() {
    	return arguments;
    }
    
    public Map<String, String> environments() {
    	return environments;
    }
    
    public Commandline environments(Map<String, String> environments) {
    	this.environments.clear();
    	this.environments.putAll(environments);
    	return this;
    }

    @Override
    public String toString() {
    	List<String> command = new ArrayList<String>();
    	command.add(executable);
    	command.addAll(arguments);

    	StringBuffer buf = new StringBuffer();
        for (String each: command) {
        	if (each.contains(" ") || each.contains("\t")) {
        		buf.append("\"").append(StringUtils.replace(
        				each, "\n", "\\n")).append("\"").append(" ");
        	} else {
        		buf.append(StringUtils.replace(
        				each, "\n", "\\n")).append(" ");
        	}
        }
        return buf.toString().trim();
    }

    public Commandline clearArgs() {
        arguments.clear();
        return this;
    }
    
	private PtyProcessBuilder createProcessBuilder() {
		File workingDir = this.workingDir;
		if (workingDir == null)
			workingDir = new File(".");
		
		String executable = this.executable;
		
        if (!new File(executable).isAbsolute()) {
            if (new File(workingDir, executable).isFile())
            	executable = new File(workingDir, executable).getAbsolutePath();
            else if (new File(workingDir, executable + ".exe").isFile())
            	executable = new File(workingDir, executable + ".exe").getAbsolutePath();
            else if (new File(workingDir, executable + ".bat").isFile())
            	executable = new File(workingDir, executable + ".bat").getAbsolutePath();
            else if (new File(workingDir, executable + ".cmd").isFile())
            	executable = new File(workingDir, executable + ".cmd").getAbsolutePath();
        }

		List<String> command = new ArrayList<String>();
		command.add(executable);
		command.addAll(arguments);
		
        PtyProcessBuilder processBuilder = new PtyProcessBuilder(command.toArray(new String[command.size()]));
        processBuilder.setDirectory(workingDir.getAbsolutePath());
        
        processBuilder.setEnvironment(environments);
        
        if (logger.isDebugEnabled()) {
    		logger.debug("Executing command: " + this);
    		if (logger.isTraceEnabled()) {
        		logger.trace("Command working directory: " + workingDir.getAbsolutePath());
        		StringBuffer buffer = new StringBuffer();
        		for (Map.Entry<String, String> entry: environments.entrySet())
        			buffer.append("	" + entry.getKey() + "=" + entry.getValue() + "\n");
        		logger.trace("Command execution environments:\n" + 
        				StringUtils.stripEnd(buffer.toString(), "\n"));
    		}
    	}

    	return processBuilder;
    }
    
	public ExecutionResult execute(@Nullable OutputStream stdout, @Nullable LineConsumer stderr) {
		return execute(stdout, stderr, null);
	}
	
	public ExecutionResult execute(@Nullable OutputStream stdout, @Nullable LineConsumer stderr, 
			@Nullable OutputStreamHandler inputHandler) {
		return execute(stdout, stderr, inputHandler, new ProcessKiller() {
			
			@Override
			public void kill(Process process, String executionId) {
				Map<String, String> envs = new HashMap<>();
				envs.put(EXECUTION_ID_ENV, executionId);
				ProcessTree.get().killAll(process, envs);
			}
			
		});
	}
	
	public ExecutionResult execute(@Nullable OutputStream stdout, @Nullable OutputStream stderr, 
			@Nullable OutputStreamHandler inputHandler) {
		return execute(stdout, stderr, inputHandler, new ProcessKiller() {
			
			@Override
			public void kill(Process process, String executionId) {
				Map<String, String> envs = new HashMap<>();
				envs.put(EXECUTION_ID_ENV, executionId);
				ProcessTree.get().killAll(process, envs);
			}
			
		});
	}
	
	/**
	 * Execute the command.
	 * 
	 * @param stdout
	 * 			output stream to write standard output, caller is responsible for closing the stream
	 * @param stderr
	 * 			line consumer to handle standard error
	 * @param stdin
	 * 			input stream to read standard input from, caller is responsible for closing the stream
	 * @return
	 * 			execution result
	 */
	public ExecutionResult execute(@Nullable OutputStream stdout, @Nullable LineConsumer stderr, 
			@Nullable OutputStreamHandler inputHandler, ProcessKiller processKiller) {
		if (stderr != null) {
			ErrorCollector errorCollector = new ErrorCollector(stderr.getEncoding()) {

				@Override
				public void consume(String line) {
					super.consume(line);
					stderr.consume(line);
				}
				
			};
			ExecutionResult result = execute(stdout, (OutputStream)errorCollector, inputHandler, processKiller);
			result.setStderr(errorCollector.getMessage());
	        return result;
		} else {
			return execute(stdout, (OutputStream)null, inputHandler, processKiller);
		}
    }
    
	public ExecutionResult execute(@Nullable OutputStream stdout, @Nullable OutputStream stderr, 
			@Nullable OutputStreamHandler inputHandler, ProcessKiller processKiller) {
    	String executionId = UUID.randomUUID().toString();
    	
    	Process process;
        try {
        	environments.put(EXECUTION_ID_ENV, executionId);
        	PtyProcessBuilder processBuilder = createProcessBuilder();
        	process = processBuilder.start();
        } catch (IOException e) {
        	throw new RuntimeException(e);
        }

		ExecutionResult result = new ExecutionResult(this);
        if (timeout != 0) {
            AtomicLong lastActiveTime = new AtomicLong(System.currentTimeMillis());
            
            class OutputStreamWrapper extends OutputStream {
            	
            	private final OutputStream delegate;
            	
            	public OutputStreamWrapper(OutputStream delegate) {
            		this.delegate = delegate;
            	}
            	
				@Override
				public void flush() throws IOException {
					if (delegate != null)
						delegate.flush();
				}

				@Override
				public void close() throws IOException {
					if (delegate != null)
						delegate.close();
				}

				@Override
				public void write(int b) throws IOException {
					lastActiveTime.set(System.currentTimeMillis());
					if (delegate != null)
						delegate.write(b);
				}

				@Override
				public void write(byte[] b) throws IOException {
					lastActiveTime.set(System.currentTimeMillis());
					if (delegate != null)
						delegate.write(b);
				}

				@Override
				public void write(byte[] b, int off, int len) throws IOException {
					lastActiveTime.set(System.currentTimeMillis());
					if (delegate != null)
						delegate.write(b, off, len);
				}
            	
            };
            
            StreamPumper stdoutPumper = new StreamPumper(process.getInputStream(), new OutputStreamWrapper(stdout));
            StreamPumper stderrPumper = new StreamPumper(process.getErrorStream(), new OutputStreamWrapper(stderr));
            
            if (inputHandler != null) {
                inputHandler.handle(process.getOutputStream());
            } else {
            	try {
    				process.getOutputStream().close();
    			} catch (IOException e) {
    			}
            }
            
        	Thread thread = Thread.currentThread();
    		AtomicBoolean stoppedRef = new AtomicBoolean(false);
    		EXECUTOR_SERVICE.execute(new Runnable() {

				@Override
				public void run() {
					while (!stoppedRef.get()) {
						if (System.currentTimeMillis() - lastActiveTime.get() > timeout*1000L) {
							thread.interrupt();
							break;
						} else {
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
							}
						}
					}
				}
    			
    		});
            try {
            	result.setReturnCode(process.waitFor());
    		} catch (InterruptedException e) {
    			processKiller.kill(process, executionId);
    			if (System.currentTimeMillis() - lastActiveTime.get() > timeout*1000L)
    				throw new RuntimeException(new TimeoutException());
    			else
    				throw new RuntimeException(e);
    		} finally {
    			stoppedRef.set(true);
    			stdoutPumper.waitFor();
    			stderrPumper.waitFor();
    			if (inputHandler != null)
    				inputHandler.waitFor();
    		}
        } else {
            StreamPumper stdoutPumper = new StreamPumper(process.getInputStream(), stdout);
            StreamPumper stderrPumper = new StreamPumper(process.getErrorStream(), stderr);
            
            if (inputHandler != null) {
                inputHandler.handle(process.getOutputStream());
            } else {
            	try {
    				process.getOutputStream().close();
    			} catch (IOException e) {
    			}
            }
        	
            try {
            	result.setReturnCode(process.waitFor());
    		} catch (InterruptedException e) {
    			processKiller.kill(process, executionId);
    			throw new RuntimeException(e);
    		} finally {
    			stdoutPumper.waitFor();
    			stderrPumper.waitFor();
    			if (inputHandler != null)
    				inputHandler.waitFor();
    		}
        }
        return result;
    }	
}
