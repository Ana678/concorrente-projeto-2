package application.interceptors;

import java.util.Arrays;
import middleware.extension.InvocationContext;
import middleware.extension.InvocationInterceptor;
import middleware.util.Log;

public class LoggingInterceptor implements InvocationInterceptor {

    @Override
    public void beforeInvocation(InvocationContext context) throws Exception {
        String methodName = context.getAbsoluteObjectReference().getMethod().getName();
        String path = context.getFullPath();
        String params = Arrays.toString(context.getMethodParameters());

        Log.info("LoggingInterceptor",
                 "Iniciando requisição -> Caminho: [%s] Metodo: [%s] Parâmetros: %s",
                 path, methodName, params);
    }

    @Override
    public void afterInvocation(InvocationContext context) {
        String methodName = context.getAbsoluteObjectReference().getMethod().getName();

        if (context.hasException()) {
            Exception e = context.getException();
            Log.error("LoggingInterceptor",
                      String.format("Requisição finalizada com ERRO -> Metodo: [%s] Exceção: %s", methodName, e.getMessage()), e);
        } else {
            Log.info("LoggingInterceptor",
                     "Requisição finalizada com SUCESSO -> Metodo: [%s]",
                     methodName);
        }
    }
}
