export type B3TipoTransacao = 'buy' | 'sell';

export interface B3ImportPreviewRow {
  rowNumber: number;
  selected: boolean;
  tipoTransacao: B3TipoTransacao;
  dataTransacao: string;
  produto: string;
  ticker: string;
  instituicao: string;
  quantidade: number;
  valorUnitario: number;
  valorTotal: number;
}

export interface B3ImportParseResult {
  transacoes: B3ImportPreviewRow[];
  ignoradas: number;
}

const MOVIMENTACAO_LIQUIDACAO = 'transferencia - liquidacao';

const normalizeHeader = (value: unknown): string => {
  return String(value ?? '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[\u2012\u2013\u2014\u2212]/g, '-')
    .toLowerCase()
    .replace(/\s+/g, ' ')
    .trim();
};

const cellText = (value: unknown): string => {
  if (value === null || value === undefined) return '';
  if (value instanceof Date && !Number.isNaN(value.getTime())) {
    const d = String(value.getDate()).padStart(2, '0');
    const m = String(value.getMonth() + 1).padStart(2, '0');
    const y = value.getFullYear();
    return `${d}/${m}/${y}`;
  }
  return String(value).replace(/\u00a0/g, ' ').trim();
};

export const extractTicker = (produto: string): string => {
  const trimmed = produto.trim();
  if (!trimmed) return '';
  const dash = trimmed.indexOf(' - ');
  if (dash > 0) {
    return trimmed.slice(0, dash).trim().toUpperCase();
  }
  return trimmed.split(/\s+/)[0].toUpperCase();
};

export const parseBRL = (value: unknown): number | null => {
  if (value === null || value === undefined || value === '') return null;
  if (typeof value === 'number' && Number.isFinite(value)) return value;

  const raw = cellText(value);
  if (!raw || raw === '-' || raw === 'R$' || raw === '$') return null;

  const cleaned = raw.replace(/R\$/gi, '').trim();
  if (!cleaned) return null;

  const brFormat = /^-?\d{1,3}(\.\d{3})*,\d+$/.test(cleaned) || /^-?\d+,\d+$/.test(cleaned);
  if (brFormat) {
    const n = parseFloat(cleaned.replace(/\./g, '').replace(',', '.'));
    return Number.isFinite(n) ? n : null;
  }

  const n = parseFloat(cleaned.replace(/,/g, ''));
  return Number.isFinite(n) ? n : null;
};

const toISODateTime = (year: number, month: number, day: number): string => {
  return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}T00:00:00`;
};

export const parseB3Date = (value: unknown): string | null => {
  if (value instanceof Date && !Number.isNaN(value.getTime())) {
    return toISODateTime(value.getFullYear(), value.getMonth() + 1, value.getDate());
  }

  if (typeof value === 'number' && Number.isFinite(value)) {
    // Excel serial date (days since 1899-12-30)
    const utc = Date.UTC(1899, 11, 30) + Math.round(value) * 86400000;
    const date = new Date(utc);
    return toISODateTime(date.getUTCFullYear(), date.getUTCMonth() + 1, date.getUTCDate());
  }

  const text = cellText(value);
  const br = text.match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})/);
  if (br) {
    return toISODateTime(Number(br[3]), Number(br[2]), Number(br[1]));
  }
  const iso = text.match(/^(\d{4})-(\d{2})-(\d{2})/);
  if (iso) {
    return toISODateTime(Number(iso[1]), Number(iso[2]), Number(iso[3]));
  }
  return null;
};

const findHeaderRow = (rows: unknown[][]): { index: number; map: Record<string, number> } | null => {
  for (let i = 0; i < Math.min(rows.length, 40); i += 1) {
    const row = rows[i] || [];
    const map: Record<string, number> = {};
    row.forEach((cell, col) => {
      const header = normalizeHeader(cell);
      if (header) map[header] = col;
    });
    if (
      map['entrada/saida'] !== undefined &&
      map['movimentacao'] !== undefined &&
      map['produto'] !== undefined
    ) {
      return { index: i, map };
    }
  }
  return null;
};

const colIndex = (map: Record<string, number>, ...names: string[]): number | undefined => {
  for (const name of names) {
    if (map[name] !== undefined) return map[name];
  }
  return undefined;
};

const moneyValuesAfter = (row: unknown[], startIndex: number): number[] => {
  const values: number[] = [];
  for (let i = startIndex; i < row.length; i += 1) {
    const parsed = parseBRL(row[i]);
    if (parsed !== null) values.push(parsed);
  }
  return values;
};

export const parseB3Rows = (rows: unknown[][]): B3ImportParseResult => {
  const header = findHeaderRow(rows);
  if (!header) {
    throw new Error('Could not find the B3 header row (Entrada/Saída, Movimentação, Produto).');
  }

  const entradaIdx = colIndex(header.map, 'entrada/saida');
  const dataIdx = colIndex(header.map, 'data');
  const movIdx = colIndex(header.map, 'movimentacao');
  const produtoIdx = colIndex(header.map, 'produto');
  const instIdx = colIndex(header.map, 'instituicao');
  const qtyIdx = colIndex(header.map, 'quantidade');

  if (
    entradaIdx === undefined ||
    dataIdx === undefined ||
    movIdx === undefined ||
    produtoIdx === undefined ||
    qtyIdx === undefined
  ) {
    throw new Error('B3 file is missing required columns.');
  }

  const transacoes: B3ImportPreviewRow[] = [];
  let ignoradas = 0;

  for (let i = header.index + 1; i < rows.length; i += 1) {
    const row = rows[i] || [];
    const movimentacao = normalizeHeader(row[movIdx]);
    const entradaSaida = normalizeHeader(row[entradaIdx]);

    if (!movimentacao && !entradaSaida && !cellText(row[produtoIdx])) {
      continue;
    }

    if (movimentacao !== MOVIMENTACAO_LIQUIDACAO) {
      ignoradas += 1;
      continue;
    }

    const tipoTransacao: B3TipoTransacao | null =
      entradaSaida === 'debito' ? 'sell' : entradaSaida === 'credito' ? 'buy' : null;

    if (!tipoTransacao) {
      ignoradas += 1;
      continue;
    }

    const produto = cellText(row[produtoIdx]);
    const ticker = extractTicker(produto);
    const dataTransacao = parseB3Date(row[dataIdx]);
    const quantidade = parseBRL(row[qtyIdx]);
    const money = moneyValuesAfter(row, qtyIdx + 1);
    const valorUnitario = money[0];
    const valorTotal = money[1] ?? (valorUnitario != null && quantidade != null ? valorUnitario * quantidade : null);

    if (!produto || !ticker || !dataTransacao || quantidade == null || valorUnitario == null || valorTotal == null) {
      ignoradas += 1;
      continue;
    }

    transacoes.push({
      rowNumber: i + 1,
      selected: true,
      tipoTransacao,
      dataTransacao,
      produto,
      ticker,
      instituicao: instIdx !== undefined ? cellText(row[instIdx]) : '',
      quantidade,
      valorUnitario,
      valorTotal,
    });
  }

  return { transacoes, ignoradas };
};
