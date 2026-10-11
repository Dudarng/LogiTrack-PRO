-- ============================================================================
-- V2__ajustes.sql: Ajustes de integridade, índices, tabela de segurança e dados dinâmicos
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. Restrições CHECK na tabela manutencoes
-- Justificativa:
-- chk_manutencoes_status: Garante a integridade de domínio dos status aceitos pelo
--   sistema ('PENDENTE', 'EM_REALIZACAO', 'CONCLUIDA'), alinhando com a regra de
--   negócio e o enum Java correspondente, impedindo estados inválidos.
-- chk_manutencoes_datas: Assegura coerência cronológica: data_finalizacao deve ser
--   igual ou posterior a data_inicio. Permite expressamente valores nulos para
--   ordens em aberto ou ainda em andamento.
-- chk_manutencoes_custo: Impede inserção de valores de custo financeiro negativos,
--   garantindo consistência contábil nas estimativas de serviços.
-- ----------------------------------------------------------------------------
ALTER TABLE manutencoes
    ADD CONSTRAINT chk_manutencoes_status
    CHECK (status IN ('PENDENTE', 'EM_REALIZACAO', 'CONCLUIDA'));

ALTER TABLE manutencoes
    ADD CONSTRAINT chk_manutencoes_datas
    CHECK (data_finalizacao IS NULL OR data_finalizacao >= data_inicio);

ALTER TABLE manutencoes
    ADD CONSTRAINT chk_manutencoes_custo
    CHECK (custo_estimado >= 0);

-- ----------------------------------------------------------------------------
-- 2. Restrições CHECK na tabela viagens
-- Justificativa:
-- chk_viagens_datas: Garante a coerência cronológica das viagens realizadas,
--   impedindo que a data de chegada seja anterior à data de saída. Permite que
--   data_chegada seja nula para viagens que ainda estejam em trânsito.
-- chk_viagens_km: Garante que a quilometragem percorrida não seja negativa,
--   preservando a integridade das métricas de distância e consumo da frota.
-- ----------------------------------------------------------------------------
ALTER TABLE viagens
    ADD CONSTRAINT chk_viagens_datas
    CHECK (data_chegada IS NULL OR data_chegada >= data_saida);

ALTER TABLE viagens
    ADD CONSTRAINT chk_viagens_km
    CHECK (km_percorrida >= 0);

-- ----------------------------------------------------------------------------
-- 3. Índices de Otimização de Performance
-- Justificativa:
-- - idx_viagens_veiculo_id: Otimiza as operações de junção (JOIN) entre veiculos
--   e viagens, além de acelerar os cálculos agregados do dashboard (total de KM por
--   veículo e ranking de utilização da frota).
-- - idx_manutencoes_veiculo_id: Acelera consultas de histórico de manutenções por
--   veículo e suporta a integridade referencial com remoção em cascata.
-- - idx_manutencoes_data_inicio: Otimiza filtros e ordenações por período em
--   manutenções, em especial a métrica de projeção financeira do mês atual.
-- - idx_manutencoes_agenda: Índice parcial (WHERE status <> 'CONCLUIDA') focado
--   exclusivamente nas manutenções agendadas e ativas. Otimiza a consulta das
--   "próximas 5 manutenções agendadas" reduzindo substancialmente o tamanho do índice
--   e o custo de I/O em comparação a um índice completo sobre toda a tabela.
-- ----------------------------------------------------------------------------
CREATE INDEX idx_viagens_veiculo_id ON viagens (veiculo_id);
CREATE INDEX idx_manutencoes_veiculo_id ON manutencoes (veiculo_id);
CREATE INDEX idx_manutencoes_data_inicio ON manutencoes (data_inicio);
CREATE INDEX idx_manutencoes_agenda ON manutencoes (data_inicio) WHERE status <> 'CONCLUIDA';

-- ----------------------------------------------------------------------------
-- 4. Criação da Tabela usuarios
-- Justificativa:
-- Cria a estrutura necessária para persistência de credenciais e controle de acesso
-- (diferencial do projeto com Spring Security). O campo username possui restrição
-- UNIQUE e NOT NULL para garantir unicidade de login; password_hash comporta
-- com folga hashes criptográficos modernos (ex: BCrypt, Argon2); e role define
-- o perfil de autorização do usuário na aplicação.
-- Observação: Conforme planejado, nenhum usuário seed é inserido nesta migration;
-- a carga de usuários administrativos será realizada em migration futura (V3).
-- ----------------------------------------------------------------------------
CREATE TABLE usuarios (
    id SERIAL PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL
);

-- ----------------------------------------------------------------------------
-- 5. Carga de Dados em manutencoes com Datas Relativas
-- Justificativa:
-- A carga inicial (V1) continha apenas datas estáticas do ano de 2024. Para que o
-- Dashboard analítico exiba métricas dinâmicas e realistas (especialmente
-- 'Projeção Financeira do Mês Atual' e 'Cronograma das Próximas 5 Manutenções Agendadas'),
-- são inseridos registros calculados com CURRENT_DATE.
--
-- NOTA IMPORTANTE:
-- As expressões com CURRENT_DATE e INTERVAL são avaliadas uma única vez, no momento
-- exato da aplicação da migration pelo Flyway no banco de dados, sendo gravadas como
-- valores de data estáticos a partir de então.
-- ----------------------------------------------------------------------------
INSERT INTO manutencoes (veiculo_id, data_inicio, data_finalizacao, tipo_servico, custo_estimado, status) VALUES 
-- Manutenções situadas no mês atual (alimenta Projeção Financeira do mês atual)
(1, DATE_TRUNC('month', CURRENT_DATE)::DATE + INTERVAL '2 days', DATE_TRUNC('month', CURRENT_DATE)::DATE + INTERVAL '3 days', 'Alinhamento e Balanceamento', 450.00, 'CONCLUIDA'),
(2, DATE_TRUNC('month', CURRENT_DATE)::DATE + INTERVAL '8 days', NULL, 'Substituição de Pastilhas de Freio', 980.00, 'EM_REALIZACAO'),
(3, DATE_TRUNC('month', CURRENT_DATE)::DATE + INTERVAL '14 days', NULL, 'Revisão do Sistema Elétrico', 1150.00, 'PENDENTE'),

