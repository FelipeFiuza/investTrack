-- Posição diária de um investimento para um usuário, entre duas datas.
-- MySQL 5.7 não tem table-valued functions: stored PROCEDURE com result set.
--
-- Uso:
--   CALL get_posicao_diaria(1, 423, '2024-07-25', '2024-08-05');
--
-- Colunas:
--   day, current_qty, current_average_cost,
--   bought_qty, bought_price, bought_taxes,
--   sold_qty, sold_price, sold_taxes,
--   valor_fechamento, gross_total_amount, net_total_amount,
--   gross_variation, net_variation
--
-- Regras:
--   - Uma linha por dia (dias de ApuracaoIndice e/ou com transação no intervalo).
--   - Transações anteriores a p_data_inicio atualizam qty, preço médio e taxas
--     acumuladas, mas não geram linha.
--   - valor_fechamento vem de apuracao_indice (fallback: apuracao_indice_fixa).
--   - Compra: atualiza current_qty e current_average_cost.
--       1ª compra / qty zero: average = custo / qty
--       seguintes: (avg * qty_anterior + custo) / nova_qty
--       custo = valor_total (senão valor_unitario * quantidade)
--   - Venda: atualiza só current_qty (preço médio permanece).
--   - bought_* / sold_*: totais do dia (qty, preço médio ponderado).
--     taxes = soma acumulada de taxas_impostos de todas as transações
--     processadas até aquele dia (inclusive anteriores ao intervalo).
--   - tipo_transacao NULL ou diferente de 'sell' => compra.
--   - gross_total_amount = valor_fechamento * current_qty
--   - net_total_amount   = gross_total_amount - bought_taxes - sold_taxes
--   - gross_variation    = valor_fechamento / current_average_cost
--   - net_variation      = net_total_amount / (current_average_cost * current_qty)

DROP PROCEDURE IF EXISTS get_posicao_diaria;

DELIMITER $$

