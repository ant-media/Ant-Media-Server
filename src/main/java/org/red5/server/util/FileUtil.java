/*
 * RED5 Open Source Media Server - https://github.com/Red5/
 * 
 * Copyright 2006-2016 by respective authors (see below). All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.red5.server.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic file utility containing useful file or directory manipulation functions.
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 * @author Dominick Accattato (daccattato@gmail.com)
 */
public class FileUtil {

    private static Logger log = LoggerFactory.getLogger(FileUtil.class);

    public static void copyFile(File source, File dest) throws IOException {
        log.debug("Copy from {} to {}", source.getAbsoluteFile(), dest.getAbsoluteFile());
        FileInputStream fi = new FileInputStream(source);
        FileChannel fic = fi.getChannel();
        MappedByteBuffer mbuf = fic.map(FileChannel.MapMode.READ_ONLY, 0, source.length());
        fic.close();
        fi.close();
        fi = null;

        // ensure the destination directory exists
        if (!dest.exists()) {
            String destPath = dest.getPath();
            log.debug("Destination path: {}", destPath);
            String destDir = destPath.substring(0, destPath.lastIndexOf(File.separatorChar));
            log.debug("Destination dir: {}", destDir);
            File dir = new File(destDir);
            if (!dir.exists()) {
                if (dir.mkdirs()) {
                    log.debug("Directory created");
                } else {
                    log.warn("Directory not created");
                }
            }
            dir = null;
        }

        FileOutputStream fo = new FileOutputStream(dest);
        FileChannel foc = fo.getChannel();
        foc.write(mbuf);
        foc.close();
        fo.close();
        fo = null;

        mbuf.clear();
        mbuf = null;
    }

    public static void copyFile(String source, String dest) throws IOException {
        copyFile(new File(source), new File(dest));
    }

    public static void moveFile(String source, String dest) throws IOException {
        copyFile(source, dest);
        File src = new File(source);
        if (src.exists() && src.canRead()) {
            if (src.delete()) {
                log.debug("Source file was deleted");
            } else {
                log.debug("Source file was not deleted, the file will be deleted on exit");
                src.deleteOnExit();
            }
        } else {
            log.warn("Source file could not be accessed for removal");
        }
        src = null;
    }

    /**
     * Deletes a directory and its contents. This will fail if there are any file locks or if the directory cannot be emptied.
     * 
     * @param directory
     *            directory to delete
     * @throws IOException
     *             if directory cannot be deleted
     * @return true if directory was successfully deleted; false if directory did not exist
     */
    public static boolean deleteDirectory(String directory) throws IOException {
        return deleteDirectory(directory, false);
    }

    /**
     * Deletes a directory and its contents. This will fail if there are any file locks or if the directory cannot be emptied.
     * 
     * @param directory
     *            directory to delete
     * @param useOSNativeDelete
     *            flag to signify use of operating system delete function
     * @throws IOException
     *             if directory cannot be deleted
     * @return true if directory was successfully deleted; false if directory did not exist
     */
    public static boolean deleteDirectory(String directory, boolean useOSNativeDelete) throws IOException {
        boolean result = false;
        if (!useOSNativeDelete) {
            File dir = new File(directory);
            // first all files have to be cleared out
            for (File file : dir.listFiles()) {
                if (file.delete()) {
                    log.debug("{} was deleted", file.getName());
                } else {
                    log.debug("{} was not deleted", file.getName());
                    file.deleteOnExit();
                }
                file = null;
            }
            // not you may remove the dir
            if (dir.delete()) {
                log.debug("Directory was deleted");
                result = true;
            } else {
                log.debug("Directory was not deleted, it may be deleted on exit");
                dir.deleteOnExit();
            }
            dir = null;
        } else {
            Process p = null;
            Thread std = null;
            try {
                Runtime runTime = Runtime.getRuntime();
                log.debug("Execute runtime");
                //determine file system type
                if (File.separatorChar == '\\') {
                    //we are windows
                    p = runTime.exec("CMD /D /C \"RMDIR /Q /S " + directory.replace('/', '\\') + "\"");
                } else {
                    //we are unix variant 
                    p = runTime.exec("rm -rf " + directory.replace('\\', File.separatorChar));
                }
                // observe std out
                std = stdOut(p);
                // wait for the observer threads to finish
                while (std.isAlive()) {
                    try {
                        Thread.sleep(250);
                    } catch (Exception e) {
                    }
                }
                log.debug("Process threads wait exited");
                result = true;
            } catch (Exception e) {
                log.error("Error running delete script", e);
            } finally {
                if (null != p) {
                    log.debug("Destroying process");
                    p.destroy();
                    p = null;
                }
                std = null;
            }
        }
        return result;
    }

