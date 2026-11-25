import { calcularPosicaoFixa, incideIrCdi, usaRendimentoCdi } from './posicaoSerieFixa';

export interface TransacaoSerie {
  idTransacao?: number | null;
  dataTransacao: string;
  tipoTransacao?: string | null;
  quantidade?: number | null;
  valorTotal?: number | null;
  valorUnitario?: number | null;
  taxasImpostos?: number | null;
}

export interface CotacaoDia {
  day: string;
  valorFechamento?: number | null;
}

export interface InvestimentoBruto {
  codInvestimento: number;
  descricao?: string | null;
  transacoes?: TransacaoSerie[] | null;
  cotacoes?: CotacaoDia[] | null;
}

export type SituacaoPosicao = 'aberto' | 'encerrado' | 'intervalo';
export type BaseValor = 'gross' | 'net';
export type MetricaGrafico = 'value' | 'return';

export interface FluxoAcumulado {
  gastoBruto: number;
  gastoLiquido: number;
  vendasBrutas: number;
  vendasLiquidas: number;
  valor: number;
  valorLiquido?: number;
}

export interface PontoPosicao {
  day: string;
  situacao: SituacaoPosicao;
  valorBruto: number | null;
  valorLiquido: number | null;
  rentabilidadeBruta: number | null;
  rentabilidadeLiquida: number | null;
  custo: number;
  fluxo: FluxoAcumulado;
}

export interface DadoGrafico {
  day: string;
  ts: number;
  total: number | null;
  [key: string]: string | number | null;
}

export interface GraficoCalculado {
  dados: DadoGrafico[];
  codigos: number[];
}

export const CHAVE_TOTAL = 'total';

const EPS = 1e-6;

export const chaveSerie = (codInvestimento: number): string => `inv_${codInvestimento}`;

export const diaParaTimestamp = (day: string): number => {
  const [year, month, date] = day.split('-').map(Number);
  return new Date(year, (month || 1) - 1, date || 1).getTime();
};

const num = (value?: number | null): number => {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return 0;
  return Number(value);
};

const numOrNull = (value?: number | null): number | null => {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return null;
  return Number(value);
};

const dayOf = (iso: string): string => iso.slice(0, 10);

