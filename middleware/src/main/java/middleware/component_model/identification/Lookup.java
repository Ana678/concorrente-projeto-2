package middleware.component_model.identification;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import middleware.util.Log;

public class Lookup {

    private final Map<String, AbsoluteObjectReference> registry = new ConcurrentHashMap<>();

    public void bind(String fullPath, AbsoluteObjectReference aor) {
        Log.info("Lookup", "Binding path: %s", fullPath);
        registry.put(fullPath, aor);
    }

    public AbsoluteObjectReference find(String fullPath) {
        return registry.get(fullPath);
    }
}
