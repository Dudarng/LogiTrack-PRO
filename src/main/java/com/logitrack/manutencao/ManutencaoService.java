package com.logitrack.manutencao;

import com.logitrack.manutencao.dto.ManutencaoRequest;
import com.logitrack.manutencao.dto.ManutencaoResponse;
import com.logitrack.manutencao.exception.EntidadeNaoEncontradaException;
import com.logitrack.manutencao.exception.OperacaoNaoPermitidaException;
import com.logitrack.manutencao.exception.RegraNegocioException;
import com.logitrack.veiculo.Veiculo;
import com.logitrack.veiculo.VeiculoRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ManutencaoService {

    private final ManutencaoRepository manutencaoRepository;
    private final VeiculoRepository veiculoRepository;

    @Transactional(readOnly = true)
    public Page<ManutencaoResponse> listarPaginado(Pageable pageable) {
        return manutencaoRepository.findAll(pageable)
                .map(ManutencaoResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public ManutencaoResponse buscarPorId(Integer id) {
        Manutencao manutencao = manutencaoRepository.findComVeiculoById(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Manutenção com ID " + id + " não encontrada."));
        return ManutencaoResponse.fromEntity(manutencao);
    }

    @Transactional(readOnly = true)
    public ManutencaoRequest buscarParaEdicao(Integer id) {
        Manutencao manutencao = manutencaoRepository.findComVeiculoById(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Manutenção com ID " + id + " não encontrada."));

        if (manutencao.getStatus() == StatusManutencao.CONCLUIDA) {
            throw new OperacaoNaoPermitidaException("Manutenções com status CONCLUÍDA não podem ser alteradas.");
        }

        return ManutencaoResponse.fromEntity(manutencao).toRequest();
    }

    @Transactional
    public ManutencaoResponse criar(ManutencaoRequest request) {
        validarRegras(request);

        Veiculo veiculo = veiculoRepository.findById(request.getVeiculoId())
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Veículo com ID " + request.getVeiculoId() + " não encontrado."));

        Manutencao manutencao = new Manutencao();
        manutencao.setVeiculo(veiculo);
        manutencao.setDataInicio(request.getDataInicio());
        manutencao.setDataFinalizacao(request.getDataFinalizacao());
        manutencao.setTipoServico(request.getTipoServico());
        manutencao.setCustoEstimado(request.getCustoEstimado());
        manutencao.setStatus(request.getStatus());

        Manutencao salva = manutencaoRepository.save(manutencao);
        return ManutencaoResponse.fromEntity(salva);
    }

    @Transactional
    public ManutencaoResponse atualizar(Integer id, ManutencaoRequest request) {
        Manutencao manutencao = manutencaoRepository.findById(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Manutenção com ID " + id + " não encontrada."));

        if (manutencao.getStatus() == StatusManutencao.CONCLUIDA) {
            throw new OperacaoNaoPermitidaException("Manutenções com status CONCLUÍDA não podem ser alteradas.");
        }

        validarRegras(request);

        Veiculo veiculo = veiculoRepository.findById(request.getVeiculoId())
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Veículo com ID " + request.getVeiculoId() + " não encontrado."));

        manutencao.setVeiculo(veiculo);
        manutencao.setDataInicio(request.getDataInicio());
        manutencao.setDataFinalizacao(request.getDataFinalizacao());
        manutencao.setTipoServico(request.getTipoServico());
        manutencao.setCustoEstimado(request.getCustoEstimado());
        manutencao.setStatus(request.getStatus());

        Manutencao atualizada = manutencaoRepository.save(manutencao);
        return ManutencaoResponse.fromEntity(atualizada);
    }

    @Transactional
    public void excluir(Integer id) {
        Manutencao manutencao = manutencaoRepository.findById(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Manutenção com ID " + id + " não encontrada."));

        if (manutencao.getStatus() == StatusManutencao.CONCLUIDA) {
            throw new OperacaoNaoPermitidaException("Manutenções com status CONCLUÍDA não podem ser excluídas.");
        }

        manutencaoRepository.delete(manutencao);
    }

    private void validarRegras(ManutencaoRequest request) {
        if (request.getDataFinalizacao() != null && request.getDataInicio() != null) {
            if (request.getDataFinalizacao().isBefore(request.getDataInicio())) {
                throw new RegraNegocioException("dataFinalizacao", "A data de finalização não pode ser anterior à data de início.");
            }
        }

        if (request.getCustoEstimado() != null && request.getCustoEstimado().compareTo(BigDecimal.ZERO) < 0) {
            throw new RegraNegocioException("custoEstimado", "O custo estimado não pode ser negativo.");
        }
    }
}