    /**
     * Rename a file natively; using REN on Windows and mv on *nix.
     * 
     * @param from
     *            old name
     * @param to
     *            new name
     */
    public static void rename(String from, String to) {
        try {
            Files.move(Paths.get(from), Paths.get(to), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Error renaming file", e);
        }
    }

    /**
     * Special method for capture of StdOut.
     * 
     * @return stdOut thread
     */
    private final static Thread stdOut(final Process p) {
        final byte[] empty = new byte[128];
        for (int b = 0; b < empty.length; b++) {
            empty[b] = (byte) 0;
        }
        Thread std = new Thread() {
            public void run() {
                StringBuilder sb = new StringBuilder(1024);
                byte[] buf = new byte[128];
                BufferedInputStream bis = new BufferedInputStream(p.getInputStream());
                log.debug("Process output:");
                try {
                    while (bis.read(buf) != -1) {
                        sb.append(new String(buf).trim());
                        // clear buffer
                        System.arraycopy(empty, 0, buf, 0, buf.length);
                    }
                    log.debug(sb.toString());
                    bis.close();
                } catch (Exception e) {
                    log.error("{}", e);
                }
            }
        };
        std.setDaemon(true);
        std.start();
        return std;
    }

    /**
     * Create a directory.
     * 
     * @param directory
     *            directory to make
     * @return whether a new directory was made
     * @throws IOException
     *             if directory does not already exist or cannot be made
     */
    public static boolean makeDirectory(String directory) throws IOException {
        return makeDirectory(directory, false);
    }

    /**
     * Create a directory. The parent directories will be created if <i>createParents</i> is passed as true.
     * 
     * @param directory
     *            directory
     * @param createParents
     *            whether to create all parents
     * @return true if directory was created; false if it already existed
     * @throws IOException
     *             if we cannot create directory
     * 
     */
    public static boolean makeDirectory(String directory, boolean createParents) throws IOException {
        boolean created = false;
        File dir = new File(directory);
        if (createParents) {
            created = dir.mkdirs();
            if (created) {
                log.debug("Directory created: {}", dir.getAbsolutePath());
            } else {
                log.debug("Directory was not created: {}", dir.getAbsolutePath());
            }
        } else {
            created = dir.mkdir();
            if (created) {
                log.debug("Directory created: {}", dir.getAbsolutePath());
            } else {
                log.debug("Directory was not created: {}", dir.getAbsolutePath());
            }
        }
        dir = null;
        return created;
    }

    /**
     * Unzips a war file to an application located under the webapps directory
     * 
     * @param compressedFileName
     *            The String name of the war file
     * @param destinationDir
     *            The destination directory, ie: webapps
     */
    public static void unzip(String compressedFileName, String destinationDir) {
        //strip everything except the applications name
        String dirName = null;
        // checks to see if there is a dash "-" in the filename of the war. 
        String applicationName = compressedFileName.substring(compressedFileName.lastIndexOf("/"));
        int dashIndex = applicationName.indexOf('-');
        if (dashIndex != -1) {
            //strip everything except the applications name
            dirName = compressedFileName.substring(0, dashIndex);
        } else {
            //grab every char up to the last '.'
            dirName = compressedFileName.substring(0, compressedFileName.lastIndexOf('.'));
        }
        log.debug("Directory: {}", dirName);
        //String tmpDir = System.getProperty("java.io.tmpdir");
        File zipDir = new File(compressedFileName);
        File parent = zipDir.getParentFile();
        log.debug("Parent: {}", (parent != null ? parent.getName() : null));
        //File tmpDir = new File(System.getProperty("java.io.tmpdir"), dirName);
        File tmpDir = new File(destinationDir);
        // make the war directory
        log.debug("Making directory: {}", tmpDir.mkdirs());
        ZipFile zf = null;
        try {
            zf = new ZipFile(compressedFileName);
            Enumeration<?> e = zf.entries();
            while (e.hasMoreElements()) {
                ZipEntry ze = (ZipEntry) e.nextElement();
                log.debug("Unzipping {}", ze.getName());
                if (ze.isDirectory()) {
                    log.debug("is a directory");
                    File dir = new File(tmpDir + "/" + ze.getName());
                    Boolean tmp = dir.mkdir();
                    log.debug("{}", tmp);
                    continue;
                }

                // checks to see if a zipEntry contains a path
                // i.e. ze.getName() == "META-INF/MANIFEST.MF"
                // if this case is true, then we create the path first
                if (ze.getName().lastIndexOf("/") != -1) {
                    String zipName = ze.getName();
                    String zipDirStructure = zipName.substring(0, zipName.lastIndexOf("/"));
                    File completeDirectory = new File(tmpDir + "/" + zipDirStructure);
                    if (!completeDirectory.exists()) {
                        if (!completeDirectory.mkdirs()) {
                            log.error("could not create complete directory structure");
                        }
                    }
                }

                // creates the file
                FileOutputStream fout = new FileOutputStream(tmpDir + "/" + ze.getName());
                InputStream in = zf.getInputStream(ze);
                copy(in, fout);
                in.close();
                fout.close();
            }
            e = null;
        } catch (IOException e) {
            log.error("Errored unzipping", e);
            //log.warn("Exception {}", e);
        } finally {
            if (zf != null) {
                try {
                    zf.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public static void copy(InputStream in, OutputStream out) throws IOException {
        
        synchronized (in) {
            synchronized (out) {
                byte[] buffer = new byte[256];
                while (true) {
                    int bytesRead = in.read(buffer);
                    if (bytesRead == -1)
                        break;
                    out.write(buffer, 0, bytesRead);
                }
            }
        }
    }

    /**
     * Quick-n-dirty directory formatting to support launching in windows, specifically from ant.
     * 
     * @param absWebappsPath
     *            abs webapps path
     * @param contextDirName
     *            conext directory name
     * @return full path
     */
    public static String formatPath(String absWebappsPath, String contextDirName) {
        StringBuilder path = new StringBuilder(absWebappsPath.length() + contextDirName.length());
        path.append(absWebappsPath);
        if (log.isTraceEnabled()) {
            log.trace("Path start: {}", path.toString());
        }
        int idx = -1;
        if (File.separatorChar != '/') {
            while ((idx = path.indexOf(File.separator)) != -1) {
                path.deleteCharAt(idx);
                path.insert(idx, '/');
            }
        }
        if (log.isTraceEnabled()) {
            log.trace("Path step 1: {}", path.toString());
        }
        //remove any './'
        if ((idx = path.indexOf("./")) != -1) {
            path.delete(idx, idx + 2);
        }
        if (log.isTraceEnabled()) {
            log.trace("Path step 2: {}", path.toString());
        }
        //add / to base path if one doesnt exist
        if (path.charAt(path.length() - 1) != '/') {
            path.append('/');
        }
        if (log.isTraceEnabled()) {
            log.trace("Path step 3: {}", path.toString());
        }
        //remove the / from the beginning of the context dir
        if (contextDirName.charAt(0) == '/' && path.charAt(path.length() - 1) == '/') {
            path.append(contextDirName.substring(1));
        } else {
            path.append(contextDirName);
        }
        if (log.isTraceEnabled()) {
            log.trace("Path step 4: {}", path.toString());
        }
        return path.toString();
    }

    /**
     * Generates a custom name containing numbers and an underscore ex. 282818_00023. The name contains current seconds and a random number component.
     * 
     * @return custom name
     */
    public static String generateCustomName() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        sb.append(PropertyConverter.getCurrentTimeSeconds());
        sb.append('_');
        int i = random.nextInt(99999);
        if (i < 10) {
            sb.append("0000");
        } else if (i < 100) {
            sb.append("000");
        } else if (i < 1000) {
            sb.append("00");
        } else if (i < 10000) {
            sb.append('0');
        }
        sb.append(i);
        return sb.toString();
    }

    /**
     * Reads all the bytes of a given file into an array. If the file size exceeds Integer.MAX_VALUE, it will be truncated.
     * 
     * @param localSwfFile
     *            swf file
     * @return file bytes
     */
    public static byte[] readAsByteArray(File localSwfFile) {
        byte[] fileBytes = new byte[(int) localSwfFile.length()];
        byte[] b = new byte[1];
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(localSwfFile);
            for (int i = 0; i < Integer.MAX_VALUE; i++) {
                if (fis.read(b) != -1) {
                    fileBytes[i] = b[0];
                } else {
                    break;
                }
            }
        } catch (IOException e) {
            log.warn("Exception reading file bytes", e);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                }
            }
        }
        return fileBytes;
    }

}
