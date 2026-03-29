package handler;
import java.nio.file.Path;

public class ConsoleFileHandler implements FileHandler {
    @Override
    public void handle(Path path) {
        if (path != null){
            System.out.print("Found" + path.toString() + '\n');
        }
    }
}
