package com.bezkoder.spring.datajpa.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bezkoder.spring.datajpa.model.ApuracaoIndiceFixa;
import com.bezkoder.spring.datajpa.model.ApuracaoIndiceFixaId;

public interface ApuracaoIndiceFixaRepository extends JpaRepository<ApuracaoIndiceFixa, ApuracaoIndiceFixaId> {
    List<ApuracaoIndiceFixa> findByCodIndice(String codIndice);
    List<ApuracaoIndiceFixa> findByDataApuracaoAfter(LocalDateTime data);
}