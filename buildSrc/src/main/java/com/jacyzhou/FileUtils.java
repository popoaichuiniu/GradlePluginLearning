package com.jacyzhou;

import com.android.tools.r8.graph.F;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtils {
    public static byte[] readFile(File file) throws IOException {

        ByteArrayOutputStream bufferOut = new ByteArrayOutputStream();
        BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file));
        byte[] buffer = new byte[1024];
        int len = -1;
        while ((len = bufferedInputStream.read(buffer)) != -1) {
            bufferOut.write(buffer, 0, len);
        }
        return bufferOut.toByteArray();
    }

    public static void writeFile(byte[] content, String path) throws IOException {
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
        BufferedOutputStream bufferedInputStream = new BufferedOutputStream(new FileOutputStream(file));
        bufferedInputStream.write(content);
        bufferedInputStream.close();
    }

    public static void copyDirectory(String sourceDirectoryLocation, String destinationDirectoryLocation)
            throws IOException {
        Files.walk(Paths.get(sourceDirectoryLocation))
                .forEach(source -> {
                    Path destination = Paths.get(destinationDirectoryLocation, source.toString()
                            .substring(sourceDirectoryLocation.length()));
                    try {
                        Files.copy(source, destination);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }
}
