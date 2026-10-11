package com.logitrack.dashboard;

import com.logitrack.dashboard.dto.DashboardResponse;
import com.logitrack.manutencao.exception.EntidadeNaoEncontradaException;
import com.logitrack.veiculo.VeiculoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final VeiculoService veiculoService;

    @GetMapping
    public String index(
            @RequestParam(name = "veiculoId", required = false) Integer veiculoId,
            Model model) {

        model.addAttribute("veiculos", veiculoService.listarTodos());

        DashboardResponse dashboard;
        try {
            dashboard = dashboardService.obterDashboard(veiculoId);
        } catch (EntidadeNaoEncontradaException ex) {
            model.addAttribute("mensagemAlerta", "Veículo informado não foi encontrado. Exibindo métricas de toda a frota.");
            dashboard = dashboardService.obterDashboard(null);
        }

        model.addAttribute("dashboard", dashboard);
        return "dashboard/index";
    }
}
