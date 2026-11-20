package br.com.financas.backend.common;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import br.com.financas.backend.categoria.CategoriaNaoEncontradaException;
import br.com.financas.backend.categoria.NomeCategoriaDuplicadoException;
import org.springframework.context.MessageSource;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private final MessageSource mensagens;

    public ApiExceptionHandler(MessageSource mensagens) {
        this.mensagens = mensagens;
    }

    @ExceptionHandler(CategoriaNaoEncontradaException.class)
    public ProblemDetail naoEncontrada(CategoriaNaoEncontradaException exception, Locale locale) {
        return problema(HttpStatus.NOT_FOUND, "CATEGORIA_NAO_ENCONTRADA", "categoria.naoEncontrada",
                locale, exception.getId().toString());
    }

    @ExceptionHandler(NomeCategoriaDuplicadoException.class)
    public ProblemDetail nomeDuplicado(Locale locale) {
        return problema(HttpStatus.CONFLICT, "CATEGORIA_NOME_DUPLICADO", "categoria.nomeDuplicado", locale);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail conflitoDeDados(Locale locale) {
        return problema(HttpStatus.CONFLICT, "CONFLITO_DADOS", "erro.conflitoDados", locale);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validacao(MethodArgumentNotValidException exception, Locale locale) {
        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "DADOS_INVALIDOS", "erro.validacao", locale);
        Map<String, String> erros = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(erro -> erros.putIfAbsent(erro.getField(), erro.getDefaultMessage()));
        problema.setProperty("erros", erros);
        return problema;
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ProblemDetail requisicaoInvalida(Locale locale) {
        return problema(HttpStatus.BAD_REQUEST, "REQUISICAO_INVALIDA", "erro.requisicaoInvalida", locale);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail erroInterno(Exception exception, Locale locale) throws Exception {
        if (exception instanceof ErrorResponse
                || AnnotatedElementUtils.hasAnnotation(exception.getClass(), ResponseStatus.class)) {
            throw exception;
        }
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "ERRO_INTERNO", "erro.interno", locale);
    }

    private ProblemDetail problema(HttpStatus status, String codigo, String chave, Locale locale, Object... argumentos) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status,
                mensagens.getMessage(chave + ".detalhe", argumentos, locale));
        problema.setTitle(mensagens.getMessage(chave + ".titulo", null, locale));
        problema.setProperty("codigo", codigo);
        return problema;
    }
}
