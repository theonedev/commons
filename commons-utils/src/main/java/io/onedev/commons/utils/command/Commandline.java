package io.onedev.commons.utils.command;

import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import com.pty4j.WinSize;
import io.onedev.commons.bootstrap.Bootstrap;
import io.onedev.commons.bootstrap.SensitiveMasker;
import io.onedev.commons.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

public class Commandline implements Serializable {
	
	private static final long serialVersionUID = 1L;

	public static final String EXECUTION_ID_ENV = "ONEDEV_COMMAND_EXECUTION_UUID";
	
	private static final Logger logger = LoggerFactory.getLogger(Commandline.class);
	
    private String executable;
    
    private List<String> arguments = new ArrayList<String>();
    
    private File workingDir;
    
    private long timeout; // timeout in seconds

    private PtyMode ptyMode;

	private ProcessKiller processKiller = new ProcessTreeKiller();
    
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
    
    public Commandline ptyMode(@Nullable PtyMode ptyMode) {
    	this.ptyMode = ptyMode;
    	return this;
    }
    
    @Nullable
    public PtyMode ptyMode() {
    	return ptyMode;
    }

	public Commandline processKiller(ProcessKiller processKiller) {
		this.processKiller = processKiller;
		return this;
	}

	public ProcessKiller processKiller() {
		return processKiller;
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
    	this.environments = new HashMap<>(environments);
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
        
        SensitiveMasker masker = SensitiveMasker.get();
        if (masker != null)
        	return masker.mask(buf.toString().trim());
        else
        	return buf.toString().trim();
    }

    public Commandline clearArgs() {
        arguments.clear();
        return this;
    }
    
    private File getEffectiveWorkingDir() {
		File workingDir = this.workingDir;
		if (workingDir == null)
			workingDir = new File(".");
		return workingDir;
    }
    
    private String getEffectiveExecutable(File workingDir) {
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
        return executable;
    }
    
	private ProcessBuilder createProcessBuilder() {
		File workingDir = getEffectiveWorkingDir();
		String executable = getEffectiveExecutable(workingDir);
		
		List<String> command = new ArrayList<String>();
		command.add(executable);
		command.addAll(arguments);
		
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(workingDir);
        
        processBuilder.environment().putAll(environments);
        
        if (logger.isDebugEnabled()) {
    		logger.debug("Executing command: " + this);
    		if (logger.isTraceEnabled()) {
        		logger.trace("Command working directory: " + workingDir.getAbsolutePath());
        		StringBuffer buffer = new StringBuffer();
        		for (Map.Entry<String, String> entry: processBuilder.environment().entrySet())
        			buffer.append("	" + entry.getKey() + "=" + entry.getValue() + "\n");
        		logger.trace("Command execution environments:\n" + 
        				StringUtils.stripEnd(buffer.toString(), "\n"));
    		}
    	}

    	return processBuilder;
    }
	
	private PtyProcessBuilder createPtyProcessBuilder() {
		File workingDir = getEffectiveWorkingDir();
		String executable = getEffectiveExecutable(workingDir);

		List<String> command = new ArrayList<String>();
		command.add(executable);
		command.addAll(arguments);
		
        PtyProcessBuilder processBuilder = new PtyProcessBuilder(command.toArray(new String[command.size()]));
        processBuilder.setDirectory(workingDir.getAbsolutePath());
        
        Map<String, String> ptyEnvironments = new HashMap<>(environments);
        ptyEnvironments.putAll(System.getenv());
        processBuilder.setEnvironment(ptyEnvironments);
        
        if (logger.isDebugEnabled()) {
    		logger.debug("Executing command: " + this);
    		if (logger.isTraceEnabled()) {
        		logger.trace("Command working directory: " + workingDir.getAbsolutePath());
        		StringBuffer buffer = new StringBuffer();
        		for (Map.Entry<String, String> entry: ptyEnvironments.entrySet())
        			buffer.append("	" + entry.getKey() + "=" + entry.getValue() + "\n");
        		logger.trace("Command execution environments:\n" + 
        				StringUtils.stripEnd(buffer.toString(), "\n"));
    		}
    	}

    	return processBuilder;
    }

	/**
	 * Various streams passed in will be closed after calling this method
	 *
	 * @param stdout
	 * @param stderr
	 * @return
	 */
	public ExecutionResult execute(@Nullable OutputStream stdout, @Nullable OutputStream stderr) {
		return execute(stdout, stderr, null);
	}

	/**
	 * Various stream passed in will be closed after executing this method
	 * @param stdout
	 * @param stderr
	 * @param stdin
	 * @return
	 */
	public ExecutionResult execute(@Nullable OutputStream stdout, @Nullable OutputStream stderr,
								   @Nullable InputStream stdin) {
		return execute(StreamPumper.pumpTo(stdout), StreamPumper.pumpTo(stderr), StreamPumper.pumpFrom(stdin));
	}