-- Próximas manutenções agendadas (alimenta Cronograma das Próximas 5 Manutenções)
(4, CURRENT_DATE + INTERVAL '1 day', CURRENT_DATE + INTERVAL '2 days', 'Troca de Filtros e Óleo', 620.00, 'EM_REALIZACAO'),
(1, CURRENT_DATE + INTERVAL '4 days', CURRENT_DATE + INTERVAL '5 days', 'Regulagem Eletrônica dos Bicos Injetores', 850.00, 'PENDENTE'),
(2, CURRENT_DATE + INTERVAL '7 days', CURRENT_DATE + INTERVAL '9 days', 'Inspeção e Troca da Suspensão Pneumática', 2400.00, 'PENDENTE'),
(3, CURRENT_DATE + INTERVAL '12 days', CURRENT_DATE + INTERVAL '13 days', 'Troca da Correia Dentada e Tensores', 750.00, 'PENDENTE'),
(4, CURRENT_DATE + INTERVAL '18 days', CURRENT_DATE + INTERVAL '20 days', 'Revisão Geral e Diagnóstico de Turbina', 4200.00, 'PENDENTE'),
(2, CURRENT_DATE + INTERVAL '28 days', NULL, 'Manutenção do Sistema de Arla 32', 1300.00, 'PENDENTE');

-- ----------------------------------------------------------------------------
-- 6. Carga de Dados em viagens com Datas Relativas e Veículos Variados
-- Justificativa:
-- Insere histórico recente de viagens distribuído entre veículos leves (Fiorino,
-- Sprinter) e pesados (Volvo FH, Scania R500). Isso alimenta e valida as métricas:
-- - 'Total de KM percorrido' (frota e veículo específico);
-- - 'Volume por Categoria' (Leve vs Pesado);
-- - 'Ranking de Utilização' (veículo com maior quilometragem acumulada).
--
-- NOTA IMPORTANTE:
-- As expressões com CURRENT_TIMESTAMP e INTERVAL são avaliadas uma única vez, no
-- momento da execução da migration pelo Flyway, persistindo timestamps definitivos.
-- ----------------------------------------------------------------------------
INSERT INTO viagens (veiculo_id, data_saida, data_chegada, origem, destino, km_percorrida) VALUES 
-- Veículo 1 (LEVE - Fiorino): rotas curtas e médias
(1, CURRENT_TIMESTAMP - INTERVAL '6 days 4 hours', CURRENT_TIMESTAMP - INTERVAL '6 days 1 hour', 'São Paulo', 'Campinas', 98.50),
(1, CURRENT_TIMESTAMP - INTERVAL '3 days 6 hours', CURRENT_TIMESTAMP - INTERVAL '3 days 2 hours', 'Campinas', 'Santos', 165.00),

-- Veículo 2 (PESADO - Volvo FH): transporte interestadual de carga pesada
(2, CURRENT_TIMESTAMP - INTERVAL '7 days 10 hours', CURRENT_TIMESTAMP - INTERVAL '6 days 18 hours', 'Santos', 'Belo Horizonte', 620.00),
(2, CURRENT_TIMESTAMP - INTERVAL '4 days 12 hours', CURRENT_TIMESTAMP - INTERVAL '3 days 22 hours', 'Belo Horizonte', 'Brasília', 740.00),
(2, CURRENT_TIMESTAMP - INTERVAL '10 hours', NULL, 'Brasília', 'Goiânia', 209.00),

-- Veículo 3 (LEVE - Mercedes Sprinter): logística e distribuição regional
(3, CURRENT_TIMESTAMP - INTERVAL '5 days 8 hours', CURRENT_TIMESTAMP - INTERVAL '5 days 5 hours', 'Curitiba', 'Joinville', 130.00),
(3, CURRENT_TIMESTAMP - INTERVAL '2 days 7 hours', CURRENT_TIMESTAMP - INTERVAL '2 days 3 hours', 'Joinville', 'Florianópolis', 180.00),
(3, CURRENT_TIMESTAMP - INTERVAL '1 day 4 hours', CURRENT_TIMESTAMP - INTERVAL '1 day 1 hour', 'Florianópolis', 'Blumenau', 150.00),

-- Veículo 4 (PESADO - Scania R500): transporte rodoviário pesado de longo curso
(4, CURRENT_TIMESTAMP - INTERVAL '5 days 14 hours', CURRENT_TIMESTAMP - INTERVAL '4 days 8 hours', 'Porto Alegre', 'Curitiba', 710.00),
(4, CURRENT_TIMESTAMP - INTERVAL '2 days 16 hours', CURRENT_TIMESTAMP - INTERVAL '1 day 14 hours', 'Curitiba', 'São Paulo', 410.00);
