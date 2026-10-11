package com.logitrack.config;

import com.logitrack.manutencao.exception.EntidadeNaoEncontradaException;
import com.logitrack.manutencao.exception.OperacaoNaoPermitidaException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntidadeNaoEncontradaException.class)
    public String handleEntidadeNaoEncontrada(EntidadeNaoEncontradaException ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("mensagemErro", ex.getMessage());
        return "redirect:/manutencoes";
    }

    @ExceptionHandler(OperacaoNaoPermitidaException.class)
    public String handleOperacaoNaoPermitida(OperacaoNaoPermitidaException ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("mensagemErro", ex.getMessage());
        return "redirect:/manutencoes";
    }
}