	/**
	 * Various handlers should close passed-in streams after dealing with it
	 *
	 * @param stdoutHandler
	 * @param stderrHandler
	 * @param stdinHandler
	 * @return
	 */
	public ExecutionResult execute(@Nullable Function<InputStream, Future<?>> stdoutHandler,
								   @Nullable Function<InputStream, Future<?>> stderrHandler,
								   @Nullable Function<OutputStream, Future<?>> stdinHandler) {
		if (stdoutHandler == null)
			stdoutHandler = StreamPumper.pumpTo(null);
		if (stderrHandler == null)
			stderrHandler = StreamPumper.pumpTo(null);
		if (stdinHandler == null)
			stdinHandler = StreamPumper.pumpFrom(null);
    	String executionId = UUID.randomUUID().toString();
    	Process process;
        try {
        	environments.put(EXECUTION_ID_ENV, executionId);
        	if (ptyMode != null) {
        		PtyProcess ptyProcess = createPtyProcessBuilder().start();
        		ptyMode.setResizeSupport((rows, cols) -> {
					try {
						ptyProcess.setWinSize(new WinSize(cols, rows));
					} catch (Exception e) {
						logger.error("Error setting window size", e);
					}
				});
        		process = ptyProcess;
        	} else {
        		process = createProcessBuilder().start();
        	}
        } catch (IOException e) {
        	throw new RuntimeException(e);
        }

		ExecutionResult result = new ExecutionResult(this);
		if (timeout != 0) {
			AtomicBoolean timedout = new AtomicBoolean(false);
			AtomicLong lastActiveTime = new AtomicLong(System.currentTimeMillis());

			class InputStreamWrapper extends InputStream {

				private final InputStream delegate;

				public InputStreamWrapper(InputStream delegate) {
					this.delegate = delegate;
				}

				@Override
				public int read() throws IOException {
					int readed = delegate.read();
					lastActiveTime.set(System.currentTimeMillis());
					return readed;
				}

				@Override
				public int read(byte[] b) throws IOException {
					int readed = delegate.read(b);
					lastActiveTime.set(System.currentTimeMillis());
					return readed;
				}

				@Override
				public int read(byte[] b, int off, int len) throws IOException {
					int readed = delegate.read(b, off, len);
					lastActiveTime.set(System.currentTimeMillis());
					return readed;
				}

				@Override
				public long skip(long n) throws IOException {
					return delegate.skip(n);
				}

				@Override
				public int available() throws IOException {
					return delegate.available();
				}

				@Override
				public void close() throws IOException {
					delegate.close();
				}

				@Override
				public synchronized void mark(int readlimit) {
					delegate.mark(readlimit);
				}

				@Override
				public synchronized void reset() throws IOException {
					delegate.reset();
				}

				@Override
				public boolean markSupported() {
					return delegate.markSupported();
				}

			}

			Thread thread = Thread.currentThread();
			AtomicBoolean stoppedRef = new AtomicBoolean(false);
			Bootstrap.executorService.execute(() -> {
				while (!stoppedRef.get()) {
					if (System.currentTimeMillis() - lastActiveTime.get() > timeout * 1000L) {
						timedout.set(true);
						thread.interrupt();
						break;
					} else {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
						}
					}
				}
			});

			var stdoutFuture = stdoutHandler.apply(new InputStreamWrapper(process.getInputStream()));
			var stderrFuture = stderrHandler.apply(new InputStreamWrapper(process.getErrorStream()));
			var stdinFuture = stdinHandler.apply(process.getOutputStream());
			try {
				result.setReturnCode(process.waitFor());
			} catch (InterruptedException e) {
				processKiller.kill(process, executionId);
				if (timedout.get())
					throw new RuntimeException(new TimeoutException());
				else
					throw new RuntimeException(e);
			} finally {
				stoppedRef.set(true);
				waitFor(stdoutFuture, stderrFuture, stdinFuture);
			}
		} else {
			var stdoutFuture = stdoutHandler.apply(process.getInputStream());
			var stderrFuture = stderrHandler.apply(process.getErrorStream());
			var stdinFuture = stdinHandler.apply(process.getOutputStream());
			try {
				result.setReturnCode(process.waitFor());
			} catch (InterruptedException e) {
				processKiller.kill(process, executionId);
				throw new RuntimeException(e);
			} finally {
				waitFor(stdoutFuture, stderrFuture, stdinFuture);
			}
		}
        return result;
    }

	private void waitFor(Future<?>... futures) {
		for (var future: futures) {
			try {
				future.get();
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}
		}
	}

}