const addDays = (day: string, amount: number): string => {
  const [year, month, date] = day.split('-').map(Number);
  const value = new Date(year, (month || 1) - 1, date || 1);
  value.setDate(value.getDate() + amount);
  const y = value.getFullYear();
  const m = String(value.getMonth() + 1).padStart(2, '0');
  const d = String(value.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
};

const isSell = (tipo?: string | null): boolean => (tipo || '').toLowerCase() === 'sell';

const unitPrice = (tx: TransacaoSerie): number => {
  const unit = numOrNull(tx.valorUnitario);
  if (unit !== null) return unit;
  const qty = num(tx.quantidade);
  const total = numOrNull(tx.valorTotal);
  if (qty !== 0 && total !== null) return total / qty;
  return total ?? 0;
};

const custoTransacao = (tx: TransacaoSerie): number => {
  const total = numOrNull(tx.valorTotal);
  if (total !== null) return total;
  const unit = numOrNull(tx.valorUnitario);
  const qty = num(tx.quantidade);
  if (unit !== null) return unit * qty;
  return unitPrice(tx) * qty;
};

interface EstadoPosicao {
  qty: number;
  costBasis: number;
  avg: number;
  boughtTaxes: number;
  soldTaxes: number;
}

interface DiaAcumulado {
  boughtQty: number;
  boughtCost: number;
  boughtTaxes: number;
  soldApplied: number;
  proceeds: number;
  soldTaxes: number;
  custoVendido: number;
}

const novoEstado = (): EstadoPosicao => ({
  qty: 0,
  costBasis: 0,
  avg: 0,
  boughtTaxes: 0,
  soldTaxes: 0,
});

const atualizarPrecoMedio = (estado: EstadoPosicao) => {
  estado.avg = estado.qty <= EPS ? 0 : estado.costBasis / estado.qty;
};

const aplicarTransacao = (estado: EstadoPosicao, tx: TransacaoSerie, dia: DiaAcumulado | null) => {
  const qtyTx = num(tx.quantidade);
  const taxas = num(tx.taxasImpostos);
  const custo = custoTransacao(tx);

  if (isSell(tx.tipoTransacao)) {
    const applied = Math.min(Math.max(qtyTx, 0), Math.max(estado.qty, 0));
    if (dia && applied > EPS && estado.qty > EPS) {
      dia.soldApplied += applied;
      dia.custoVendido += (estado.costBasis * applied) / estado.qty;
      const baseQty = qtyTx > EPS ? qtyTx : applied;
      dia.proceeds += (custo * applied) / baseQty;
      dia.soldTaxes += taxas;
    }
    if (estado.qty > EPS) {
      estado.costBasis = (estado.costBasis * (estado.qty - applied)) / estado.qty;
    }
    estado.qty -= applied;
    estado.soldTaxes += taxas;
    if (estado.qty <= EPS) {
      estado.qty = 0;
      estado.costBasis = 0;
      estado.avg = 0;
    } else {
      atualizarPrecoMedio(estado);
    }
    return;
  }

  estado.costBasis += custo;
  estado.qty += qtyTx;
  atualizarPrecoMedio(estado);
  estado.boughtTaxes += taxas;
  if (dia) {
    dia.boughtQty += qtyTx;
    dia.boughtCost += custo;
    dia.boughtTaxes += taxas;
  }
};

const rentabilidadeDoPreco = (preco: number | null, base: number | null): number | null => {
  if (preco === null || base === null || base <= EPS) return null;
  return preco / base - 1;
};

const pontoAberto = (
  day: string,
  estado: EstadoPosicao,
  fechamento: number | null,
  diaDeCompra: boolean,
  basePreco: number | null
): PontoPosicao => {
  const preco = diaDeCompra && estado.avg > EPS ? estado.avg : fechamento;
  const valorBruto = preco === null ? null : preco * estado.qty;
  const valorLiquido = valorBruto === null ? null : valorBruto - estado.boughtTaxes - estado.soldTaxes;
  const precoLiquido = valorLiquido !== null && estado.qty > EPS ? valorLiquido / estado.qty : null;
  return {
    day,
    situacao: 'aberto',
    valorBruto,
    valorLiquido,
    rentabilidadeBruta: rentabilidadeDoPreco(preco, basePreco),
    rentabilidadeLiquida: rentabilidadeDoPreco(precoLiquido, basePreco),
    custo: basePreco !== null && estado.qty > EPS ? basePreco * estado.qty : 0,
    fluxo: fluxoVazio(),
  };
};

const pontoEncerrado = (
  day: string,
  dia: DiaAcumulado,
  estado: EstadoPosicao,
  basePreco: number | null
): PontoPosicao => {
  const valorLiquido = dia.proceeds - estado.boughtTaxes - estado.soldTaxes;
  const precoVenda = dia.soldApplied > EPS ? dia.proceeds / dia.soldApplied : null;
  const precoLiquido = dia.soldApplied > EPS ? valorLiquido / dia.soldApplied : null;
  return {
    day,
    situacao: 'encerrado',
    valorBruto: dia.proceeds,
    valorLiquido,
    rentabilidadeBruta: rentabilidadeDoPreco(precoVenda, basePreco),
    rentabilidadeLiquida: rentabilidadeDoPreco(precoLiquido, basePreco),
    custo: basePreco !== null && dia.soldApplied > EPS ? basePreco * dia.soldApplied : 0,
    fluxo: fluxoVazio(),
  };
};

const fluxoVazio = (): FluxoAcumulado => ({
  gastoBruto: 0,
  gastoLiquido: 0,
  vendasBrutas: 0,
  vendasLiquidas: 0,
  valor: 0,
});

const comporFluxo = (
  abertura: number,
  compras: number,
  taxasCompra: number,
  vendas: number,
  taxasVenda: number,
  valor: number
): FluxoAcumulado => ({
  gastoBruto: abertura + compras,
  gastoLiquido: abertura + compras + taxasCompra,
  vendasBrutas: vendas,
  vendasLiquidas: vendas - taxasVenda,
  valor,
});

const pontoIntervalo = (day: string): PontoPosicao => ({
  day,
  situacao: 'intervalo',
  valorBruto: null,
  valorLiquido: null,
  rentabilidadeBruta: null,
  rentabilidadeLiquida: null,
  custo: 0,
  fluxo: fluxoVazio(),
});

const ordenarTransacoes = (transacoes: TransacaoSerie[]): TransacaoSerie[] => {
  return transacoes
    .filter((tx) => !!tx.dataTransacao)
    .slice()
    .sort((a, b) => {
      const porData = a.dataTransacao.localeCompare(b.dataTransacao);
      if (porData !== 0) return porData;
      return num(a.idTransacao) - num(b.idTransacao);
    });
};

export const calcularPosicao = (
  transacoes: TransacaoSerie[] | null | undefined,
  cotacoes: CotacaoDia[] | null | undefined,
  inicio: string,
  fim: string
): PontoPosicao[] => {
  const quotes = new Map<string, number>();
  let fechamentoInicio: number | null = null;
  let diaBase: string | null = null;
  let primeiroNoPeriodo: number | null = null;
  let diaPrimeiroNoPeriodo: string | null = null;
  (cotacoes || []).forEach((cotacao) => {
    if (!cotacao.day || cotacao.day > fim) return;
    const valor = numOrNull(cotacao.valorFechamento);
    if (valor === null) return;
    if (cotacao.day >= inicio) quotes.set(cotacao.day, valor);
    if (cotacao.day <= inicio && (diaBase === null || cotacao.day >= diaBase)) {
      diaBase = cotacao.day;
      fechamentoInicio = valor;
    }
    if (cotacao.day >= inicio && (diaPrimeiroNoPeriodo === null || cotacao.day < diaPrimeiroNoPeriodo)) {
      diaPrimeiroNoPeriodo = cotacao.day;
      primeiroNoPeriodo = valor;
    }
  });
  const estado = novoEstado();
  const porDia = new Map<string, TransacaoSerie[]>();
  ordenarTransacoes(transacoes || []).forEach((tx) => {
    const day = dayOf(tx.dataTransacao);
    if (day < inicio) {
      aplicarTransacao(estado, tx, null);
      return;
    }
    if (day > fim) return;
    const lista = porDia.get(day) || [];
    lista.push(tx);
    porDia.set(day, lista);
  });

  const days = new Set<string>();
  quotes.forEach((_, day) => days.add(day));
  porDia.forEach((_, day) => days.add(day));

  let ultimoFechamento: number | null = fechamentoInicio;
  let encerrada = false;
  let basePreco: number | null = estado.qty > EPS ? fechamentoInicio ?? primeiroNoPeriodo : null;
  const qtyNoInicio = estado.qty;
  let abertura = 0;
  let aberturaDefinida = false;
  if (qtyNoInicio > EPS && fechamentoInicio !== null) {
    abertura = fechamentoInicio * qtyNoInicio;
    aberturaDefinida = true;
  }
  let compras = 0;
  let taxasCompra = 0;
  let vendas = 0;
  let taxasVenda = 0;
  const pontos: PontoPosicao[] = [];

  const fluxoDoDia = (valor: number): FluxoAcumulado =>
    comporFluxo(abertura, compras, taxasCompra, vendas, taxasVenda, valor);

  Array.from(days)
    .sort()
    .forEach((day) => {
      if (quotes.has(day)) ultimoFechamento = quotes.get(day) ?? ultimoFechamento;
      if (!aberturaDefinida && qtyNoInicio > EPS && quotes.has(day)) {
        abertura = (quotes.get(day) ?? 0) * qtyNoInicio;
        aberturaDefinida = true;
      }
      const dia: DiaAcumulado = {
        boughtQty: 0,
        boughtCost: 0,
        boughtTaxes: 0,
        soldApplied: 0,
        proceeds: 0,
        soldTaxes: 0,
        custoVendido: 0,
      };
      const qtyAntes = estado.qty;
      (porDia.get(day) || []).forEach((tx) => aplicarTransacao(estado, tx, dia));
      compras += dia.boughtCost;
      taxasCompra += dia.boughtTaxes;
      vendas += dia.proceeds;
      taxasVenda += dia.soldTaxes;

      if (qtyAntes <= EPS && dia.boughtQty > EPS && estado.qty > EPS && estado.avg > EPS) {
        basePreco = estado.avg;
      }

      const encerrou =
        estado.qty <= EPS && dia.soldApplied > EPS && (qtyAntes > EPS || dia.boughtQty > EPS);
      const fechamento = quotes.has(day) ? quotes.get(day) ?? null : ultimoFechamento;
      const abriuDoZero = qtyAntes <= EPS && dia.boughtQty > EPS && estado.qty > EPS;
      let valorMercado = 0;
      if (estado.qty > EPS) {
        if (abriuDoZero && estado.avg > EPS) {
          valorMercado = estado.avg * estado.qty;
        } else if (fechamento !== null) {
          valorMercado = fechamento * estado.qty;
        } else if (estado.avg > EPS) {
          valorMercado = estado.avg * estado.qty;
        }
      }

      if (encerrou) {
        const ponto = pontoEncerrado(day, dia, estado, basePreco);
        ponto.fluxo = fluxoDoDia(0);
        pontos.push(ponto);
        encerrada = true;
        return;
      }
      if (estado.qty > EPS) {
        if (encerrada) {
          const saida = pontos.slice().reverse().find((ponto) => ponto.situacao === 'encerrado');
          if (saida) {
            const gapDay = addDays(saida.day, 1);
            if (gapDay < day && !pontos.some((ponto) => ponto.day === gapDay)) {
              const intervalo = pontoIntervalo(gapDay);
              intervalo.fluxo = { ...saida.fluxo };
              pontos.push(intervalo);
            }
          }
        }
        const ponto = pontoAberto(day, estado, fechamento, dia.boughtQty > EPS, basePreco);
        ponto.fluxo = fluxoDoDia(valorMercado);
        pontos.push(ponto);
        encerrada = false;
        return;
      }
      if (encerrada) {
        const intervalo = pontoIntervalo(day);
        intervalo.fluxo = fluxoDoDia(0);
        pontos.push(intervalo);
      }
    });

  return pontos;
};

interface SlotSerie {
  situacao: SituacaoPosicao;
  valor: number | null;
  amount: number | null;
  custo: number;
  fluxo: FluxoAcumulado;
}

const slotDe = (ponto: PontoPosicao, basis: BaseValor, metric: MetricaGrafico): SlotSerie => {
  const amount = basis === 'gross' ? ponto.valorBruto : ponto.valorLiquido;
  const rate = basis === 'gross' ? ponto.rentabilidadeBruta : ponto.rentabilidadeLiquida;
  return {
    situacao: ponto.situacao,
    valor: metric === 'value' ? amount : rate,
    amount,
    custo: ponto.custo,
    fluxo: ponto.fluxo,
  };
};

const valorExibido = (slot: SlotSerie | undefined, carregado: SlotSerie | null): number | null => {
  if (!slot) return carregado && carregado.situacao === 'aberto' ? carregado.valor : null;
  if (slot.situacao === 'intervalo') return null;
  if (slot.valor === null && slot.situacao === 'aberto' && carregado) return carregado.valor;
  return slot.valor;
};

const slotCarregado = (slot: SlotSerie | undefined, carregado: SlotSerie | null): SlotSerie | null => {
  if (!slot) return carregado && carregado.situacao === 'aberto' ? carregado : null;
  if (slot.situacao === 'aberto' && slot.valor !== null) return slot;
  if (slot.situacao === 'aberto') return carregado;
  return null;
};

export const montarGrafico = (
  investimentos: InvestimentoBruto[],
  basis: BaseValor,
  metric: MetricaGrafico,
  inicio: string,
  fim: string
): GraficoCalculado => {
  const series = investimentos.map((inv) => ({
    cod: inv.codInvestimento,
    pontos: usaRendimentoCdi(inv.descricao)
      ? calcularPosicaoFixa(inv.transacoes, inv.cotacoes, inicio, fim, incideIrCdi(inv.descricao))
      : calcularPosicao(inv.transacoes, inv.cotacoes, inicio, fim),
  }));
  const codigos = series
    .filter((serie) => serie.pontos.some((ponto) => ponto.situacao !== 'intervalo'))
    .map((serie) => serie.cod);

  const porDia = new Map<number, Map<string, SlotSerie>>();
  const diasReais = new Set<string>();
  series.forEach((serie) => {
    const mapa = new Map<string, SlotSerie>();
    serie.pontos.forEach((ponto) => {
      mapa.set(ponto.day, slotDe(ponto, basis, metric));
      diasReais.add(ponto.day);
    });
    porDia.set(serie.cod, mapa);
  });

  const dias = new Set<string>(diasReais);
  if (inicio) dias.add(inicio);
  if (fim) dias.add(fim);

  const dados = Array.from(dias)
    .sort()
    .map((day) => {
      const ponto: DadoGrafico = { day, ts: diaParaTimestamp(day), total: null };
      codigos.forEach((cod) => {
        ponto[chaveSerie(cod)] = null;
      });
      return ponto;
    });

  const carregados = new Map<number, SlotSerie | null>();
  const fluxos = new Map<number, FluxoAcumulado | null>();
  codigos.forEach((cod) => {
    carregados.set(cod, null);
    fluxos.set(cod, null);
  });

  dados.forEach((ponto) => {
    const sintetico = !diasReais.has(ponto.day);
    let amount = 0;
    let hasValue = false;

    codigos.forEach((cod) => {
      const mapa = porDia.get(cod);
      const slot = mapa ? mapa.get(ponto.day) : undefined;
      const anterior = carregados.get(cod) || null;
      if (sintetico) {
        ponto[chaveSerie(cod)] = null;
        return;
      }
      if (slot) fluxos.set(cod, slot.fluxo);
      const valor = valorExibido(slot, anterior);
      ponto[chaveSerie(cod)] = valor;
      carregados.set(cod, slotCarregado(slot, anterior));
      if (metric !== 'value' || valor === null) return;
      amount += valor;
      hasValue = true;
    });

    if (metric === 'value') {
      ponto.total = hasValue ? amount : null;
      return;
    }
    if (sintetico) {
      ponto.total = null;
      return;
    }

    let gasto = 0;
    let vendas = 0;
    let valorAtual = 0;
    let participa = false;
    fluxos.forEach((fluxo) => {
      if (!fluxo) return;
      const gastoAtivo = basis === 'gross' ? fluxo.gastoBruto : fluxo.gastoLiquido;
      const vendasAtivo = basis === 'gross' ? fluxo.vendasBrutas : fluxo.vendasLiquidas;
      if (gastoAtivo <= EPS && vendasAtivo <= EPS && fluxo.valor <= EPS) return;
      gasto += gastoAtivo;
      vendas += vendasAtivo;
      valorAtual += basis === 'net' && fluxo.valorLiquido !== undefined ? fluxo.valorLiquido : fluxo.valor;
      participa = true;
    });
    ponto.total = participa && gasto > EPS ? (vendas + valorAtual) / gasto - 1 : null;
  });

  return { dados, codigos };
};
