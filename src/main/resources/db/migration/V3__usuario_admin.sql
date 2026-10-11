-- ============================================================================
-- V3__usuario_admin.sql: Inserção do usuário administrador inicial
-- ============================================================================
-- ATENÇÃO / AVISO DE SEGURANÇA:
-- A credencial inserida abaixo (admin / admin123) destina-se exclusivamente a
-- fins de demonstração, desenvolvimento e testes do sistema.
-- Em ambiente de produção, esta senha deve ser obrigatoriamente alterada
-- imediatamente após a implantação inicial.
--
-- O hash BCrypt foi gerado programaticamente via BCryptPasswordEncoder do Spring Security.
-- ============================================================================

INSERT INTO usuarios (username, password_hash, role) VALUES 
('admin', '$2a$10$92ruw.aMmtGE/y0Gh1Zwru5wQ7.kPbuQckx9EZzsp2qM/IpEBvPE.', 'ADMIN');
