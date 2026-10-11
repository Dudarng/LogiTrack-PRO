package com.logitrack.dashboard;

import com.logitrack.dashboard.projection.CronogramaManutencaoProjection;
import com.logitrack.dashboard.projection.RankingUtilizacaoProjection;
import com.logitrack.dashboard.projection.VolumeCategoriaProjection;
import com.logitrack.veiculo.Veiculo;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

@org.springframework.stereotype.Repository
public interface DashboardRepository extends Repository<Veiculo, Integer> {

    /**
     * Métrica 1: Total de KM percorrido em viagens realizadas (data_chegada IS NOT NULL),
     * com filtro opcional por veículo. O CAST garante compatibilidade de tipo com o PostgreSQL.
     */
    @Query(value = """
        SELECT COALESCE(SUM(km_percorrida), 0)
        FROM viagens
        WHERE data_chegada IS NOT NULL
          AND (CAST(:veiculoId AS INTEGER) IS NULL OR veiculo_id = CAST(:veiculoId AS INTEGER))
        """, nativeQuery = true)
    BigDecimal findTotalKm(@Param("veiculoId") Integer veiculoId);

    /**
     * Métrica 2: Volume por Categoria (Leve vs Pesado) de viagens realizadas.
     * Utiliza CTE de categorias para garantir que LEVE e PESADO sempre existam,
     * calculando o percentual e o total geral via window function SQL sem divisão por zero.
     */
    @Query(value = """
        SELECT 
            c.tipo AS "tipo", 
            COUNT(v.id) AS "totalViagens",
            CAST(COALESCE(SUM(COUNT(v.id)) OVER (), 0) AS BIGINT) AS "totalGeral",
            COALESCE(ROUND(100.0 * COUNT(v.id) / NULLIF(SUM(COUNT(v.id)) OVER (), 0), 1), 0) AS "percentual"
        FROM (VALUES ('LEVE'), ('PESADO')) AS c(tipo)
        LEFT JOIN veiculos ve ON ve.tipo = c.tipo
        LEFT JOIN viagens v ON v.veiculo_id = ve.id AND v.data_chegada IS NOT NULL
        GROUP BY c.tipo
        ORDER BY c.tipo
        """, nativeQuery = true)
    List<VolumeCategoriaProjection> findVolumePorCategoria();

    /**
     * Métrica 3: Cronograma das próximas 5 manutenções agendadas.
     * Filtra manutenções não concluídas (status <> 'CONCLUIDA') com data_inicio >= CURRENT_DATE,
     * ordenadas por data de início ascendente e desempate por id, limitadas a 5 registros.
     */
    @Query(value = """
        SELECT 
            m.id AS "id",
            m.data_inicio AS "dataInicio",
            m.data_finalizacao AS "dataFinalizacao",
            m.tipo_servico AS "tipoServico",
            m.custo_estimado AS "custoEstimado",
            m.status AS "status",
            v.id AS "veiculoId",
            v.placa AS "veiculoPlaca",
            v.modelo AS "veiculoModelo"
        FROM manutencoes m
        JOIN veiculos v ON m.veiculo_id = v.id
        WHERE m.status <> 'CONCLUIDA'
          AND m.data_inicio >= CURRENT_DATE
        ORDER BY m.data_inicio ASC, m.id ASC
        LIMIT 5
        """, nativeQuery = true)
    List<CronogramaManutencaoProjection> findCronogramaManutencoes();

    /**
     * Métrica 4: Ranking de Utilização da frota.
     * Identifica o veículo com a maior soma acumulada de quilometragem em viagens realizadas.
     * Desempata por veiculo_id ascendente e limita a 1 registro.
     */
    @Query(value = """
        SELECT 
            v.id AS "veiculoId",
            v.placa AS "placa",
            v.modelo AS "modelo",
            v.tipo AS "tipo",
            SUM(vi.km_percorrida) AS "totalKm"
        FROM viagens vi
        JOIN veiculos v ON vi.veiculo_id = v.id
        WHERE vi.data_chegada IS NOT NULL
        GROUP BY v.id, v.placa, v.modelo, v.tipo
        ORDER BY SUM(vi.km_percorrida) DESC, v.id ASC
        LIMIT 1
        """, nativeQuery = true)
    Optional<RankingUtilizacaoProjection> findRankingUtilizacao();

    /**
     * Métrica 5: Projeção Financeira de manutenção para o mês atual.
     * Soma os custos estimados no intervalo indexável [início do mês, início do próximo mês),
     * sem aplicar função sobre a coluna data_inicio para preservar o uso de índice.
     */
    @Query(value = """
        SELECT COALESCE(SUM(custo_estimado), 0)
        FROM manutencoes
        WHERE data_inicio >= date_trunc('month', CURRENT_DATE)::date
          AND data_inicio < (date_trunc('month', CURRENT_DATE) + INTERVAL '1 month')::date
        """, nativeQuery = true)
    BigDecimal findProjecaoFinanceiraMesAtual();
}
