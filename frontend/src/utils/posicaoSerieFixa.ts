import { CotacaoDia, TransacaoSerie } from '../types';
import type { FluxoAcumulado, PontoPosicao } from './posicaoSerie';

const CENTAVOS = 100;

const nomeTipo = (descricao?: string | null): string => (descricao || '').trim().toUpperCase();

export const usaRendimentoCdi = (descricao?: string | null): boolean => {
  const nome = nomeTipo(descricao);
  return nome.startsWith('LCA ') || nome.startsWith('LCI ') || nome.startsWith('CDB ');
};

export const incideIrCdi = (descricao?: string | null): boolean => nomeTipo(descricao).startsWith('CDB ');

const aliquotaIr = (dias: number): number => {
  if (dias <= 180) return 0.225;
  if (dias <= 360) return 0.2;
  if (dias <= 720) return 0.175;
  return 0.15;
};

const diasCorridos = (inicio: string, fim: string): number => {
  const [anoInicio, mesInicio, diaInicio] = inicio.split('-').map(Number);
  const [anoFim, mesFim, diaFim] = fim.split('-').map(Number);
  const ms = Date.UTC(anoFim, (mesFim || 1) - 1, diaFim || 1) - Date.UTC(anoInicio, (mesInicio || 1) - 1, diaInicio || 1);
  return Math.round(ms / 86400000);
};

const dayOf = (iso: string): string => iso.slice(0, 10);

const num = (value?: number | null): number => {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return 0;
  return Number(value);
};

const isSell = (tipo?: string | null): boolean => (tipo || '').toLowerCase() === 'sell';

const fatorDiario = (cdi: number, percentual: number): number => 1 + (cdi * percentual) / 10000;

interface LoteCdi {
  inicio: string;
  percentual: number;
  principalCents: number;
  saldoReais: number;
  quoteIndex: number;
}

interface CotacaoOrdenada {
  day: string;
  rate: number;
}

const reaisParaCentavos = (reais: number): number => Math.round(reais * CENTAVOS);

const aplicarCdi = (
  lote: LoteCdi,
  cotacoes: CotacaoOrdenada[],
  limite: string,
  incluiLimite: boolean
) => {
  while (lote.quoteIndex < cotacoes.length) {
    const cotacao = cotacoes[lote.quoteIndex];
    if (cotacao.day < lote.inicio) {
      lote.quoteIndex += 1;
      continue;
    }
    if (incluiLimite ? cotacao.day > limite : cotacao.day >= limite) return;
    lote.saldoReais *= fatorDiario(cotacao.rate, lote.percentual);
    lote.quoteIndex += 1;
  }
};

const fluxoDe = (
  gastoCents: number,
  vendasCents: number,
  saldoCents: number,
  saldoLiquidoCents: number
): FluxoAcumulado => ({
  gastoBruto: gastoCents,
  gastoLiquido: gastoCents,
  vendasBrutas: vendasCents,
  vendasLiquidas: vendasCents,
  valor: saldoCents,
  valorLiquido: saldoLiquidoCents,
});

const saldoLiquidoReais = (lote: LoteCdi, day: string, comIr: boolean): number => {
  if (!comIr) return lote.saldoReais;
  const principal = lote.principalCents / CENTAVOS;
  const rendimento = Math.max(0, lote.saldoReais - principal);
  return lote.saldoReais - rendimento * aliquotaIr(diasCorridos(lote.inicio, day));
};

export const calcularPosicaoFixa = (
  transacoes: TransacaoSerie[] | null | undefined,
  cotacoes: CotacaoDia[] | null | undefined,
  inicio: string,
  fim: string,
  comIr = false
): PontoPosicao[] => {
  const cotacoesOrdenadas: CotacaoOrdenada[] = (cotacoes || [])
    .filter((cotacao) => !!cotacao.day && cotacao.valorFechamento !== null && cotacao.valorFechamento !== undefined)
    .map((cotacao) => ({ day: cotacao.day, rate: Number(cotacao.valorFechamento) }))
    .filter((cotacao) => !Number.isNaN(cotacao.rate) && cotacao.day <= fim)
    .sort((a, b) => a.day.localeCompare(b.day));

  const porDia = new Map<string, TransacaoSerie[]>();
  (transacoes || [])
    .filter((tx) => !!tx.dataTransacao)
    .forEach((tx) => {
      const day = dayOf(tx.dataTransacao);
      if (day > fim) return;
      const lista = porDia.get(day) || [];
      lista.push(tx);
      porDia.set(day, lista);
    });

  const dias = new Set<string>();
  cotacoesOrdenadas.forEach((cotacao) => dias.add(cotacao.day));
  porDia.forEach((_, day) => dias.add(day));

  const lotes: LoteCdi[] = [];
  let gastoCents = 0;
  let vendasCents = 0;
  const pontos: PontoPosicao[] = [];

  Array.from(dias)
    .sort()
    .forEach((day) => {
      lotes.forEach((lote) => aplicarCdi(lote, cotacoesOrdenadas, day, false));

      (porDia.get(day) || [])
        .filter((tx) => !isSell(tx.tipoTransacao))
        .forEach((tx) => {
          const principalCents = num(tx.valorTotal);
          const percentual = num(tx.quantidade) || 100;
          const lote: LoteCdi = {
            inicio: day,
            percentual,
            principalCents,
            saldoReais: principalCents / CENTAVOS,
            quoteIndex: 0,
          };
          aplicarCdi(lote, cotacoesOrdenadas, day, true);
          lotes.push(lote);
          gastoCents += principalCents;
        });

      let saldoReais = lotes.reduce((total, lote) => total + lote.saldoReais, 0);
      const resgateCents = (porDia.get(day) || [])
        .filter((tx) => isSell(tx.tipoTransacao))
        .reduce((total, tx) => total + num(tx.valorTotal), 0);
      if (resgateCents > 0 && saldoReais > 0) {
        const fracao = Math.min(1, resgateCents / CENTAVOS / saldoReais);
        lotes.forEach((lote) => {
          lote.saldoReais *= 1 - fracao;
          lote.principalCents *= 1 - fracao;
        });
        vendasCents += Math.min(resgateCents, reaisParaCentavos(saldoReais));
        saldoReais = lotes.reduce((total, lote) => total + lote.saldoReais, 0);
      }

      if (day < inicio || lotes.length === 0) return;

      const saldoCents = reaisParaCentavos(saldoReais);
      const saldoLiquidoCents = reaisParaCentavos(
        lotes.reduce((total, lote) => total + saldoLiquidoReais(lote, day, comIr), 0)
      );
      const custoCents = Math.round(lotes.reduce((total, lote) => total + lote.principalCents, 0));
      const encerrado = saldoCents <= 0;
      const rentabilidadeBruta = custoCents > 0 ? saldoCents / custoCents - 1 : null;
      const rentabilidadeLiquida = custoCents > 0 ? saldoLiquidoCents / custoCents - 1 : null;
      pontos.push({
        day,
        situacao: encerrado ? 'encerrado' : 'aberto',
        valorBruto: encerrado ? 0 : saldoCents,
        valorLiquido: encerrado ? 0 : saldoLiquidoCents,
        rentabilidadeBruta,
        rentabilidadeLiquida,
        custo: custoCents,
        fluxo: fluxoDe(gastoCents, vendasCents, encerrado ? 0 : saldoCents, encerrado ? 0 : saldoLiquidoCents),
      });
    });

  return pontos;
};
