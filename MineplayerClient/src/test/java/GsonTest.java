import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

public class GsonTest {

    @Test
    public void testGson() {
        JsonObject serverEnvInit = new JsonObject();
        JsonObject serverEnvInitBody = new JsonObject();
        serverEnvInit.addProperty("context", "init");
        serverEnvInitBody.addProperty("env_type", "woodbreak");
        serverEnvInitBody.add("props", null);
        serverEnvInit.add("body", serverEnvInitBody);

        System.out.println(new Gson().toJson(serverEnvInit));
    }
}
