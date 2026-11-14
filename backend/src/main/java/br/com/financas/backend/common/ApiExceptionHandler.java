package br.com.financas.backend.common;

import java.util.LinkedHashMap;
import java.util.Map;

import br.com.financas.backend.categoria.CategoriaNaoEncontradaException;
import br.com.financas.backend.categoria.NomeCategoriaDuplicadoException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(CategoriaNaoEncontradaException.class)
    public ProblemDetail naoEncontrada(CategoriaNaoEncontradaException exception) {
        return problema(HttpStatus.NOT_FOUND, "Categoria não encontrada", exception.getMessage());
    }

    @ExceptionHandler(NomeCategoriaDuplicadoException.class)
    public ProblemDetail nomeDuplicado(NomeCategoriaDuplicadoException exception) {
        return problema(HttpStatus.CONFLICT, "Nome já utilizado", exception.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail conflitoDeDados() {
        return problema(HttpStatus.CONFLICT, "Conflito de dados",
                "Não foi possível concluir a operação. Verifique se o nome já está em uso.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validacao(MethodArgumentNotValidException exception) {
        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "Dados inválidos",
                "Revise os campos informados.");
        Map<String, String> erros = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(erro -> erros.putIfAbsent(erro.getField(), erro.getDefaultMessage()));
        problema.setProperty("erros", erros);
        return problema;
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ProblemDetail requisicaoInvalida() {
        return problema(HttpStatus.BAD_REQUEST, "Requisição inválida",
                "Verifique o JSON e os tipos dos valores informados.");
    }

    private ProblemDetail problema(HttpStatus status, String titulo, String detalhe) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        return problema;
    }
}
