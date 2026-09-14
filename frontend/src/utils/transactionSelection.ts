import { Transacao } from '../types';

export const isBuy = (tipo?: string): boolean => {
  return (tipo || 'buy').toLowerCase() !== 'sell';
};

export const sortTransacoes = (a: Transacao, b: Transacao): number => {
  const da = a.dataTransacao || '';
  const db = b.dataTransacao || '';
  if (da !== db) {
    return da < db ? -1 : 1;
  }
  return (a.idTransacao || 0) - (b.idTransacao || 0);
};

export const allTransactionIds = (transacoes: Transacao[]): number[] => {
  return transacoes
    .map((t) => t.idTransacao)
    .filter((id): id is number => id != null);
};

export const findSellsCausingNegativeBalance = (
  transacoes: Transacao[],
  selectedIds: Set<number>,
  codInvestimento: number
): number[] => {
  const txs = transacoes
    .filter(
      (t) =>
        t.idTransacao != null &&
        t.codInvestimento === codInvestimento &&
        selectedIds.has(t.idTransacao)
    )
    .slice()
    .sort(sortTransacoes);

  let qty = 0;
  const toUncheck: number[] = [];

  txs.forEach((t) => {
    const q = Number(t.quantidade) || 0;
    if (isBuy(t.tipoTransacao)) {
      qty += q;
      return;
    }
    if (qty - q < 0) {
      toUncheck.push(t.idTransacao as number);
      return;
    }
    qty -= q;
  });

  return toUncheck;
};

export const applyTransactionToggle = (
  transacoes: Transacao[],
  selectedIds: Set<number>,
  id: number,
  checked: boolean
): Set<number> => {
  const next = new Set(selectedIds);
  if (checked) {
    next.add(id);
    return next;
  }

  next.delete(id);
  const tx = transacoes.find((t) => t.idTransacao === id);
  if (!tx || !isBuy(tx.tipoTransacao) || tx.codInvestimento == null) {
    return next;
  }

  findSellsCausingNegativeBalance(transacoes, next, tx.codInvestimento).forEach((sellId) => {
    next.delete(sellId);
  });
  return next;
};
