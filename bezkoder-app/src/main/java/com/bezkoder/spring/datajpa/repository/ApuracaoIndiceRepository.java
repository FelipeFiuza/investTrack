package com.bezkoder.spring.datajpa.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bezkoder.spring.datajpa.model.ApuracaoIndice;
import com.bezkoder.spring.datajpa.model.ApuracaoIndiceId;

public interface ApuracaoIndiceRepository extends JpaRepository<ApuracaoIndice, ApuracaoIndiceId> {
    List<ApuracaoIndice> findByCodIndice(String codIndice);
    List<ApuracaoIndice> findByDataApuracaoBetween(LocalDateTime inicio, LocalDateTime fim);
}