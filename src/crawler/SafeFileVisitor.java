package crawler;

import handler.FileHandler;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class SafeFileVisitor extends SimpleFileVisitor<Path> {

    private final FileHandler fileHandler;
    public SafeFileVisitor(FileHandler fileHandler) {
        this.fileHandler = fileHandler;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        fileHandler.handle(file);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc){
        if (exc instanceof AccessDeniedException) {
            return FileVisitResult.CONTINUE;
        }

        System.err.println("Error accessing file: " + file + " - " + exc.getMessage());
        return FileVisitResult.CONTINUE;
    }
}
