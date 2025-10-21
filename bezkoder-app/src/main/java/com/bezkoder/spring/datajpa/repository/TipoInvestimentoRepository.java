package com.bezkoder.spring.datajpa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.datajpa.model.Indice;
import com.bezkoder.spring.datajpa.model.TipoInvestimento;

@Repository
public interface TipoInvestimentoRepository extends JpaRepository<TipoInvestimento, Long> {
    List<TipoInvestimento> findByDescricaoContaining(String descricao);
    TipoInvestimento findByIndice(Indice indice);
    boolean existsByIndice(Indice indice);
}