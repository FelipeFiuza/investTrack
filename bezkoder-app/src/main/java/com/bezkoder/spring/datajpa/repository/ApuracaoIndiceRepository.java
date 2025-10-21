package com.bezkoder.spring.datajpa.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.datajpa.model.ApuracaoIndice;
import com.bezkoder.spring.datajpa.model.ApuracaoIndiceId;
import com.bezkoder.spring.datajpa.model.Indice;

@Repository
public interface ApuracaoIndiceRepository extends JpaRepository<ApuracaoIndice, ApuracaoIndiceId> {
    List<ApuracaoIndice> findByIndice(Indice indice);
    List<ApuracaoIndice> findByDataApuracaoBetween(LocalDateTime inicio, LocalDateTime fim);
    List<ApuracaoIndice> findByIndiceAndDataApuracaoBetween(Indice indice, LocalDateTime inicio, LocalDateTime fim);
}