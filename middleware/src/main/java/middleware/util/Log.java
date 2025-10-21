package middleware.util;

public final class Log {

    private Log() {}

    public static void info(String origin, String message) {
        System.out.println("[" + origin + "] " + message);
    }

    public static void info(String origin, String message, Object... args) {
        System.out.println("[" + origin + "] " + String.format(message, args));
    }

    public static void error(String origin, String message) {
        System.err.println("[" + origin + "] " + message);
    }

    public static void error(String origin, String message, Throwable t) {
        System.err.println("[" + origin + "] " + message);
        if (t != null) {
            t.printStackTrace(System.err);
        }
    }
}
