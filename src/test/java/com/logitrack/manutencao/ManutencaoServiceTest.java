package com.logitrack.manutencao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.logitrack.manutencao.dto.ManutencaoRequest;
import com.logitrack.manutencao.dto.ManutencaoResponse;
import com.logitrack.manutencao.exception.EntidadeNaoEncontradaException;
import com.logitrack.manutencao.exception.OperacaoNaoPermitidaException;
import com.logitrack.manutencao.exception.RegraNegocioException;
import com.logitrack.veiculo.TipoVeiculo;
import com.logitrack.veiculo.Veiculo;
import com.logitrack.veiculo.VeiculoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ManutencaoServiceTest {

    @Mock
    private ManutencaoRepository manutencaoRepository;

    @Mock
    private VeiculoRepository veiculoRepository;

    @InjectMocks
    private ManutencaoService manutencaoService;

    private Veiculo veiculo;
    private Manutencao manutencaoPendente;
    private Manutencao manutencaoConcluida;

    @BeforeEach
    void setUp() {
        veiculo = new Veiculo(1, "ABC-1234", "Fiorino", TipoVeiculo.LEVE, 2022);

        manutencaoPendente = new Manutencao(
                10,
                veiculo,
                LocalDate.of(2026, 10, 15),
                LocalDate.of(2026, 10, 16),
                "Troca de Pastilhas",
                new BigDecimal("450.00"),
                StatusManutencao.PENDENTE
        );

        manutencaoConcluida = new Manutencao(
                20,
                veiculo,
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 2),
                "Revisão Completa",
                new BigDecimal("1200.00"),
                StatusManutencao.CONCLUIDA
        );
    }

    @Test
    @DisplayName("Deve criar manutenção com sucesso quando todos os dados são válidos")
    void deveCriarManutencaoComSucesso() {
        ManutencaoRequest request = ManutencaoRequest.builder()
                .veiculoId(1)
                .dataInicio(LocalDate.of(2026, 10, 20))
                .dataFinalizacao(LocalDate.of(2026, 10, 22))
                .tipoServico("Troca de Óleo")
                .custoEstimado(new BigDecimal("350.00"))
                .status(StatusManutencao.PENDENTE)
                .build();

        when(veiculoRepository.findById(1)).thenReturn(Optional.of(veiculo));
        when(manutencaoRepository.save(any(Manutencao.class))).thenAnswer(invocation -> {
            Manutencao m = invocation.getArgument(0);
            m.setId(100);
            return m;
        });

        ManutencaoResponse response = manutencaoService.criar(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100);
        assertThat(response.getVeiculoPlaca()).isEqualTo("ABC-1234");
        assertThat(response.getTipoServico()).isEqualTo("Troca de Óleo");
        assertThat(response.getStatus()).isEqualTo(StatusManutencao.PENDENTE);

        verify(manutencaoRepository).save(any(Manutencao.class));
    }

    @Test
    @DisplayName("Deve permitir criar manutenção quando dataFinalizacao for nula")
    void deveCriarComDataFinalizacaoNula() {
        ManutencaoRequest request = ManutencaoRequest.builder()
                .veiculoId(1)
                .dataInicio(LocalDate.of(2026, 10, 20))
                .dataFinalizacao(null)
                .tipoServico("Inspeção Inicial")
                .custoEstimado(new BigDecimal("200.00"))
                .status(StatusManutencao.EM_REALIZACAO)
                .build();

        when(veiculoRepository.findById(1)).thenReturn(Optional.of(veiculo));
        when(manutencaoRepository.save(any(Manutencao.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ManutencaoResponse response = manutencaoService.criar(request);

        assertThat(response).isNotNull();
        assertThat(response.getDataFinalizacao()).isNull();
        verify(manutencaoRepository).save(any(Manutencao.class));
    }

    @Test
    @DisplayName("Deve lançar RegraNegocioException ao tentar criar com dataFinalizacao anterior a dataInicio")
    void deveLancarExcecaoAoCriarComDataFinalizacaoAnteriorADataInicio() {
        ManutencaoRequest request = ManutencaoRequest.builder()
                .veiculoId(1)
                .dataInicio(LocalDate.of(2026, 10, 20))
                .dataFinalizacao(LocalDate.of(2026, 10, 19))
                .tipoServico("Troca de Óleo")
                .custoEstimado(new BigDecimal("350.00"))
                .status(StatusManutencao.PENDENTE)
                .build();

        RegraNegocioException ex = assertThrows(RegraNegocioException.class, () -> manutencaoService.criar(request));

        assertThat(ex.getCampo()).isEqualTo("dataFinalizacao");
        assertThat(ex.getMessage()).contains("não pode ser anterior à data de início");
        verify(manutencaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar RegraNegocioException ao tentar criar com custoEstimado negativo")
    void deveLancarExcecaoAoCriarComCustoNegativo() {
        ManutencaoRequest request = ManutencaoRequest.builder()
                .veiculoId(1)
                .dataInicio(LocalDate.of(2026, 10, 20))
                .dataFinalizacao(LocalDate.of(2026, 10, 21))
                .tipoServico("Troca de Óleo")
                .custoEstimado(new BigDecimal("-50.00"))
                .status(StatusManutencao.PENDENTE)
                .build();

        RegraNegocioException ex = assertThrows(RegraNegocioException.class, () -> manutencaoService.criar(request));

        assertThat(ex.getCampo()).isEqualTo("custoEstimado");
        assertThat(ex.getMessage()).contains("não pode ser negativo");
        verify(manutencaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar EntidadeNaoEncontradaException ao criar com veiculo inexistente")
    void deveLancarExcecaoAoCriarComVeiculoInexistente() {
        ManutencaoRequest request = ManutencaoRequest.builder()
                .veiculoId(999)
                .dataInicio(LocalDate.of(2026, 10, 20))
                .tipoServico("Alinhamento")
                .custoEstimado(new BigDecimal("150.00"))
                .status(StatusManutencao.PENDENTE)
                .build();

        when(veiculoRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(EntidadeNaoEncontradaException.class, () -> manutencaoService.criar(request));
        verify(manutencaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve atualizar manutenção com sucesso quando dados são válidos e status não é CONCLUIDA")
    void deveAtualizarManutencaoComSucesso() {
        ManutencaoRequest request = ManutencaoRequest.builder()
                .veiculoId(1)
                .dataInicio(LocalDate.of(2026, 10, 15))
                .dataFinalizacao(LocalDate.of(2026, 10, 18))
                .tipoServico("Troca de Pastilhas e Discos")
                .custoEstimado(new BigDecimal("600.00"))
                .status(StatusManutencao.EM_REALIZACAO)
                .build();

        when(manutencaoRepository.findById(10)).thenReturn(Optional.of(manutencaoPendente));
        when(veiculoRepository.findById(1)).thenReturn(Optional.of(veiculo));
        when(manutencaoRepository.save(any(Manutencao.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ManutencaoResponse response = manutencaoService.atualizar(10, request);

        assertThat(response).isNotNull();
        assertThat(response.getTipoServico()).isEqualTo("Troca de Pastilhas e Discos");
        assertThat(response.getCustoEstimado()).isEqualByComparingTo(new BigDecimal("600.00"));
        assertThat(response.getStatus()).isEqualTo(StatusManutencao.EM_REALIZACAO);
        verify(manutencaoRepository).save(manutencaoPendente);
    }

    @Test
    @DisplayName("Deve lançar OperacaoNaoPermitidaException ao tentar atualizar manutenção com status CONCLUIDA")
    void deveLancarExcecaoAoAtualizarManutencaoConcluida() {
        ManutencaoRequest request = ManutencaoRequest.builder()
                .veiculoId(1)
                .dataInicio(LocalDate.of(2026, 10, 1))
                .dataFinalizacao(LocalDate.of(2026, 10, 2))
                .tipoServico("Alteração indevida")
                .custoEstimado(new BigDecimal("1200.00"))
                .status(StatusManutencao.CONCLUIDA)
                .build();

        when(manutencaoRepository.findById(20)).thenReturn(Optional.of(manutencaoConcluida));

        OperacaoNaoPermitidaException ex = assertThrows(OperacaoNaoPermitidaException.class,
                () -> manutencaoService.atualizar(20, request));

        assertThat(ex.getMessage()).contains("CONCLUÍDA não podem ser alteradas");
        verify(manutencaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar EntidadeNaoEncontradaException ao atualizar manutenção inexistente")
    void deveLancarExcecaoAoAtualizarManutencaoInexistente() {
        ManutencaoRequest request = ManutencaoRequest.builder()
                .veiculoId(1)
                .dataInicio(LocalDate.of(2026, 10, 1))
                .tipoServico("Teste")
                .custoEstimado(BigDecimal.ZERO)
                .status(StatusManutencao.PENDENTE)
                .build();

        when(manutencaoRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(EntidadeNaoEncontradaException.class, () -> manutencaoService.atualizar(999, request));
        verify(manutencaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar RegraNegocioException ao atualizar com dataFinalizacao anterior a dataInicio")
    void deveLancarExcecaoAoAtualizarComDataFinalizacaoInvalida() {
        ManutencaoRequest request = ManutencaoRequest.builder()
                .veiculoId(1)
                .dataInicio(LocalDate.of(2026, 10, 15))
                .dataFinalizacao(LocalDate.of(2026, 10, 10))
                .tipoServico("Troca de Pastilhas")
                .custoEstimado(new BigDecimal("450.00"))
                .status(StatusManutencao.PENDENTE)
                .build();

        when(manutencaoRepository.findById(10)).thenReturn(Optional.of(manutencaoPendente));

        assertThrows(RegraNegocioException.class, () -> manutencaoService.atualizar(10, request));
        verify(manutencaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve excluir manutenção com sucesso quando status não é CONCLUIDA")
    void deveExcluirManutencaoComSucesso() {
        when(manutencaoRepository.findById(10)).thenReturn(Optional.of(manutencaoPendente));

        manutencaoService.excluir(10);

        verify(manutencaoRepository).delete(manutencaoPendente);
    }

    @Test
    @DisplayName("Deve lançar OperacaoNaoPermitidaException ao tentar excluir manutenção com status CONCLUIDA")
    void deveLancarExcecaoAoExcluirManutencaoConcluida() {
        when(manutencaoRepository.findById(20)).thenReturn(Optional.of(manutencaoConcluida));

        OperacaoNaoPermitidaException ex = assertThrows(OperacaoNaoPermitidaException.class,
                () -> manutencaoService.excluir(20));

        assertThat(ex.getMessage()).contains("CONCLUÍDA não podem ser excluídas");
        verify(manutencaoRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Deve lançar EntidadeNaoEncontradaException ao excluir manutenção inexistente")
    void deveLancarExcecaoAoExcluirManutencaoInexistente() {
        when(manutencaoRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(EntidadeNaoEncontradaException.class, () -> manutencaoService.excluir(999));
        verify(manutencaoRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Deve buscar manutenção por ID com sucesso")
    void deveBuscarPorIdComSucesso() {
        when(manutencaoRepository.findComVeiculoById(10)).thenReturn(Optional.of(manutencaoPendente));

        ManutencaoResponse response = manutencaoService.buscarPorId(10);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10);
        assertThat(response.getVeiculoPlaca()).isEqualTo("ABC-1234");
    }

    @Test
    @DisplayName("Deve buscar para edição com sucesso quando status não é CONCLUIDA")
    void deveBuscarParaEdicaoComSucesso() {
        when(manutencaoRepository.findComVeiculoById(10)).thenReturn(Optional.of(manutencaoPendente));

        ManutencaoRequest request = manutencaoService.buscarParaEdicao(10);

        assertThat(request).isNotNull();
        assertThat(request.getVeiculoId()).isEqualTo(1);
        assertThat(request.getTipoServico()).isEqualTo("Troca de Pastilhas");
    }

    @Test
    @DisplayName("Deve lançar OperacaoNaoPermitidaException ao buscar para edição manutenção CONCLUIDA")
    void deveLancarExcecaoAoBuscarParaEdicaoManutencaoConcluida() {
        when(manutencaoRepository.findComVeiculoById(20)).thenReturn(Optional.of(manutencaoConcluida));

        assertThrows(OperacaoNaoPermitidaException.class, () -> manutencaoService.buscarParaEdicao(20));
    }

    @Test
    @DisplayName("Deve listar manutenções paginadas retornando Page de ManutencaoResponse")
    void deveListarManutencoesPaginadas() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Manutencao> page = new PageImpl<>(List.of(manutencaoPendente, manutencaoConcluida), pageable, 2);

        when(manutencaoRepository.findAll(pageable)).thenReturn(page);

        Page<ManutencaoResponse> resultado = manutencaoService.listarPaginado(pageable);

        assertThat(resultado).hasSize(2);
        assertThat(resultado.getContent().get(0).getVeiculoPlaca()).isEqualTo("ABC-1234");
        assertThat(resultado.getContent().get(1).isConcluida()).isTrue();
    }
}
