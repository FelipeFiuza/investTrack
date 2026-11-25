package com.bezkoder.spring.datajpa.service;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class StoredProcedureInitializer {

    private static final Logger log = LoggerFactory.getLogger(StoredProcedureInitializer.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void dropPosicaoProcedure() {
        try {
            jdbcTemplate.execute("DROP PROCEDURE IF EXISTS get_posicao_diaria");
            log.info("Stored procedure get_posicao_diaria removida. O grafico e calculado no frontend.");
        } catch (Exception e) {
            log.warn("Nao foi possivel remover a procedure get_posicao_diaria: {}", e.getMessage());
        }
    }
}
