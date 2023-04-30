import io.github.jefflegendpower.mineplayerclient.env.EnvTCPServer;
import org.junit.jupiter.api.Test;

public class EnvTCPTest {

    @Test
    public void testTCPConnection() {
        EnvTCPServer tcp = new EnvTCPServer();
        try {
            System.out.println("Starting TCP server");
            tcp.start(444, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("TCP server stopped");
    }
}
