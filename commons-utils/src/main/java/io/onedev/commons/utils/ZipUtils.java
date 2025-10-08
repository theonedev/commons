package io.onedev.commons.utils;

import io.onedev.commons.bootstrap.Bootstrap;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Zip;
import org.apache.tools.ant.types.ZipFileSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jspecify.annotations.Nullable;
import java.io.*;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipUtils {

    private static final int BUFFER_SIZE = 64 * 1024;

    private static final Logger logger = LoggerFactory.getLogger(ZipUtils.class);

    /**
     * Zip specified directory recursively as specified file, with
     *
     * @param dir
     * @param file
     */
    public static void zip(File dir, File file, @Nullable String executables) {
        Zip zip = new Zip();

        Project antProject = new Project();
        antProject.init();
        antProject.addBuildListener(new BuildListener() {

            @Override
            public void messageLogged(BuildEvent event) {
                if (event.getPriority() == Project.MSG_ERR)
                    logger.error(event.getMessage());
                else if (event.getPriority() == Project.MSG_WARN)
                    logger.warn(event.getMessage());
                else if (event.getPriority() == Project.MSG_INFO)
                    logger.info(event.getMessage());
                else
                    logger.debug(event.getMessage());
            }

            @Override
            public void buildFinished(BuildEvent event) {
            }

            @Override
            public void buildStarted(BuildEvent event) {
            }

            @Override
            public void targetFinished(BuildEvent event) {
            }

            @Override
            public void targetStarted(BuildEvent event) {
            }

            @Override
            public void taskFinished(BuildEvent event) {
            }

            @Override
            public void taskStarted(BuildEvent event) {
            }

        });

        zip.setProject(antProject);
        zip.setDestFile(file);

        if (executables != null) {
            ZipFileSet zipFileSet = new ZipFileSet();
            zipFileSet.setDir(dir);
            zipFileSet.setExcludes(executables);
            zip.addZipfileset(zipFileSet);

            zipFileSet = new ZipFileSet();
            zipFileSet.setDir(dir);
            zipFileSet.setIncludes(executables);
            zipFileSet.setFileMode("755");
            zip.addZipfileset(zipFileSet);
        } else {
            ZipFileSet zipFileSet = new ZipFileSet();
            zipFileSet.setDir(dir);
            zip.addZipfileset(zipFileSet);
        }

        zip.execute();
    }

    /**
     * Zip specified directory recursively as specified file.
     *
     * @param dir
     */
    public static void zip(File dir, OutputStream os) {
        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(os, BUFFER_SIZE))) {
            zos.setLevel(Deflater.DEFAULT_COMPRESSION);
            zip(zos, dir, "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void zip(ZipOutputStream zos, File dir, String basePath) {
        byte buffer[] = new byte[BUFFER_SIZE];

        try {
            if (basePath.length() != 0)
                zos.putNextEntry(new ZipEntry(basePath + "/"));

            for (File file : dir.listFiles()) {
                if (file.isDirectory()) {
                    if (basePath.length() != 0)
                        zip(zos, file, basePath + "/" + file.getName());
                    else
                        zip(zos, file, file.getName());
                } else {
                    try (FileInputStream is = new FileInputStream(file)) {
                        if (basePath.length() != 0)
                            zos.putNextEntry(new ZipEntry(basePath + "/" + file.getName()));
                        else
                            zos.putNextEntry(new ZipEntry(file.getName()));
                        int len;
                        while ((len = is.read(buffer)) > 0)
                            zos.write(buffer, 0, len);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Unzip files matching specified matcher from specified file to specified directory.
     *
     * @param srcFile zip file to extract from
     * @param destDir destination directory to extract to
     */
    public static void unzip(File srcFile, File destDir) {
        Bootstrap.unzip(srcFile, destDir);
    }

    /**
     * Unzip files matching specified matcher from input stream to specified directory.
     *
     * @param is      input stream to unzip files from. This method will not close the stream
     *                after using it. Caller should be responsible for closing
     * @param destDir destination directory to extract files to
     */
    public static void unzip(InputStream is, File destDir) {
        Bootstrap.unzip(is, destDir);
    }

}
