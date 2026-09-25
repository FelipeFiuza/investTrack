package com.bezkoder.spring.datajpa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.datajpa.model.Panel;

@Repository
public interface PanelRepository extends JpaRepository<Panel, Long> {
    List<Panel> findByUsuario_IdUsuarioOrderByIdPanelAsc(Long idUsuario);

    @Modifying
    @Query(value = "DELETE FROM panel_transacao WHERE id_transacao = :idTransacao", nativeQuery = true)
    void deleteLinksByTransacao(@Param("idTransacao") Long idTransacao);
}
