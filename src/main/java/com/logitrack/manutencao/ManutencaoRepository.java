package com.logitrack.manutencao;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ManutencaoRepository extends JpaRepository<Manutencao, Integer> {

    @EntityGraph(attributePaths = {"veiculo"})
    Page<Manutencao> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"veiculo"})
    Optional<Manutencao> findComVeiculoById(Integer id);
}
