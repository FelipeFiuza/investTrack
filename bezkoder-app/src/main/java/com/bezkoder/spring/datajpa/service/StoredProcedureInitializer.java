package com.bezkoder.spring.datajpa.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class StoredProcedureInitializer {

    private static final Logger log = LoggerFactory.getLogger(StoredProcedureInitializer.class);
    private static final String RESOURCE = "/db/get_posicao_diaria.sql";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        try {
            String sql = loadSql();
            String create = extractCreateProcedure(sql);
            jdbcTemplate.execute("DROP PROCEDURE IF EXISTS get_posicao_diaria");
            jdbcTemplate.execute(create);
            log.info("Stored procedure get_posicao_diaria criada/atualizada.");
        } catch (Exception e) {
            log.error("Nao foi possivel criar a stored procedure get_posicao_diaria", e);
        }
    }

    private String loadSql() throws IOException {
        InputStream in = getClass().getResourceAsStream(RESOURCE);
        if (in == null) {
            throw new IllegalStateException("Recurso nao encontrado no classpath: " + RESOURCE);
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } finally {
            reader.close();
        }
        return sb.toString();
    }

    private String extractCreateProcedure(String sql) {
        int createIdx = indexOfIgnoreCase(sql, "CREATE PROCEDURE");
        if (createIdx < 0) {
            throw new IllegalStateException("CREATE PROCEDURE nao encontrado em " + RESOURCE);
        }
        String create = sql.substring(createIdx);
        int endDollar = indexOfIgnoreCase(create, "END$$");
        if (endDollar >= 0) {
            return create.substring(0, endDollar + 3);
        }
        throw new IllegalStateException("END$$ da procedure nao encontrado em " + RESOURCE);
    }

    private int indexOfIgnoreCase(String value, String token) {
        return value.toUpperCase().indexOf(token.toUpperCase());
    }
}
