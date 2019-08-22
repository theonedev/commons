/*
 * The MIT License
 *
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.onedev.commons.utils.command;

import static com.sun.jna.Pointer.NULL;
import static io.onedev.commons.utils.command.GNUCLibrary.LIBC;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import org.jvnet.winp.WinProcess;
import org.jvnet.winp.WinpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.LastErrorException;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.ptr.IntByReference;

import io.onedev.commons.utils.command.ProcessTree.OSProcess;

/**
 * Adapted from Jenkins to kill process tree
 */
public abstract class ProcessTree implements Iterable<OSProcess> {
	
	private static final Logger logger = LoggerFactory.getLogger(ProcessTree.class);
	
    /**
     * To be filled in the constructor of the derived type.
     */
    protected final Map<Integer/*pid*/, OSProcess> processes = new HashMap<>();

    /**
     * Gets the process given a specific ID, or null if no such process exists.
     */
    public final OSProcess get(int pid) {
        return processes.get(pid);
    }

    /**
     * Lists all the processes in the system.
     */
    public final Iterator<OSProcess> iterator() {
        return processes.values().iterator();
    }

    /**
     * Try to convert {@link Process} into this process object
     * or null if it fails (for example, maybe the snapshot is taken after
     * this process has already finished.)
     */
    public abstract OSProcess get(Process proc);

    /**
     * Kills all the processes that have matching environment variables.
     *
     * <p>
     * In this method, the method is given a
     * "model environment variables", which is a list of environment variables
     * and their values that are characteristic to the launched process.
     * The implementation is expected to find processes
     * in the system that inherit these environment variables, and kill
     * them all. This is suitable for locating daemon processes
     * that cannot be tracked by the regular ancestor/descendant relationship.
     */
    public abstract void killAll(Map<String, String> modelEnvVars) throws InterruptedException;

    private final long softKillWaitSeconds = Integer.getInteger("SoftKillWaitSeconds", 30); 

    /**
     * Convenience method that does {@link #killAll(Map)} and {@link OSProcess#killRecursively()}.
     * This is necessary to reliably kill the process and its descendants, as some OS
     * may not implement {@link #killAll(Map)}.
     *
     * Either of the parameter can be null.
     */
    public void killAll(Process proc, Map<String, String> modelEnvVars) {
        logger.debug("killAll: process="+proc+" and envs="+modelEnvVars);
        OSProcess p = get(proc);
        try {
			if(p!=null) p.killRecursively();
			if(modelEnvVars!=null)
			    killAll(modelEnvVars);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
    }

    /**
     * Represents a process.
     */
    public abstract class OSProcess {
    	
        final int pid;

        // instantiation only allowed for subtypes in this class
        private OSProcess(int pid) {
            this.pid = pid;
        }

        public final int getPid() {
            return pid;
        }

        /**
         * Gets the parent process. This method may return null, because
         * there's no guarantee that we are getting a consistent snapshot
         * of the whole system state.
         */
        public abstract OSProcess getParent();

        final ProcessTree getTree() {
            return ProcessTree.this;
        }

        /**
         * Immediate child processes.
         */
        public final List<OSProcess> getChildren() {
            List<OSProcess> r = new ArrayList<OSProcess>();
            for (OSProcess p : ProcessTree.this)
                if(p.getParent()==this)
                    r.add(p);
            return r;
        }

        /**
         * Kills this process.
         */
        public abstract void kill() throws InterruptedException;

        /**
         * Kills this process and all the descendants.
         * <p>
         * Note that the notion of "descendants" is somewhat vague,
         * in the presence of such things like daemons. On platforms
         * where the recursive operation is not supported, this just kills
         * the current process.
         */
        public abstract void killRecursively() throws InterruptedException;

        /**
         * Obtains the environment variables of this process.
         *
         * @return
         *      empty map if failed (for example because the process is already dead,
         *      or the permission was denied.)
         */
        public abstract Map<String, String> getEnvironmentVariables();

        /**
         * Given the environment variable of a process and the "model environment variable" that Hudson
         * used for launching the build, returns true if there's a match (which means the process should
         * be considered a descendant of a build.)
         */
        public final boolean hasMatchingEnvVars(Map<String,String> modelEnvVar) {
            if(modelEnvVar.isEmpty())
                // sanity check so that we don't start rampage.
                return false;

            Map<String,String> envs = getEnvironmentVariables();
            for (Entry<String,String> e : modelEnvVar.entrySet()) {
                String v = envs.get(e.getKey());
                if(v==null || !v.equals(e.getValue()))
                    return false;   // no match
            }

            return true;
        }

    }

    /**
     * Gets the {@link ProcessTree} of the current system
     * that JVM runs in, or in the worst case return the default one
     * that's not capable of killing descendants at all.
     */
    public static ProcessTree get() {
        try {
            if(File.pathSeparatorChar==';')
                return new Windows();

            String os = System.getProperty("os.name");
            if("Linux".equals(os))
                return new Linux();
            if("AIX".equals(os))
                return new AIX();
            if("SunOS".equals(os))
                return new Solaris();
            if("Mac OS X".equals(os))
                return new Darwin();
        } catch (LinkageError e) {
            logger.warn("Failed to load winp. Reverting to the default",e);
        }

        return DEFAULT;
    }
    
    /**
     * Empty process list as a default value if the platform doesn't support it.
     */
    static final ProcessTree DEFAULT = new ProcessTree() {
    	
        public OSProcess get(final Process proc) {
            return new OSProcess(-1) {
                public OSProcess getParent() {
                    return null;
                }

                public void killRecursively() {
                    // fall back to a single process killer
                    proc.destroy();
                }

                public void kill() throws InterruptedException {
                    proc.destroy();
                }

                public Map<String, String> getEnvironmentVariables() {
                    return new HashMap<>();
                }
            };
        }

        public void killAll(Map<String, String> modelEnvVars) {
            // no-op
        }
    };

    private class WindowsOSProcess extends OSProcess {
        
        private final WinProcess p;
        
        private Map<String, String> env;
        
        WindowsOSProcess(WinProcess p) {
            super(p.getPid());
            this.p = p;
        }

        @Override
        public OSProcess getParent() {
            // Windows process doesn't have parent/child relationship
            return null;
        }

        @Override
        public void killRecursively() throws InterruptedException {
            logger.debug("Killing recursively {}", getPid());
            // killSoftly();
            p.killRecursively();
        }

        @Override
        public void kill() throws InterruptedException {
            logger.debug("Killing {}", getPid());
            // killSoftly();
            p.kill();
        }

        /*
        private void killSoftly() throws InterruptedException {
            // send Ctrl+C to the process
            try {
                if (!p.sendCtrlC()) {
                    return;
                }
            }
            catch (WinpException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Failed to send CTRL+C to pid=" + getPid(), e);
                }
                return;
            }

            // after that wait for it to cease to exist
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(softKillWaitSeconds);
            int sleepTime = 10; // initially we sleep briefly, then sleep up to 1sec
            do {
                if (!p.isRunning()) {
                    break;
                }

                Thread.sleep(sleepTime);
                sleepTime = Math.min(sleepTime * 2, 1000);
            } while (System.nanoTime() < deadline);
        }
        */

        @Override
        public synchronized Map<String, String> getEnvironmentVariables() {
            try {
               return getEnvironmentVariables2();
            } catch (WindowsOSProcessException e) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Failed to get the environment variables of process with pid=" + p.getPid(), e);
                }
            }
            return null;
        }
        
