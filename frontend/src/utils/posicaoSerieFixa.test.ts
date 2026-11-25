import { calcularPosicaoFixa, incideIrCdi, usaRendimentoCdi } from './posicaoSerieFixa';
import { montarGrafico } from './posicaoSerie';
import { cotacoesCdiLca } from './cotacoesCdiLca';

declare function describe(name: string, fn: () => void): void;
declare function it(name: string, fn: () => void): void;
interface JestMatchers {
  toBe(expected: unknown): void;
  toBeCloseTo(expected: number, precision?: number): void;
  toBeGreaterThan(expected: number): void;
}
declare function expect(actual: unknown): JestMatchers;

const reais = (centavos?: number | null): number => Number(centavos || 0) / 100;

describe('calcularPosicaoFixa', () => {
  const compraLca = {
    idTransacao: 142,
    dataTransacao: '2025-10-06T00:00:00',
    tipoTransacao: 'buy',
    quantidade: 120,
    valorTotal: 1000000,
  };

  it('reconhece LCA, LCI e CDB como rendimento de CDI', () => {
    expect(usaRendimentoCdi('LCA CDI (Ex: 100% CDI)')).toBe(true);
    expect(usaRendimentoCdi('LCI CDI (Ex: 100% CDI)')).toBe(true);
    expect(usaRendimentoCdi('CDB CDI (Ex: 100% CDI)')).toBe(true);
    expect(usaRendimentoCdi('PETR4 - PETROBRAS')).toBe(false);
    expect(incideIrCdi('CDB CDI (Ex: 100% CDI)')).toBe(true);
    expect(incideIrCdi('LCA CDI (Ex: 100% CDI)')).toBe(false);
    expect(incideIrCdi('LCI CDI (Ex: 100% CDI)')).toBe(false);
  });

  it('aplica o CDI do dia da compra sobre 120% e reproduz a calculadora do cidadão', () => {
    const pontos = calcularPosicaoFixa([compraLca], cotacoesCdiLca, '2025-10-06', '2026-11-09');
    const compra = pontos.find((ponto) => ponto.day === '2025-10-06');
    const fechamento = pontos.find((ponto) => ponto.day === '2026-11-09');

    expect(reais(compra?.valorBruto)).toBeCloseTo(10000 * (1 + (0.055131 * 120) / 10000), 2);
    expect(reais(fechamento?.valorBruto)).toBeCloseTo(11917.86, 2);
    expect(reais(fechamento?.valorBruto)).toBeGreaterThan(reais(compra?.valorBruto));
  });

  it('entra no gráfico de posição quando o investimento é LCA ou LCI', () => {
    const grafico = montarGrafico(
      [{
        codInvestimento: 1,
        descricao: 'LCI CDI (Ex: 100% CDI)',
        transacoes: [compraLca],
        cotacoes: cotacoesCdiLca,
      }],
      'gross',
      'value',
      '2025-10-06',
      '2026-11-09'
    );
    const ponto = grafico.dados.find((item) => item.day === '2026-11-09');
    expect(reais(ponto?.inv_1 as number)).toBeCloseTo(11917.86, 2);
  });

  const somarDias = (iso: string, dias: number): string => {
    const [ano, mes, dia] = iso.split('-').map(Number);
    return new Date(Date.UTC(ano, mes - 1, dia + dias)).toISOString().slice(0, 10);
  };

  const compraCdb = {
    idTransacao: 200,
    dataTransacao: '2025-01-01T00:00:00',
    tipoTransacao: 'buy',
    quantidade: 100,
    valorTotal: 1000000,
  };

  const cotacoesFaixa = (alvo: string) => [
    { day: '2025-01-01', valorFechamento: 1 },
    { day: alvo, valorFechamento: 0 },
  ];

  it('desconta o IR regressivo do rendimento quando o painel pede valores líquidos', () => {
    const faixas: Array<[number, number]> = [
      [180, 0.225],
      [181, 0.2],
      [360, 0.2],
      [361, 0.175],
      [720, 0.175],
      [721, 0.15],
    ];
    faixas.forEach(([dias, aliquota]) => {
      const alvo = somarDias('2025-01-01', dias);
      const pontos = calcularPosicaoFixa([compraCdb], cotacoesFaixa(alvo), '2025-01-01', alvo, true);
      const ponto = pontos.find((item) => item.day === alvo);
      const bruto = 10100;
      const liquido = bruto - (bruto - 10000) * aliquota;
      expect(reais(ponto?.valorBruto)).toBeCloseTo(bruto, 2);
      expect(reais(ponto?.valorLiquido)).toBeCloseTo(liquido, 2);
    });
  });

  it('mantém o CDB bruto igual à LCA e exibe o líquido com IR no gráfico', () => {
    const grafico = montarGrafico(
      [{
        codInvestimento: 2,
        descricao: 'CDB CDI (Ex: 100% CDI)',
        transacoes: [compraLca],
        cotacoes: cotacoesCdiLca,
      }],
      'net',
      'value',
      '2025-10-06',
      '2026-11-09'
    );
    const ponto = grafico.dados.find((item) => item.day === '2026-11-09');
    const liquido = 11917.86 - (11917.86 - 10000) * 0.175;
    expect(reais(ponto?.inv_2 as number)).toBeCloseTo(liquido, 2);
  });
});