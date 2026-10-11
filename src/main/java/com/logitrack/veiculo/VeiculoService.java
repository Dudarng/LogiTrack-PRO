package com.logitrack.veiculo;

import com.logitrack.veiculo.dto.VeiculoResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VeiculoService {

    private final VeiculoRepository veiculoRepository;

    public List<VeiculoResponse> listarTodos() {
        return veiculoRepository.findAllByOrderByPlacaAsc().stream()
                .map(VeiculoResponse::fromEntity)
                .toList();
    }
}