        private synchronized Map<String, String> getEnvironmentVariables2() throws WindowsOSProcessException {
            if(env !=null) {
              return env;
            }
            env = new HashMap<>();

            try {
               env.putAll(p.getEnvironmentVariables());
            } catch (WinpException e) {
               throw new WindowsOSProcessException("Failed to get the environment variables", e);
            }
            return env;
        }
        
        private boolean hasMatchingEnvVars2(Map<String,String> modelEnvVar) throws WindowsOSProcessException {
            if(modelEnvVar.isEmpty())
                // sanity check so that we don't start rampage.
                return false;

            Map<String,String> envs = getEnvironmentVariables2();
            for (Entry<String,String> e : modelEnvVar.entrySet()) {
                String v = envs.get(e.getKey());
                if(v==null || !v.equals(e.getValue()))
                    return false;   // no match
            }

            return true;
        }
    }
    
    //TODO: Cleanup once Winp provides proper API 
    /**
     * Wrapper for runtime {@link WinpException}.
     */
    private static class WindowsOSProcessException extends Exception {
		private static final long serialVersionUID = 1L;

		WindowsOSProcessException(WinpException ex) {
            super(ex);
        }
        
        WindowsOSProcessException(String message, WinpException ex) {
            super(message, ex);
        }
    }

    private static final class Windows extends ProcessTree {
        Windows() {
            for (final WinProcess p : WinProcess.all()) {
                int pid = p.getPid();
                if(pid == 0 || pid == 4) continue; // skip the System Idle and System processes
                super.processes.put(pid, new WindowsOSProcess(p));
            }
        }

        @Override
        public OSProcess get(Process proc) {
            return get(new WinProcess(proc).getPid());
        }

        @Override
        public void killAll(Map<String, String> modelEnvVars) throws InterruptedException {
            for( OSProcess p : this) {
                if(p.getPid()<10)
                    continue;   // ignore system processes like "idle process"

                logger.trace("Considering to kill {}", p.getPid());

                boolean matched;
                try {
                    matched = hasMatchingEnvVars(p, modelEnvVars);
                } catch (WindowsOSProcessException e) {
                    // likely a missing privilege
                    // TODO: not a minor issue - causes process termination error in JENKINS-30782
                    if (logger.isTraceEnabled()) {
                        logger.trace("Failed to check environment variable match for process with pid=" + p.getPid() ,e);
                    }
                    continue;
                }

                if(matched) {
                    p.killRecursively();
                } else {
                    logger.trace("Environment variable didn't match for process with pid={}", p.getPid());
                }
            }
        }

        static {
            WinProcess.enableDebugPrivilege();
        }
        
        private static boolean hasMatchingEnvVars(@Nonnull OSProcess p, @Nonnull Map<String, String> modelEnvVars)
                throws WindowsOSProcessException {
            if (p instanceof WindowsOSProcess) {
                return ((WindowsOSProcess)p).hasMatchingEnvVars2(modelEnvVars);
            } else {
                // Should never happen, but there is a risk of getting such class during deserialization
                try {
                    return p.hasMatchingEnvVars(modelEnvVars);
                } catch (WinpException e) {
                    // likely a missing privilege
                    throw new WindowsOSProcessException(e);
                }
            }
        }
    }

    static abstract class Unix extends ProcessTree {
        @Override
        public OSProcess get(Process proc) {
            return get(UnixReflection.pid(proc));
        }

        public void killAll(Map<String, String> modelEnvVars) throws InterruptedException {
            for (OSProcess p : this)
                if(p.hasMatchingEnvVars(modelEnvVars))
                    p.killRecursively();
        }
    }
    /**
     * {@link ProcessTree} based on /proc.
     */
    static abstract class ProcfsUnix extends Unix {
        ProcfsUnix() {
            File[] processes = new File("/proc").listFiles(new FileFilter() {
                public boolean accept(File f) {
                    return f.isDirectory();
                }
            });
            if(processes==null) {
                logger.info("No /proc");
                return;
            }

            for (File p : processes) {
                int pid;
                try {
                    pid = Integer.parseInt(p.getName());
                } catch (NumberFormatException e) {
                    // other sub-directories
                    continue;
                }
                try {
                    this.processes.put(pid,createProcess(pid));
                } catch (IOException e) {
                    // perhaps the process status has changed since we obtained a directory listing
                }
            }
        }

        protected abstract OSProcess createProcess(int pid) throws IOException;
    }

    /**
     * A process.
     */
    public abstract class UnixProcess extends OSProcess {
        protected UnixProcess(int pid) {
            super(pid);
        }

        protected final File getFile(String relativePath) {
            return new File(new File("/proc/"+getPid()),relativePath);
        }

        /**
         * Tries to kill this process.
         */
        public void kill() throws InterruptedException {
            // after sending SIGTERM, wait for the process to cease to exist
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(softKillWaitSeconds);
            kill(deadline);
        }

        private void kill(long deadline) throws InterruptedException {
            try {
                int pid = getPid();
                logger.debug("Killing pid="+pid);
                UnixReflection.destroy(pid);
                // after sending SIGTERM, wait for the process to cease to exist
                int sleepTime = 10; // initially we sleep briefly, then sleep up to 1sec
                File status = getFile("status");
                do {
                    if (!status.exists()) {
                        break; // status is gone, process therefore as well
                    }

                    Thread.sleep(sleepTime);
                    sleepTime = Math.min(sleepTime * 2, 1000);
                } while (System.nanoTime() < deadline);
            } catch (IllegalAccessException e) {
                // this is impossible
                IllegalAccessError x = new IllegalAccessError();
                x.initCause(e);
                throw x;
            } catch (InvocationTargetException e) {
                // tunnel serious errors
                if(e.getTargetException() instanceof Error)
                    throw (Error)e.getTargetException();
                // otherwise log and let go. I need to see when this happens
                logger.info("Failed to terminate pid="+getPid(),e);
            }
        }

        public void killRecursively() throws InterruptedException {
            // after sending SIGTERM, wait for the processes to cease to exist until the deadline
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(softKillWaitSeconds);
            killRecursively(deadline);
        }

        private void killRecursively(long deadline) throws InterruptedException {
            // We kill individual processes of a tree, so handling vetoes inside #kill() is enough for UnixProcess es
            logger.debug("Recursively killing pid="+getPid());
            List<OSProcess> children = getChildren();
            kill(deadline);
            for (OSProcess p : children) {
                if (p instanceof UnixProcess) {
                    ((UnixProcess)p).killRecursively(deadline);
                } else {
                    p.killRecursively(); // should not happen, fallback to non-deadline version
                }
            }
        }

    }

    //TODO: can be replaced by multi-release JAR
    /**
     * Reflection used in the Unix support.
     */
    private static final class UnixReflection {
        /**
         * Field to access the PID of the process.
         * Required for Java 8 and older JVMs.
         */
        private static final Field JAVA8_PID_FIELD;

        /**
         * Field to access the PID of the process.
         * Required for Java 9 and above until this is replaced by multi-release JAR.
         */
        private static final Method JAVA9_PID_METHOD;

        /**
         * Method to destroy a process, given pid.
         *
         * Looking at the JavaSE source code, this is using SIGTERM (15)
         */
        private static final Method JAVA8_DESTROY_PROCESS;
        private static final Method JAVA_9_PROCESSHANDLE_OF;
        private static final Method JAVA_9_PROCESSHANDLE_DESTROY;

        static {
            try {
                if (!System.getProperty("java.specification.version").startsWith("1.")) {
                    Class<?> clazz = Process.class;
                    JAVA9_PID_METHOD = clazz.getMethod("pid");
                    JAVA8_PID_FIELD = null;
                    Class<?> processHandleClazz = Class.forName("java.lang.ProcessHandle");
                    JAVA_9_PROCESSHANDLE_OF = processHandleClazz.getMethod("of", long.class);
                    JAVA_9_PROCESSHANDLE_DESTROY = processHandleClazz.getMethod("destroy");
                    JAVA8_DESTROY_PROCESS = null;
                } else {
                    Class<?> clazz = Class.forName("java.lang.UNIXProcess");
                    JAVA8_PID_FIELD = clazz.getDeclaredField("pid");
                    JAVA8_PID_FIELD.setAccessible(true);
                    JAVA9_PID_METHOD = null;

                    JAVA8_DESTROY_PROCESS = clazz.getDeclaredMethod("destroyProcess", int.class, boolean.class);
                    JAVA8_DESTROY_PROCESS.setAccessible(true);
                    JAVA_9_PROCESSHANDLE_OF = null;
                    JAVA_9_PROCESSHANDLE_DESTROY = null;
                }
            } catch (ClassNotFoundException | NoSuchFieldException | NoSuchMethodException e) {
                throw new LinkageError("Cannot initialize reflection for Unix Processes", e);
            }
        }

        @SuppressWarnings("rawtypes")
		public static void destroy(int pid) throws IllegalAccessException,
                InvocationTargetException {
            if (JAVA8_DESTROY_PROCESS != null) {
                JAVA8_DESTROY_PROCESS.invoke(null, pid, false);
            } else {
                final Optional handle = (Optional)JAVA_9_PROCESSHANDLE_OF.invoke(null, pid);
                if (handle.isPresent()) {
                    JAVA_9_PROCESSHANDLE_DESTROY.invoke(handle.get());
                }
            }
        }

        //TODO: We ideally need to update ProcessTree APIs to Support Long (JENKINS-53799).
        public static int pid(@Nonnull Process proc) {
            try {
                if (JAVA8_PID_FIELD != null) {
                    return JAVA8_PID_FIELD.getInt(proc);
                } else {
                    long pid = (long)JAVA9_PID_METHOD.invoke(proc);
                    if (pid > Integer.MAX_VALUE) {
                        throw new IllegalAccessError("Java 9+ support error (JENKINS-53799). PID is out of Jenkins API bounds: " + pid);
                    }
                    return (int)pid;
                }
            } catch (IllegalAccessException | InvocationTargetException e) { // impossible
                IllegalAccessError x = new IllegalAccessError();
                x.initCause(e);
                throw x;
            }
        }
    }


    static class Linux extends ProcfsUnix {
        protected LinuxProcess createProcess(int pid) throws IOException {
            return new LinuxProcess(pid);
        }

        class LinuxProcess extends UnixProcess {
            private int ppid = -1;
            private Map<String, String> envVars;

            LinuxProcess(int pid) throws IOException {
                super(pid);

                BufferedReader r = new BufferedReader(new FileReader(getFile("status")));
                try {
                    String line;
                    while((line=r.readLine())!=null) {
                        line=line.toLowerCase(Locale.ENGLISH);
                        if(line.startsWith("ppid:")) {
                            ppid = Integer.parseInt(line.substring(5).trim());
                            break;
                        }
                    }
                } finally {
                    r.close();
                }
                if(ppid==-1)
                    throw new IOException("Failed to parse PPID from /proc/"+pid+"/status");
            }

            public OSProcess getParent() {
                return get(ppid);
            }

            public synchronized Map<String, String> getEnvironmentVariables() {
                if(envVars !=null)
                    return envVars;
                envVars = new HashMap<>();
                try {
                    byte[] environ = readFileToByteArray(getFile("environ"));
                    int pos=0;
                    for (int i = 0; i < environ.length; i++) {
                        byte b = environ[i];
                        if(b==0) {
                        	String line = new String(environ,pos,i-pos);
                            int sep = line.indexOf('=');
                            if(sep > 0) {
                                envVars.put(line.substring(0,sep),line.substring(sep+1));
                            }                        	
                            pos=i+1;
                        }
                    }
                } catch (IOException e) {
                    // failed to read. this can happen under normal circumstances (most notably permission denied)
                    // so don't report this as an error.
                }
                return envVars;
            }
        }

        public byte[] readFileToByteArray(File file) throws IOException {
            InputStream in = org.apache.commons.io.FileUtils.openInputStream(file);
            try {
                return org.apache.commons.io.IOUtils.toByteArray(in);
            } finally {
                    in.close();
            }
        }
    }

    /**
     * Implementation for AIX that uses {@code /proc}.
     *
     * /proc/PID/status contains a pstatus struct. We use it to determine if the process is 32 or 64 bit
     *
     * /proc/PID/psinfo contains a psinfo struct. We use it to determine where the
     *     process arguments and environment are located in PID's address space.
     *
     * /proc/PID/as contains the address space of the process we are inspecting. We can
     *     follow the pr_envp and pr_argv pointers from psinfo to find the vectors to the
     *     environment variables and process arguments, respectvely. When following pointers
     *     in this address space we need to make sure to use 32-bit or 64-bit pointers
     *     depending on what sized pointers PID uses, regardless of what size pointers
     *     the Java process uses.
     *
     *     Note that the size of a 64-bit address space is larger than Long.MAX_VALUE (because
     *     longs are signed). So normal Java utilities like RandomAccessFile and FileChannel
     *     (which use signed longs as offsets) are not able to read from the end of the address
     *     space, where envp and argv will be. Therefore we need to use LIBC.pread() directly.
     *     when accessing this file.
     */
    static class AIX extends ProcfsUnix {
        
        protected OSProcess createProcess(final int pid) throws IOException {
            return new AIXProcess(pid);
        }

        private class AIXProcess extends UnixProcess {
            private static final byte PR_MODEL_ILP32 = 0;
            private static final byte PR_MODEL_LP64 = 1;

            /*
             * An arbitrary upper-limit on how many characters readLine() will
             * try reading before giving up. This avoids having readLine() loop
             * over the entire process address space if this class has bugs.
             */
            private final int LINE_LENGTH_LIMIT = 10000;

            /*
             * True if target process is 64-bit (Java process may be different).
             */
            private final boolean b64;

            private final int ppid;

            private final long pr_envp;
            private Map<String, String> envVars;

            private AIXProcess(int pid) throws IOException {
                super(pid);

                RandomAccessFile pstatus = new RandomAccessFile(getFile("status"),"r");
                try {
					// typedef struct pstatus {
					//    uint32_t pr_flag;                /* process flags from proc struct p_flag */
					//    uint32_t pr_flag2;               /* process flags from proc struct p_flag2 */
					//    uint32_t pr_flags;               /* /proc flags */
					//    uint32_t pr_nlwp;                /* number of threads in the process */
					//    char     pr_stat;                /* process state from proc p_stat */
					//    char     pr_dmodel;              /* data model for the process */
					//    char     pr__pad1[6];            /* reserved for future use */
					//    pr_sigset_t pr_sigpend;          /* set of process pending signals */
					//    prptr64_t pr_brkbase;            /* address of the process heap */
					//    uint64_t pr_brksize;             /* size of the process heap, in bytes */
					//    prptr64_t pr_stkbase;            /* address of the process stack */
					//    uint64_t pr_stksize;             /* size of the process stack, in bytes */
					//    uint64_t pr_pid;                 /* process id */
					//    uint64_t pr_ppid;                /* parent process id */
					//    uint64_t pr_pgid;                /* process group id */
					//    uint64_t pr_sid;                 /* session id */
					//    pr_timestruc64_t pr_utime;       /* process user cpu time */
					//    pr_timestruc64_t pr_stime;       /* process system cpu time */
					//    pr_timestruc64_t pr_cutime;      /* sum of children's user times */
					//    pr_timestruc64_t pr_cstime;      /* sum of children's system times */
					//    pr_sigset_t pr_sigtrace;         /* mask of traced signals */
					//    fltset_t pr_flttrace;            /* mask of traced hardware faults */
					//    uint32_t pr_sysentry_offset;     /* offset into pstatus file of sysset_t
					//                                      * identifying system calls traced on
					//                                      * entry.  If 0, then no entry syscalls
					//                                      * are being traced. */
					//    uint32_t pr_sysexit_offset;      /* offset into pstatus file of sysset_t
					//                                      * identifying system calls traced on
					//                                      * exit.  If 0, then no exit syscalls
					//                                      * are being traced. */
					//    uint64_t pr__pad[8];             /* reserved for future use */
					//    lwpstatus_t pr_lwp;              /* "representative" thread status */
					// } pstatus_t;

                    pstatus.seek(17); // offset of pr_dmodel

					byte pr_dmodel = pstatus.readByte();

                    if (pr_dmodel == PR_MODEL_ILP32) {
                        b64 = false;
                    } else if (pr_dmodel == PR_MODEL_LP64) {
                        b64 = true;
                    } else {
                        throw new IOException("Unrecognized data model value"); // sanity check
                    }

                    pstatus.seek(88); // offset of pr_pid

                    if (adjust((int)pstatus.readLong()) != pid)
                        throw new IOException("pstatus PID mismatch"); // sanity check

                    ppid = adjust((int)pstatus.readLong()); // AIX pids are stored as a 64 bit integer, 
                                                            // but the first 4 bytes are always 0

                } finally {
                    pstatus.close();
                }

                RandomAccessFile psinfo = new RandomAccessFile(getFile("psinfo"),"r");
                try {
                    // typedef struct psinfo {
                    //   uint32_t pr_flag;                /* process flags from proc struct p_flag */
                    //   uint32_t pr_flag2;               /* process flags from proc struct p_flag2 *
                    //   uint32_t pr_nlwp;                /* number of threads in process */
                    //   uint32_t pr__pad1;               /* reserved for future use */
                    //   uint64_t pr_uid;                 /* real user id */
                    //   uint64_t pr_euid;                /* effective user id */
                    //   uint64_t pr_gid;                 /* real group id */
                    //   uint64_t pr_egid;                /* effective group id */
                    //   uint64_t pr_pid;                 /* unique process id */
                    //   uint64_t pr_ppid;                /* process id of parent */
                    //   uint64_t pr_pgid;                /* pid of process group leader */
                    //   uint64_t pr_sid;                 /* session id */
                    //   uint64_t pr_ttydev;              /* controlling tty device */
                    //   prptr64_t   pr_addr;             /* internal address of proc struct */
                    //   uint64_t pr_size;                /* process image size in kb (1024) units */
                    //   uint64_t pr_rssize;              /* resident set size in kb (1024) units */
                    //   pr_timestruc64_t pr_start;       /* process start time, time since epoch */
                    //   pr_timestruc64_t pr_time;        /* usr+sys cpu time for this process */
                    //   cid_t    pr_cid;                 /* corral id */
                    //   ushort_t pr__pad2;               /* reserved for future use */
                    //   uint32_t pr_argc;                /* initial argument count */
                    //   prptr64_t   pr_argv;             /* address of initial argument vector in
                    //                                     * user process */
                    //   prptr64_t   pr_envp;             /* address of initial environment vector
                    //                                     * in user process */
                    //   char     pr_fname[prfnsz];       /* last component of exec()ed pathname*/
                    //   char     pr_psargs[prargsz];     /* initial characters of arg list */
                    //   uint64_t pr__pad[8];             /* reserved for future use */
                    //   struct   lwpsinfo pr_lwp;        /* "representative" thread info */
                    // }

                    psinfo.seek(48); // offset of pr_pid

                    if (adjust((int)psinfo.readLong()) != pid)
                        throw new IOException("psinfo PID mismatch"); // sanity check

                    if (adjust((int)psinfo.readLong()) != ppid)
                        throw new IOException("psinfo PPID mismatch"); // sanity check

                    psinfo.seek(148); // offset of pr_argc

                    adjust(psinfo.readInt());
                    adjustL(psinfo.readLong());
                    pr_envp = adjustL(psinfo.readLong());
                } finally {
                    psinfo.close();
                }
            }

            public OSProcess getParent() {
                return get(ppid);
            }

            public synchronized Map<String, String> getEnvironmentVariables() {
                if(envVars != null)
                    return envVars;
                envVars = new HashMap<>();

                if (pr_envp == 0) {
                    return envVars;
                }

                try {
                    int psize = b64 ? 8 : 4;
                    Memory m = new Memory(psize);
                    int fd = LIBC.open(getFile("as").getAbsolutePath(), 0);

                    try {
                        // Get address of the environment vector
                        LIBC.pread(fd, m, new NativeLong(psize), new NativeLong(pr_envp));
                        long envp = b64 ? m.getLong(0) : to64(m.getInt(0));

                        if (envp == 0) // Should never happen
                            return envVars;

                        // Itterate through environment vector
                        for( int n=0; ; n++ ) {

                            LIBC.pread(fd, m, new NativeLong(psize), new NativeLong(envp+(n*psize)));
                            long addr = b64 ? m.getLong(0) : to64(m.getInt(0));

                            if (addr == 0) // completed the walk
                                break;

                            // now read the null-terminated string
                            String line = readLine(fd, addr, "env["+ n +"]");
                            int sep = line.indexOf('=');
                            if(sep > 0) {
                                envVars.put(line.substring(0,sep),line.substring(sep+1));
                            }                            
                        }
                    } finally  {
                       LIBC.close(fd); 
                    }
                } catch (IOException | LastErrorException e) {
                    // failed to read. this can happen under normal circumstances (most notably permission denied)
                    // so don't report this as an error.
                }
                return envVars;
            }

            private String readLine(int fd, long addr, String prefix) throws IOException {
                if(logger.isTraceEnabled())
                    logger.trace("Reading "+prefix+" at "+addr);

                Memory m = new Memory(1);
                byte ch = 1;
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                int i = 0;
                while(true) {
                    if (i++ > LINE_LENGTH_LIMIT) {
                        logger.trace("could not find end of line, giving up");
                        throw new IOException("could not find end of line, giving up");
                    }

                    LIBC.pread(fd, m, new NativeLong(1), new NativeLong(addr));
                    ch = m.getByte(0);

                    if (ch == 0)
                        break;
                    buf.write(ch);
                    addr++;
                }
                String line = buf.toString();
                if(logger.isTraceEnabled())
                    logger.trace(prefix+" was "+line);
                return line;
            }
        }

        /**
         * int to long conversion with zero-padding.
         */
        private static long to64(int i) {
            return i&0xFFFFFFFFL;
        }

        /**
         * {@link DataInputStream} reads a value in big-endian, so
         * convert it to the correct value on little-endian systems.
         */
        private static int adjust(int i) {
            if(IS_LITTLE_ENDIAN)
                return (i<<24) |((i<<8) & 0x00FF0000) | ((i>>8) & 0x0000FF00) | (i>>>24);
            else
                return i;
        }

        public static long adjustL(long i) {
            if(IS_LITTLE_ENDIAN) {
                return Long.reverseBytes(i);
            } else {
                return i;
            }
        }
    }

    /**
     * Implementation for Solaris that uses {@code /proc}.
     *
     * /proc/PID/psinfo contains a psinfo_t struct. We use it to determine where the
     *     process arguments and environment are located in PID's address space.
     *     Note that the psinfo_t struct is different (different sized elements) for 32-bit
     *     vs 64-bit processes and the kernel will provide the version of the struct that
     *     matches the _reader_ (this Java process) regardless of whether PID is a
     *     32-bit or 64-bit process.
     *
     *     Note that this means that if PID is a 64-bit process, then a 32-bit Java
     *     process can not get meaningful values for envp and argv out of the psinfo_t. The
     *     values will have been truncated to 32-bits.
     *
     * /proc/PID/as contains the address space of the process we are inspecting. We can
     *     follow the envp and argv pointers from psinfo_t to find the environment variables
     *     and process arguments. When following pointers in this address space we need to
     *     make sure to use 32-bit or 64-bit pointers depending on what sized pointers
     *     PID uses, regardless of what size pointers the Java process uses.
     *
     *     Note that the size of a 64-bit address space is larger than Long.MAX_VALUE (because
     *     longs are signed). So normal Java utilities like RandomAccessFile and FileChannel
     *     (which use signed longs as offsets) are not able to read from the end of the address
     *     space, where envp and argv will be. Therefore we need to use LIBC.pread() directly.
     *     when accessing this file.
     */
    static class Solaris extends ProcfsUnix {
        protected OSProcess createProcess(final int pid) throws IOException {
            return new SolarisProcess(pid);
        }

        private class SolarisProcess extends UnixProcess {
            private static final byte PR_MODEL_LP64 = 2;

            /*
             * An arbitrary upper-limit on how many characters readLine() will
             * try reading before giving up. This avoids having readLine() loop
             * over the entire process address space if this class has bugs.
             */
            private final int LINE_LENGTH_LIMIT = 10000;

            /*
             * True if target process is 64-bit (Java process may be different).
             */
            private final boolean b64;

            private final int ppid;
            /**
             * Address of the environment vector.
             */
            private final long envp;
            /**
             * Similarly, address of the arguments vector.
             */
            private Map<String, String> envVars;

            private SolarisProcess(int pid) throws IOException {
                super(pid);

                RandomAccessFile psinfo = new RandomAccessFile(getFile("psinfo"),"r");
                try {
                    // see http://cvs.opensolaris.org/source/xref/onnv/onnv-gate/usr/src/uts/common/sys/procfs.h
                    //typedef struct psinfo {
                    //	int	pr_flag;	/* process flags */
                    //	int	pr_nlwp;	/* number of lwps in the process */
                    //	pid_t	pr_pid;	/* process id */
                    //	pid_t	pr_ppid;	/* process id of parent */
                    //	pid_t	pr_pgid;	/* process id of process group leader */
                    //	pid_t	pr_sid;	/* session id */
                    //	uid_t	pr_uid;	/* real user id */
                    //	uid_t	pr_euid;	/* effective user id */
                    //	gid_t	pr_gid;	/* real group id */
                    //	gid_t	pr_egid;	/* effective group id */
                    //	uintptr_t	pr_addr;	/* address of process */
                    //	size_t	pr_size;	/* size of process image in Kbytes */
                    //	size_t	pr_rssize;	/* resident set size in Kbytes */
                    //	dev_t	pr_ttydev;	/* controlling tty device (or PRNODEV) */
                    //	ushort_t	pr_pctcpu;	/* % of recent cpu time used by all lwps */
                    //	ushort_t	pr_pctmem;	/* % of system memory used by process */
                    //	timestruc_t	pr_start;	/* process start time, from the epoch */
                    //	timestruc_t	pr_time;	/* cpu time for this process */
                    //	timestruc_t	pr_ctime;	/* cpu time for reaped children */
                    //	char	pr_fname[PRFNSZ];	/* name of exec'ed file */
                    //	char	pr_psargs[PRARGSZ];	/* initial characters of arg list */
                    //	int	pr_wstat;	/* if zombie, the wait() status */
                    //	int	pr_argc;	/* initial argument count */
                    //	uintptr_t	pr_argv;	/* address of initial argument vector */
                    //	uintptr_t	pr_envp;	/* address of initial environment vector */
                    //	char	pr_dmodel;	/* data model of the process */
                    //	lwpsinfo_t	pr_lwp;	/* information for representative lwp */
                    //} psinfo_t;

                    // see http://cvs.opensolaris.org/source/xref/onnv/onnv-gate/usr/src/uts/common/sys/types.h
                    // for the size of the various datatype.

                    // see http://cvs.opensolaris.org/source/xref/onnv/onnv-gate/usr/src/cmd/ptools/pargs/pargs.c
                    // for how to read this information

                    psinfo.seek(8);
                    if(adjust(psinfo.readInt())!=pid)
                        throw new IOException("psinfo PID mismatch");   // sanity check
                    ppid = adjust(psinfo.readInt());

                    /*
                     * Read the remainder of psinfo_t differently depending on whether the
                     * Java process is 32-bit or 64-bit.
                     */
                    if (Native.POINTER_SIZE == 8) {
                        psinfo.seek(236);  // offset of pr_argc
                        adjust(psinfo.readInt());
                        adjustL(psinfo.readLong());
                        envp = adjustL(psinfo.readLong());
                        b64 = (psinfo.readByte() == PR_MODEL_LP64);
                    } else {
                        psinfo.seek(188);  // offset of pr_argc
                        adjust(psinfo.readInt());
                        to64(adjust(psinfo.readInt()));
                        envp = to64(adjust(psinfo.readInt()));
                        b64 = (psinfo.readByte() == PR_MODEL_LP64);
                    }
                } finally {
                    psinfo.close();
                }
                if(ppid==-1)
                    throw new IOException("Failed to parse PPID from /proc/"+pid+"/status");

            }

            public OSProcess getParent() {
                return get(ppid);
            }

            public synchronized Map<String, String> getEnvironmentVariables() {
                if(envVars !=null)
                    return envVars;
                envVars = new HashMap<>();

				if (envp == 0) {
				    return envVars;
				}

                int psize = b64 ? 8 : 4;
                Memory m = new Memory(psize);
                try {
                    if(logger.isDebugEnabled())
                        logger.debug("Reading "+getFile("as"));
                    int fd = LIBC.open(getFile("as").getAbsolutePath(), 0);
                    try {
                        for( int n=0; ; n++ ) {
                            // read a pointer to one entry
                            LIBC.pread(fd, m, new NativeLong(psize), new NativeLong(envp+n*psize));
                            long addr = b64 ? m.getLong(0) : to64(m.getInt(0));
                            if (addr == 0) // completed the walk
                                break;

                            // now read the null-terminated string
                            String line = readLine(fd, addr, "env["+ n +"]");
                            int sep = line.indexOf('=');
                            if(sep > 0) {
                                envVars.put(line.substring(0,sep),line.substring(sep+1));
                            }                            
                        }
                    } finally {
                        LIBC.close(fd);
                    }
                } catch (IOException | LastErrorException e) {
                    // failed to read. this can happen under normal circumstances (most notably permission denied)
                    // so don't report this as an error.
                }
                return envVars;
            }

            private String readLine(int fd, long addr, String prefix) throws IOException {
                if(logger.isTraceEnabled())
                    logger.trace("Reading "+prefix+" at "+addr);

                Memory m = new Memory(1);
                byte ch = 1;
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                int i = 0;
                while(true) {
                    if (i++ > LINE_LENGTH_LIMIT) {
                        logger.trace("could not find end of line, giving up");
                        throw new IOException("could not find end of line, giving up");
                    }

                    LIBC.pread(fd, m, new NativeLong(1), new NativeLong(addr));
                    ch = m.getByte(0);
                    if (ch == 0)
                        break;
                    buf.write(ch);
                    addr++;
                }
                String line = buf.toString();
                if(logger.isTraceEnabled())
                    logger.trace(prefix+" was "+line);
                return line;
            }
        }

        /**
         * int to long conversion with zero-padding.
         */
        private static long to64(int i) {
            return i&0xFFFFFFFFL;
        }

        /**
         * {@link DataInputStream} reads a value in big-endian, so
         * convert it to the correct value on little-endian systems.
         */
        private static int adjust(int i) {
            if(IS_LITTLE_ENDIAN)
                return (i<<24) |((i<<8) & 0x00FF0000) | ((i>>8) & 0x0000FF00) | (i>>>24);
            else
                return i;
        }

        public static long adjustL(long i) {
            if(IS_LITTLE_ENDIAN) {
                return Long.reverseBytes(i);
            } else {
                return i;
            }
        }
    }

    /**
     * Implementation for Mac OS X based on sysctl(3).
     */
    private static class Darwin extends Unix {
        Darwin() {
            String arch = System.getProperty("sun.arch.data.model");
            if ("64".equals(arch)) {
                sizeOf_kinfo_proc = sizeOf_kinfo_proc_64;
                kinfo_proc_pid_offset = kinfo_proc_pid_offset_64;
                kinfo_proc_ppid_offset = kinfo_proc_ppid_offset_64;
            } else {
                sizeOf_kinfo_proc = sizeOf_kinfo_proc_32;
                kinfo_proc_pid_offset = kinfo_proc_pid_offset_32;
                kinfo_proc_ppid_offset = kinfo_proc_ppid_offset_32;
            }
            try {
                IntByReference ref = new IntByReference(sizeOfInt);
                IntByReference size = new IntByReference(sizeOfInt);
                Memory m;
                int nRetry = 0;
                while(true) {
                    // find out how much memory we need to do this
                    if(LIBC.sysctl(MIB_PROC_ALL,3, NULL, size, NULL, ref)!=0)
                        throw new IOException("Failed to obtain memory requirement: "+LIBC.strerror(Native.getLastError()));

                    // now try the real call
                    m = new Memory(size.getValue());
                    if(LIBC.sysctl(MIB_PROC_ALL,3, m, size, NULL, ref)!=0) {
                        if(Native.getLastError()==ENOMEM && nRetry++<16)
                            continue; // retry
                        throw new IOException("Failed to call kern.proc.all: "+LIBC.strerror(Native.getLastError()));
                    }
                    break;
                }

                int count = size.getValue()/sizeOf_kinfo_proc;
                logger.debug("Found "+count+" processes");

                for( int base=0; base<size.getValue(); base+=sizeOf_kinfo_proc) {
                    int pid = m.getInt(base+ kinfo_proc_pid_offset);
                    int ppid = m.getInt(base+ kinfo_proc_ppid_offset);
//                    int effective_uid = m.getInt(base+304);
//                    byte[] comm = new byte[16];
//                    m.read(base+163,comm,0,16);

                    super.processes.put(pid,new DarwinProcess(pid,ppid));
                }
            } catch (IOException e) {
                logger.warn("Failed to obtain process list",e);
            }
        }

        private class DarwinProcess extends UnixProcess {
            private final int ppid;
            private Map<String, String> envVars;
            private List<String> arguments;

            DarwinProcess(int pid, int ppid) {
                super(pid);
                this.ppid = ppid;
            }

            public OSProcess getParent() {
                return get(ppid);
            }

            public synchronized Map<String, String> getEnvironmentVariables() {
                if(envVars !=null)
                    return envVars;
                parse();
                return envVars;
            }

            private void parse() {
                try {
                    arguments = new ArrayList<String>();
                    envVars = new HashMap<>();

                    IntByReference intByRef = new IntByReference();

                    IntByReference argmaxRef = new IntByReference(0);
                    IntByReference size = new IntByReference(sizeOfInt);

                    if(LIBC.sysctl(new int[]{CTL_KERN,KERN_ARGMAX},2, argmaxRef.getPointer(), size, NULL, intByRef)!=0)
                        throw new IOException("Failed to get kern.argmax: "+LIBC.strerror(Native.getLastError()));

                    int argmax = argmaxRef.getValue();

                    class StringArrayMemory extends Memory {
                        private long offset=0;

                        StringArrayMemory(long l) {
                            super(l);
                        }

                        int readInt() {
                            int r = getInt(offset);
                            offset+=sizeOfInt;
                            return r;
                        }

                        byte peek() {
                            return getByte(offset);
                        }

                        String readString() {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            byte ch;
                            while((ch = getByte(offset++))!='\0')
                                baos.write(ch);
                            return baos.toString();
                        }

                        void skip0() {
                            // skip padding '\0's
                            while(getByte(offset)=='\0')
                                offset++;
                        }
                    }
                    StringArrayMemory m = new StringArrayMemory(argmax);
                    size.setValue(argmax);
                    if(LIBC.sysctl(new int[]{CTL_KERN,KERN_PROCARGS2,pid},3, m, size, NULL, intByRef)!=0)
                        throw new IOException("Failed to obtain ken.procargs2: "+LIBC.strerror(Native.getLastError()));


                    /*
                    * Make a sysctl() call to get the raw argument space of the
                        * process.  The layout is documented in start.s, which is part
                        * of the Csu project.  In summary, it looks like:
                        *
                        * /---------------\ 0x00000000
                        * :               :
                        * :               :
                        * |---------------|
                        * | argc          |
                        * |---------------|
                        * | arg[0]        |
                        * |---------------|
                        * :               :
                        * :               :
                        * |---------------|
                        * | arg[argc - 1] |
                        * |---------------|
                        * | 0             |
                        * |---------------|
                        * | env[0]        |
                        * |---------------|
                        * :               :
                        * :               :
                        * |---------------|
                        * | env[n]        |
                        * |---------------|
                        * | 0             |
                        * |---------------| <-- Beginning of data returned by sysctl()
                        * | exec_path     |     is here.
                        * |:::::::::::::::|
                        * |               |
                        * | String area.  |
                        * |               |
                        * |---------------| <-- Top of stack.
                        * :               :
                        * :               :
                        * \---------------/ 0xffffffff
                        */

                    // I find the Darwin source code of the 'ps' command helpful in understanding how it does this:
                    // see http://www.opensource.apple.com/source/adv_cmds/adv_cmds-147/ps/print.c
                    int argc = m.readInt();
                    String args0 = m.readString(); // exec path
                    m.skip0();
                    try {
                        for( int i=0; i<argc; i++) {
                            arguments.add(m.readString());
                        }
                    } catch (IndexOutOfBoundsException e) {
                        throw new IllegalStateException("Failed to parse arguments: pid="+pid+", arg0="+args0+", arguments="+arguments+", nargs="+argc+". Please see https://jenkins.io/redirect/troubleshooting/darwin-failed-to-parse-arguments",e);
                    }

                    // read env vars that follow
                    while(m.peek()!=0) {
                    	String line = m.readString();
                        int sep = line.indexOf('=');
                        if(sep > 0) {
                            envVars.put(line.substring(0,sep),line.substring(sep+1));
                        }                    	
                    }
                } catch (IOException e) {
                    // this happens with insufficient permissions, so just ignore the problem.
                }
            }
        }

        // local constants
        private final int sizeOf_kinfo_proc;
        private static final int sizeOf_kinfo_proc_32 = 492; // on 32bit Mac OS X.
        private static final int sizeOf_kinfo_proc_64 = 648; // on 64bit Mac OS X.
        private final int kinfo_proc_pid_offset;
        private static final int kinfo_proc_pid_offset_32 = 24;
        private static final int kinfo_proc_pid_offset_64 = 40;
        private final int kinfo_proc_ppid_offset;
        private static final int kinfo_proc_ppid_offset_32 = 416;
        private static final int kinfo_proc_ppid_offset_64 = 560;
        private static final int sizeOfInt = Native.getNativeSize(int.class);
        private static final int CTL_KERN = 1;
        private static final int KERN_PROC = 14;
        private static final int KERN_PROC_ALL = 0;
        private static final int ENOMEM = 12;
        private static int[] MIB_PROC_ALL = {CTL_KERN, KERN_PROC, KERN_PROC_ALL};
        private static final int KERN_ARGMAX = 8;
        private static final int KERN_PROCARGS2 = 49;
    }

    /*
        On MacOS X, there's no procfs <http://www.osxbook.com/book/bonus/chapter11/procfs/>
        instead you'd do it with the sysctl <http://search.cpan.org/src/DURIST/Proc-ProcessTable-0.42/os/darwin.c>
        <http://developer.apple.com/documentation/Darwin/Reference/ManPages/man3/sysctl.3.html>
        There's CLI but that doesn't seem to offer the access to per-process info
        <http://developer.apple.com/documentation/Darwin/Reference/ManPages/man8/sysctl.8.html>
        On HP-UX, pstat_getcommandline get you command line, but I'm not seeing any environment variables.
     */

    private static final boolean IS_LITTLE_ENDIAN = "little".equals(System.getProperty("sun.cpu.endian"));

}