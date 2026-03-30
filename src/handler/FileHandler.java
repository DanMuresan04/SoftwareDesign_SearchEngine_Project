package handler;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

public interface FileHandler {
    void handle(Path filePath, BasicFileAttributes attrs);
}
