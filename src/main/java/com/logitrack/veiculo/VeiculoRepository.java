package com.logitrack.veiculo;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VeiculoRepository extends JpaRepository<Veiculo, Integer> {

    List<Veiculo> findAllByOrderByPlacaAsc();
}
