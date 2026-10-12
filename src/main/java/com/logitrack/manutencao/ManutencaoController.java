package com.logitrack.manutencao;

import com.logitrack.manutencao.dto.ManutencaoRequest;
import com.logitrack.manutencao.dto.ManutencaoResponse;
import com.logitrack.manutencao.exception.RegraNegocioException;
import com.logitrack.veiculo.VeiculoService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/manutencoes")
@RequiredArgsConstructor
public class ManutencaoController {

    private final ManutencaoService manutencaoService;
    private final VeiculoService veiculoService;

    @GetMapping
    public String listar(
            @PageableDefault(size = 10, sort = {"dataInicio", "id"}, direction = Sort.Direction.ASC) Pageable pageable,
            Model model) {
        Page<ManutencaoResponse> pagina = manutencaoService.listarPaginado(pageable);
        model.addAttribute("pagina", pagina);
        model.addAttribute("veiculos", veiculoService.listarTodos());
        model.addAttribute("statusList", StatusManutencao.values());
        return "manutencao/lista";
    }

    @GetMapping("/novo")
    public String novoFormulario(Model model) {
        ManutencaoRequest request = new ManutencaoRequest();
        request.setStatus(StatusManutencao.PENDENTE);
        request.setDataInicio(LocalDate.now());

        model.addAttribute("manutencaoRequest", request);
        carregarDadosFormulario(model, false, null);
        return "manutencao/formulario";
    }

    @PostMapping
    public String salvar(
            @Valid @ModelAttribute("manutencaoRequest") ManutencaoRequest request,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            carregarDadosFormulario(model, false, null);
            return "manutencao/formulario";
        }

        try {
            manutencaoService.criar(request);
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Manutenção agendada com sucesso!");
            return "redirect:/manutencoes";
        } catch (RegraNegocioException ex) {
            if (ex.getCampo() != null) {
                result.rejectValue(ex.getCampo(), "erroRegra", ex.getMessage());
            } else {
                result.reject("erroRegra", ex.getMessage());
            }
            carregarDadosFormulario(model, false, null);
            return "manutencao/formulario";
        }
    }

    @GetMapping("/{id}/editar")
    public String editarFormulario(@PathVariable("id") Integer id, Model model) {
        ManutencaoRequest request = manutencaoService.buscarParaEdicao(id);
        model.addAttribute("manutencaoRequest", request);
        carregarDadosFormulario(model, true, id);
        return "manutencao/formulario";
    }

    @PostMapping("/{id}")
    public String atualizar(
            @PathVariable("id") Integer id,
            @Valid @ModelAttribute("manutencaoRequest") ManutencaoRequest request,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            carregarDadosFormulario(model, true, id);
            return "manutencao/formulario";
        }

        try {
            manutencaoService.atualizar(id, request);
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Manutenção atualizada com sucesso!");
            return "redirect:/manutencoes";
        } catch (RegraNegocioException ex) {
            if (ex.getCampo() != null) {
                result.rejectValue(ex.getCampo(), "erroRegra", ex.getMessage());
            } else {
                result.reject("erroRegra", ex.getMessage());
            }
            carregarDadosFormulario(model, true, id);
            return "manutencao/formulario";
        }
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        manutencaoService.excluir(id);
        redirectAttributes.addFlashAttribute("mensagemSucesso", "Manutenção excluída com sucesso!");
        return "redirect:/manutencoes";
    }

    private void carregarDadosFormulario(Model model, boolean modoEdicao, Integer id) {
        model.addAttribute("veiculos", veiculoService.listarTodos());
        model.addAttribute("statusList", StatusManutencao.values());
        model.addAttribute("modoEdicao", modoEdicao);
        model.addAttribute("manutencaoId", id);
    }
}
