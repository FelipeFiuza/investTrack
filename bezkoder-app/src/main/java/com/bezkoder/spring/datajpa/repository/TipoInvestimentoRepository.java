package com.bezkoder.spring.datajpa.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.datajpa.model.Indice;
import com.bezkoder.spring.datajpa.model.TipoInvestimento;

@Repository
public interface TipoInvestimentoRepository extends JpaRepository<TipoInvestimento, Long> {
    List<TipoInvestimento> findByDescricaoContaining(String descricao);
    List<TipoInvestimento> findByDescricao(String descricao);
    List<TipoInvestimento> findByIndice(Indice indice);
    boolean existsByIndice(Indice indice);
    List<TipoInvestimento> findByIndice_DescricaoIgnoreCase(String descricao);

    @Query("SELECT t FROM TipoInvestimento t JOIN t.indice i " +
        "WHERE i.tipoIndice = 'F' AND (" +
        "UPPER(i.descricao) LIKE UPPER(CONCAT('%', :q, '%')) OR " +
        "UPPER(t.descricao) LIKE UPPER(CONCAT('%', :q, '%'))) " +
        "ORDER BY CASE WHEN UPPER(i.descricao) LIKE UPPER(CONCAT(:q, '%')) THEN 0 ELSE 1 END, i.descricao")
    List<TipoInvestimento> searchAcoes(@Param("q") String q, Pageable pageable);
}