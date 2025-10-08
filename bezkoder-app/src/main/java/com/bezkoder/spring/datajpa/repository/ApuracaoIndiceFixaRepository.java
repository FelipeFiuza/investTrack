package com.bezkoder.spring.datajpa.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bezkoder.spring.datajpa.model.ApuracaoIndiceFixa;
import com.bezkoder.spring.datajpa.model.ApuracaoIndiceId;
import com.bezkoder.spring.datajpa.model.Indice;

public interface ApuracaoIndiceFixaRepository extends JpaRepository<ApuracaoIndiceFixa, ApuracaoIndiceId> {
    List<ApuracaoIndiceFixa> findByIndice(Indice indice);
    List<ApuracaoIndiceFixa> findByDataApuracaoBetween(LocalDateTime inicio, LocalDateTime fim);
    List<ApuracaoIndiceFixa> findByIndiceAndDataApuracaoBetween(Indice indice, LocalDateTime inicio, LocalDateTime fim);
}