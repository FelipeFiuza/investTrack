package com.bezkoder.spring.datajpa.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bezkoder.spring.datajpa.model.Transacao;

public interface TransacaoRepository extends JpaRepository<Transacao, Long> {
    List<Transacao> findByIdUsuario(Long idUsuario);
    List<Transacao> findByCodInvestimento(Long codInvestimento);
    List<Transacao> findByDataTransacaoBetween(LocalDateTime inicio, LocalDateTime fim);
    List<Transacao> findByDataVencimentoBetween(LocalDateTime inicio, LocalDateTime fim);
}