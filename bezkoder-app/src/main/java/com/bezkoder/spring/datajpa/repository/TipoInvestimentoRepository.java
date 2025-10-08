package com.bezkoder.spring.datajpa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bezkoder.spring.datajpa.model.Indice;
import com.bezkoder.spring.datajpa.model.TipoInvestimento;

public interface TipoInvestimentoRepository extends JpaRepository<TipoInvestimento, Long> {
    List<TipoInvestimento> findByDescricaoContaining(String descricao);
    List<TipoInvestimento> findByIndice(Indice indice);
}