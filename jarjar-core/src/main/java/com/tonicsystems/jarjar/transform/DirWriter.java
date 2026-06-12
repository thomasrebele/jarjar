package com.tonicsystems.jarjar.transform;

import edu.umd.cs.findbugs.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * Writes files to a dir-like target (a jar file, or a dir on the file system).
 */
public interface DirWriter extends AutoCloseable {

    static DirWriter create(File file) throws IOException {
        boolean isDir = Files.isDirectory(file.toPath());
        return isDir ? new DirWriter.FSWriter(file.toPath()) : new DirWriter.JarWriter(file);
    }

    @Nullable
    private static String getDirname(String name) {
        int dirIdx = name.lastIndexOf('/');
        if (dirIdx == -1)
            return null;
        String dirName = name.substring(0, dirIdx + 1);
        return dirName;
    }

    default void mkdirs(Transformable t) throws IOException {
        String dirname = getDirname(t.name);
        if (dirname != null) {
            mkDirs(dirname);
        }
    }

    void mkDirs(String dirname) throws IOException;

    void writeFile(Transformable t) throws IOException;

    void close() throws IOException;

    class JarWriter implements DirWriter {

        private final JarOutputStream outputJarStream ;
        private final Set<String> dirs = new HashSet<String>();

        public JarWriter(File outputFile) throws IOException {
            outputJarStream = new JarOutputStream(new FileOutputStream(outputFile));
        }

        public void mkDirs(String dirName) throws IOException {
            if (dirs.add(dirName)) {
                JarEntry dirEntry = new JarEntry(dirName);
                outputJarStream.putNextEntry(dirEntry);
            }
        }

        @Override
        public void writeFile(Transformable t) throws IOException {
            mkdirs(t);

            JarEntry outputEntry = new JarEntry(t.name);
            outputEntry.setTime(t.time);
            outputEntry.setCompressedSize(-1);
            outputJarStream.putNextEntry(outputEntry);
            outputJarStream.write(t.data);
        }

        @Override
        public void close() throws IOException {
            outputJarStream.close();
        }
    }

    class FSWriter implements DirWriter {
        private final Path path;

        public FSWriter(Path path) {
            this.path = path;
        }

        @Override
        public void mkDirs(String dirs) throws IOException {
            //noinspection ResultOfMethodCallIgnored
            path.resolve(dirs).toFile().mkdirs();
        }

        @Override
        public void writeFile(Transformable t) throws IOException {
            mkdirs(t);

            Path tgt = path.resolve(t.name);
            try(FileOutputStream fs = new FileOutputStream(tgt.toFile(), true)) {
                fs.write(t.data);
            }
        }

        @Override
        public void close() throws IOException {
            // nop
        }
    }
}
