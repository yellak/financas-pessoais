package br.com.financas.backend.common;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.event.Level;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerMapping;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequisicaoLogFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequisicaoLogFilter.class);
    private static final String CABECALHO_ID = "X-Request-ID";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest requisicao) {
        String caminho = requisicao.getRequestURI().substring(requisicao.getContextPath().length());
        return !caminho.equals("/api") && !caminho.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requisicao, HttpServletResponse resposta, FilterChain cadeia)
            throws ServletException, IOException {
        long inicio = System.nanoTime();
        String requestId = UUID.randomUUID().toString();
        resposta.setHeader(CABECALHO_ID, requestId);

        try (var contexto = MDC.putCloseable("requestId", requestId)) {
            Throwable falha = null;
            try {
                cadeia.doFilter(requisicao, resposta);
            } catch (ServletException | IOException | RuntimeException exception) {
                falha = exception;
                throw exception;
            } finally {
                int status = falha != null && !resposta.isCommitted()
                        ? HttpServletResponse.SC_INTERNAL_SERVER_ERROR
                        : resposta.getStatus();
                if (falha == null && requisicao.getAttribute(DispatcherServlet.EXCEPTION_ATTRIBUTE) instanceof Throwable exception) {
                    falha = exception;
                }
                registrar(requisicao, status, inicio, falha);
            }
        }
    }

    private void registrar(HttpServletRequest requisicao, int status, long inicio, Throwable falha) {
        Object rota = requisicao.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        long duracaoMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio);
        var evento = log.atLevel(status >= 500 ? Level.ERROR : Level.INFO)
                .addKeyValue("metodo", requisicao.getMethod())
                .addKeyValue("rota", rota == null ? "sem-rota" : rota.toString())
                .addKeyValue("status", status)
                .addKeyValue("duracaoMs", duracaoMs);

        if (status >= 500 && falha != null) {
            Throwable causa = NestedExceptionUtils.getMostSpecificCause(falha);
            // Mensagens de exceção podem incluir valores enviados pelo usuário.
            evento.addKeyValue("tipoExcecao", causa.getClass().getName())
                    .addKeyValue("stackTrace", () -> Arrays.stream(causa.getStackTrace())
                            .map(quadro -> "\tat " + quadro)
                            .collect(Collectors.joining("\n")));
        }

        evento.log("Requisição HTTP finalizada");
    }
}
