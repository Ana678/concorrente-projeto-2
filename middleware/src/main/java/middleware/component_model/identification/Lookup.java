package middleware.component_model.identification;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Lookup {

    private final Map<String, AbsoluteObjectReference> registry = new ConcurrentHashMap<>();

    public void bind(String fullPath, AbsoluteObjectReference aor) {
        System.out.println("Binding path: " + fullPath);
        registry.put(fullPath, aor);
    }

    public AbsoluteObjectReference find(String fullPath) {
        return registry.get(fullPath);
    }
}
