package com.bezkoder.spring.datajpa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bezkoder.spring.datajpa.model.Indice;

public interface IndiceRepository extends JpaRepository<Indice, Long> {
    List<Indice> findByDescricaoContaining(String descricao);
    List<Indice> findByTipoIndice(String tipoIndice);
    List<Indice> findByCodIndice(Long codIndice);
    boolean existsByDescricao(String descricao);
}