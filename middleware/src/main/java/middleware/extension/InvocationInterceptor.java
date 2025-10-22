package middleware.extension;

public interface InvocationInterceptor {

    void beforeInvocation(InvocationContext context) throws Exception;
    void afterInvocation(InvocationContext context);
}