CREATE PROCEDURE get_posicao_diaria(
    IN p_id_usuario BIGINT,
    IN p_cod_investimento BIGINT,
    IN p_data_inicio DATE,
    IN p_data_fim DATE
)
BEGIN
    DECLARE v_cod_indice BIGINT DEFAULT NULL;

    DECLARE v_qty DECIMAL(19, 6) DEFAULT 0;
    DECLARE v_avg DECIMAL(19, 6) DEFAULT 0;
    DECLARE v_cost_basis DECIMAL(19, 6) DEFAULT 0;
    DECLARE v_bought_taxes DECIMAL(19, 6) DEFAULT 0;
    DECLARE v_sold_taxes DECIMAL(19, 6) DEFAULT 0;
    DECLARE v_sold_applied DECIMAL(19, 6);

    DECLARE v_dia_id INT DEFAULT 1;
    DECLARE v_dia_max INT DEFAULT 0;
    DECLARE v_dia DATE;
    DECLARE v_fechamento DECIMAL(19, 6);

    DECLARE v_tx_id INT DEFAULT 1;
    DECLARE v_tx_max INT DEFAULT 0;
    DECLARE v_tx_tipo VARCHAR(4);
    DECLARE v_tx_qty DECIMAL(19, 6);
    DECLARE v_tx_valor_total DECIMAL(19, 6);
    DECLARE v_tx_valor_unitario DECIMAL(19, 6);
    DECLARE v_tx_taxas DECIMAL(19, 6);

    DECLARE v_custo DECIMAL(19, 6);
    DECLARE v_unit DECIMAL(19, 6);

    DECLARE v_day_bought_qty DECIMAL(19, 6);
    DECLARE v_day_bought_cost DECIMAL(19, 6);
    DECLARE v_day_sold_qty DECIMAL(19, 6);
    DECLARE v_day_sold_cost DECIMAL(19, 6);

    DECLARE v_bought_qty DECIMAL(19, 6);
    DECLARE v_bought_price DECIMAL(19, 6);
    DECLARE v_sold_qty DECIMAL(19, 6);
    DECLARE v_sold_price DECIMAL(19, 6);
    DECLARE v_gross DECIMAL(19, 6);
    DECLARE v_net DECIMAL(19, 6);
    DECLARE v_gross_var DECIMAL(19, 6);
    DECLARE v_net_var DECIMAL(19, 6);
    DECLARE v_had_tx TINYINT;

    SET v_cod_indice = (
        SELECT ti.cod_indice
          FROM tipo_investimento ti
         WHERE ti.cod_investimento = p_cod_investimento
         LIMIT 1
    );

    DROP TEMPORARY TABLE IF EXISTS tmp_posicao_diaria;
    CREATE TEMPORARY TABLE tmp_posicao_diaria (
        dia DATE NOT NULL,
        current_qty DECIMAL(19, 6) NOT NULL,
        current_average_cost DECIMAL(19, 6) NOT NULL,
        bought_qty DECIMAL(19, 6) NULL,
        bought_price DECIMAL(19, 6) NULL,
        bought_taxes DECIMAL(19, 6) NOT NULL,
        sold_qty DECIMAL(19, 6) NULL,
        sold_price DECIMAL(19, 6) NULL,
        sold_taxes DECIMAL(19, 6) NOT NULL,
        valor_fechamento DECIMAL(19, 6) NULL,
        gross_total_amount DECIMAL(19, 6) NULL,
        net_total_amount DECIMAL(19, 6) NULL,
        gross_variation DECIMAL(19, 6) NULL,
        net_variation DECIMAL(19, 6) NULL
    );

    DROP TEMPORARY TABLE IF EXISTS tmp_posicao_dias;
    CREATE TEMPORARY TABLE tmp_posicao_dias (
        id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        dia DATE NOT NULL,
        valor_fechamento DECIMAL(19, 6) NULL
    );

    DROP TEMPORARY TABLE IF EXISTS tmp_posicao_txs;
    CREATE TEMPORARY TABLE tmp_posicao_txs (
        id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        dia DATE NOT NULL,
        tipo VARCHAR(4) NOT NULL,
        quantidade DECIMAL(19, 6) NOT NULL,
        valor_total DECIMAL(19, 6) NULL,
        valor_unitario DECIMAL(19, 6) NULL,
        taxas DECIMAL(19, 6) NOT NULL
    );

    INSERT INTO tmp_posicao_txs (dia, tipo, quantidade, valor_total, valor_unitario, taxas)
    SELECT DATE(t.data_transacao),
           CASE
               WHEN LOWER(IFNULL(t.tipo_transacao, 'buy')) = 'sell' THEN 'sell'
               ELSE 'buy'
           END,
           IFNULL(t.quantidade, 0),
           t.valor_total,
           t.valor_unitario,
           IFNULL(t.taxas_impostos, 0)
      FROM transacao t
     WHERE t.id_usuario = p_id_usuario
       AND t.cod_investimento = p_cod_investimento
       AND DATE(t.data_transacao) <= p_data_fim
     ORDER BY t.data_transacao, t.id_transacao;

    INSERT INTO tmp_posicao_dias (dia, valor_fechamento)
    SELECT d.dia, COALESCE(a.valor_fechamento, f.valor_fechamento)
    FROM (
        SELECT DISTINCT DATE(ai.data_apuracao) AS dia
          FROM apuracao_indice ai
         WHERE v_cod_indice IS NOT NULL
           AND ai.cod_indice = v_cod_indice
           AND DATE(ai.data_apuracao) BETWEEN p_data_inicio AND p_data_fim
        UNION
        SELECT DISTINCT DATE(af.data_apuracao) AS dia
          FROM apuracao_indice_fixa af
         WHERE v_cod_indice IS NOT NULL
           AND af.cod_indice = v_cod_indice
           AND DATE(af.data_apuracao) BETWEEN p_data_inicio AND p_data_fim
        UNION
        SELECT DISTINCT DATE(t.data_transacao) AS dia
          FROM transacao t
         WHERE t.id_usuario = p_id_usuario
           AND t.cod_investimento = p_cod_investimento
           AND DATE(t.data_transacao) BETWEEN p_data_inicio AND p_data_fim
    ) d
    LEFT JOIN (
        SELECT DATE(ai.data_apuracao) AS dia, MAX(ai.valor_fechamento) AS valor_fechamento
          FROM apuracao_indice ai
         WHERE v_cod_indice IS NOT NULL
           AND ai.cod_indice = v_cod_indice
           AND DATE(ai.data_apuracao) BETWEEN p_data_inicio AND p_data_fim
         GROUP BY DATE(ai.data_apuracao)
    ) a ON a.dia = d.dia
    LEFT JOIN (
        SELECT DATE(af.data_apuracao) AS dia, MAX(af.valor_fechamento) AS valor_fechamento
          FROM apuracao_indice_fixa af
         WHERE v_cod_indice IS NOT NULL
           AND af.cod_indice = v_cod_indice
           AND DATE(af.data_apuracao) BETWEEN p_data_inicio AND p_data_fim
         GROUP BY DATE(af.data_apuracao)
    ) f ON f.dia = d.dia
    ORDER BY d.dia;

    -- Posição de abertura: aplica transações anteriores ao intervalo.
    SELECT IFNULL(MIN(id), 0), IFNULL(MAX(id), 0)
      INTO v_tx_id, v_tx_max
      FROM tmp_posicao_txs
     WHERE dia < p_data_inicio;

    IF v_tx_id > 0 THEN
        WHILE v_tx_id <= v_tx_max DO
            SELECT tipo, quantidade, valor_total, valor_unitario, taxas
              INTO v_tx_tipo, v_tx_qty, v_tx_valor_total, v_tx_valor_unitario, v_tx_taxas
              FROM tmp_posicao_txs
             WHERE id = v_tx_id;

            SET v_unit = COALESCE(
                v_tx_valor_unitario,
                CASE
                    WHEN v_tx_qty IS NOT NULL AND v_tx_qty <> 0 AND v_tx_valor_total IS NOT NULL
                        THEN v_tx_valor_total / v_tx_qty
                    ELSE v_tx_valor_total
                END
            );
            SET v_custo = COALESCE(
                v_tx_valor_total,
                CASE
                    WHEN v_tx_valor_unitario IS NOT NULL THEN v_tx_valor_unitario * IFNULL(v_tx_qty, 0)
                    ELSE IFNULL(v_unit, 0) * IFNULL(v_tx_qty, 0)
                END
            );

            IF v_tx_tipo = 'sell' THEN
                SET v_sold_applied = LEAST(IFNULL(v_tx_qty, 0), v_qty);
                IF v_qty > 0 THEN
                    SET v_cost_basis = v_cost_basis * (v_qty - v_sold_applied) / v_qty;
                END IF;
                SET v_qty = v_qty - v_sold_applied;
                SET v_sold_taxes = v_sold_taxes + IFNULL(v_tx_taxas, 0);
                IF v_qty = 0 THEN
                    SET v_cost_basis = 0;
                    SET v_avg = 0;
                ELSE
                    SET v_avg = v_cost_basis / v_qty;
                END IF;
            ELSE
                SET v_cost_basis = v_cost_basis + IFNULL(v_custo, 0);
                SET v_qty = v_qty + IFNULL(v_tx_qty, 0);
                SET v_avg = CASE WHEN v_qty = 0 THEN 0 ELSE v_cost_basis / v_qty END;
                SET v_bought_taxes = v_bought_taxes + IFNULL(v_tx_taxas, 0);
            END IF;

            SET v_tx_id = v_tx_id + 1;
        END WHILE;
    END IF;

    SELECT IFNULL(MAX(id), 0) INTO v_dia_max FROM tmp_posicao_dias;
    SET v_dia_id = 1;

    WHILE v_dia_id <= v_dia_max DO
        SELECT dia, valor_fechamento
          INTO v_dia, v_fechamento
          FROM tmp_posicao_dias
         WHERE id = v_dia_id;

        SET v_day_bought_qty = 0;
        SET v_day_bought_cost = 0;
        SET v_day_sold_qty = 0;
        SET v_day_sold_cost = 0;
        SET v_had_tx = 0;

        SELECT IFNULL(MIN(id), 0), IFNULL(MAX(id), 0)
          INTO v_tx_id, v_tx_max
          FROM tmp_posicao_txs
         WHERE dia = v_dia;

        IF v_tx_id > 0 THEN
            SET v_had_tx = 1;
            WHILE v_tx_id <= v_tx_max DO
                SELECT tipo, quantidade, valor_total, valor_unitario, taxas
                  INTO v_tx_tipo, v_tx_qty, v_tx_valor_total, v_tx_valor_unitario, v_tx_taxas
                  FROM tmp_posicao_txs
                 WHERE id = v_tx_id;

                SET v_unit = COALESCE(
                    v_tx_valor_unitario,
                    CASE
                        WHEN v_tx_qty IS NOT NULL AND v_tx_qty <> 0 AND v_tx_valor_total IS NOT NULL
                            THEN v_tx_valor_total / v_tx_qty
                        ELSE v_tx_valor_total
                    END
                );
                SET v_custo = COALESCE(
                    v_tx_valor_total,
                    CASE
                        WHEN v_tx_valor_unitario IS NOT NULL THEN v_tx_valor_unitario * IFNULL(v_tx_qty, 0)
                        ELSE IFNULL(v_unit, 0) * IFNULL(v_tx_qty, 0)
                    END
                );

                IF v_tx_tipo = 'sell' THEN
                    SET v_sold_applied = LEAST(IFNULL(v_tx_qty, 0), v_qty);
                    IF v_qty > 0 THEN
                        SET v_cost_basis = v_cost_basis * (v_qty - v_sold_applied) / v_qty;
                    END IF;
                    SET v_qty = v_qty - v_sold_applied;
                    SET v_sold_taxes = v_sold_taxes + IFNULL(v_tx_taxas, 0);
                    SET v_day_sold_qty = v_day_sold_qty + IFNULL(v_tx_qty, 0);
                    SET v_day_sold_cost = v_day_sold_cost + IFNULL(v_custo, 0);
                    IF v_qty = 0 THEN
                        SET v_cost_basis = 0;
                        SET v_avg = 0;
                    ELSE
                        SET v_avg = v_cost_basis / v_qty;
                    END IF;
                ELSE
                    SET v_cost_basis = v_cost_basis + IFNULL(v_custo, 0);
                    SET v_qty = v_qty + IFNULL(v_tx_qty, 0);
                    SET v_avg = CASE WHEN v_qty = 0 THEN 0 ELSE v_cost_basis / v_qty END;
                    SET v_bought_taxes = v_bought_taxes + IFNULL(v_tx_taxas, 0);
                    SET v_day_bought_qty = v_day_bought_qty + IFNULL(v_tx_qty, 0);
                    SET v_day_bought_cost = v_day_bought_cost + IFNULL(v_custo, 0);
                END IF;

                SET v_tx_id = v_tx_id + 1;
            END WHILE;
        END IF;

        IF v_had_tx = 1 OR v_qty > 0 THEN
            SET v_bought_qty = CASE WHEN v_day_bought_qty = 0 THEN NULL ELSE v_day_bought_qty END;
            SET v_bought_price = CASE
                WHEN v_day_bought_qty = 0 THEN NULL
                ELSE v_day_bought_cost / v_day_bought_qty
            END;
            SET v_sold_qty = CASE WHEN v_day_sold_qty = 0 THEN NULL ELSE v_day_sold_qty END;
            SET v_sold_price = CASE
                WHEN v_day_sold_qty = 0 THEN NULL
                ELSE v_day_sold_cost / v_day_sold_qty
            END;

            SET v_gross = v_fechamento * v_qty;
            SET v_net = v_gross - v_bought_taxes - v_sold_taxes;
            SET v_gross_var = CASE
                WHEN v_avg IS NULL OR v_avg = 0 OR v_fechamento IS NULL THEN NULL
                ELSE v_fechamento / v_avg
            END;
            SET v_net_var = CASE
                WHEN v_cost_basis IS NULL OR v_cost_basis = 0 OR v_net IS NULL THEN NULL
                ELSE v_net / v_cost_basis
            END;

            INSERT INTO tmp_posicao_diaria (
                dia, current_qty, current_average_cost,
                bought_qty, bought_price, bought_taxes,
                sold_qty, sold_price, sold_taxes,
                valor_fechamento, gross_total_amount, net_total_amount,
                gross_variation, net_variation
            ) VALUES (
                v_dia, v_qty, v_avg,
                v_bought_qty, v_bought_price, v_bought_taxes,
                v_sold_qty, v_sold_price, v_sold_taxes,
                v_fechamento, v_gross, v_net,
                v_gross_var, v_net_var
            );
        END IF;

        SET v_dia_id = v_dia_id + 1;
    END WHILE;

    SELECT
        dia AS `day`,
        current_qty,
        current_average_cost,
        bought_qty,
        bought_price,
        bought_taxes,
        sold_qty,
        sold_price,
        sold_taxes,
        valor_fechamento,
        gross_total_amount,
        net_total_amount,
        gross_variation,
        net_variation
      FROM tmp_posicao_diaria
     ORDER BY dia;
END$$

DELIMITER ;
