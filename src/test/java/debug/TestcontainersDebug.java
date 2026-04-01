package debug;

import org.testcontainers.DockerClientFactory;
import com.github.dockerjava.api.DockerClient;

public class TestcontainersDebug {
    public static void main(String[] args) {
        System.out.println("Starting Testcontainers Debug...");
        try {
            DockerClient client = DockerClientFactory.instance().client();
            System.out.println("SUCCESS: Connected to Docker!");
            System.out.println("Docker Version: " + client.versionCmd().exec().getVersion());
        } catch (Exception e) {
            System.out.println("FATAL CONNECTION ERROR:");
            e.printStackTrace();
            
            Throwable t = e.getCause();
            while (t != null) {
                System.out.println("Caused by:");
                t.printStackTrace();
                t = t.getCause();
            }
        }
    }
}
