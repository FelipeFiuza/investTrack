import { calcularPosicao, chaveSerie, montarGrafico } from './posicaoSerie';

declare function describe(name: string, fn: () => void): void;
declare function it(name: string, fn: () => void): void;
interface JestMatchers {
  toBe(expected: unknown): void;
  toEqual(expected: unknown): void;
  toBeNull(): void;
  toBeCloseTo(expected: number, precision?: number): void;
}
declare function expect(actual: unknown): JestMatchers;

const compra = (
  id: number,
  data: string,
  quantidade: number,
  valorTotal: number,
  taxas = 0
) => ({
  idTransacao: id,
  dataTransacao: data,
  tipoTransacao: 'buy',
  quantidade,
  valorTotal,
  taxasImpostos: taxas,
});

const venda = (id: number, data: string, quantidade: number, valorTotal: number, taxas = 0) => ({
  idTransacao: id,
  dataTransacao: data,
  tipoTransacao: 'sell',
  quantidade,
  valorTotal,
  taxasImpostos: taxas,
});

describe('calcularPosicao', () => {
  it('usa o custo médio como fechamento no dia da compra, inclusive na primeira', () => {
    const pontos = calcularPosicao(
      [compra(1, '2024-02-15T10:00:00', 10, 1000)],
      [
        { day: '2024-02-15', valorFechamento: 150 },
        { day: '2024-02-16', valorFechamento: 120 },
      ],
      '2024-01-01',
      '2024-03-01'
    );

    const compraDia = pontos.find((ponto) => ponto.day === '2024-02-15');
    const diaSeguinte = pontos.find((ponto) => ponto.day === '2024-02-16');

    expect(compraDia?.situacao).toBe('aberto');
    expect(compraDia?.valorBruto).toBeCloseTo(1000);
    expect(compraDia?.rentabilidadeBruta).toBeCloseTo(0);
    expect(diaSeguinte?.valorBruto).toBeCloseTo(1200);
    expect(diaSeguinte?.rentabilidadeBruta).toBeCloseTo(0.2);
    expect(pontos.some((ponto) => ponto.day < '2024-02-15' && ponto.situacao !== 'intervalo')).toBe(false);
  });

  it('reancora o fechamento no novo custo médio quando há outra compra', () => {
    const pontos = calcularPosicao(
      [
        compra(1, '2024-02-01T10:00:00', 10, 1000),
        compra(2, '2024-02-10T10:00:00', 10, 2000),
      ],
      [{ day: '2024-02-10', valorFechamento: 180 }],
      '2024-02-01',
      '2024-02-28'
    );

    const reforco = pontos.find((ponto) => ponto.day === '2024-02-10');
    expect(reforco?.valorBruto).toBeCloseTo(3000);
    expect(reforco?.rentabilidadeBruta).toBeCloseTo(0.5);
  });

  it('desconta taxas acumuladas no valor líquido', () => {
    const pontos = calcularPosicao(
      [compra(1, '2024-02-01T10:00:00', 10, 1000, 50)],
      [{ day: '2024-02-02', valorFechamento: 120 }],
      '2024-02-01',
      '2024-02-28'
    );

    const abertura = pontos.find((ponto) => ponto.day === '2024-02-01');
    const mercado = pontos.find((ponto) => ponto.day === '2024-02-02');
    expect(abertura?.valorLiquido).toBeCloseTo(950);
    expect(abertura?.rentabilidadeLiquida).toBeCloseTo(-0.05);
    expect(mercado?.valorLiquido).toBeCloseTo(1150);
    expect(mercado?.rentabilidadeLiquida).toBeCloseTo(0.15);
  });

  it('mantém preço médio e quantidade restante numa venda parcial', () => {
    const pontos = calcularPosicao(
      [compra(1, '2024-02-01T10:00:00', 10, 1000), venda(2, '2024-02-05T10:00:00', 4, 600)],
      [{ day: '2024-02-05', valorFechamento: 110 }],
      '2024-02-01',
      '2024-02-28'
    );

    const parcial = pontos.find((ponto) => ponto.day === '2024-02-05');
    expect(parcial?.situacao).toBe('aberto');
    expect(parcial?.valorBruto).toBeCloseTo(660);
    expect(parcial?.rentabilidadeBruta).toBeCloseTo(0.1);
  });

  it('trava a venda integral no valor recebido e interrompe a série depois', () => {
    const pontos = calcularPosicao(
      [compra(1, '2024-02-01T10:00:00', 10, 1000), venda(2, '2024-02-10T10:00:00', 10, 1500)],
      [
        { day: '2024-02-09', valorFechamento: 140 },
        { day: '2024-02-10', valorFechamento: 155 },
        { day: '2024-02-11', valorFechamento: 160 },
      ],
      '2024-02-01',
      '2024-02-28'
    );

    const antes = pontos.find((ponto) => ponto.day === '2024-02-09');
    const saida = pontos.find((ponto) => ponto.day === '2024-02-10');
    const depois = pontos.find((ponto) => ponto.day === '2024-02-11');
    expect(antes?.valorBruto).toBeCloseTo(1400);
    expect(saida?.situacao).toBe('encerrado');
    expect(saida?.valorBruto).toBeCloseTo(1500);
    expect(saida?.rentabilidadeBruta).toBeCloseTo(0.5);
    expect(depois?.situacao).toBe('intervalo');
    expect(depois?.valorBruto).toBeNull();
  });

  it('aplica transações anteriores ao período só como saldo de abertura', () => {
    const pontos = calcularPosicao(
      [compra(1, '2024-01-10T10:00:00', 10, 1000)],
      [{ day: '2024-02-03', valorFechamento: 130 }],
      '2024-02-01',
      '2024-02-28'
    );

    expect(pontos.map((ponto) => ponto.day)).toEqual(['2024-02-03']);
    expect(pontos[0].valorBruto).toBeCloseTo(1300);
    expect(pontos[0].rentabilidadeBruta).toBeCloseTo(0);
  });

  it('mede a rentabilidade pelo fechamento da data inicial', () => {
    const pontos = calcularPosicao(
      [compra(1, '2024-01-10T10:00:00', 10, 1000)],
      [
        { day: '2024-01-31', valorFechamento: 105 },
        { day: '2026-02-02', valorFechamento: 110 },
        { day: '2026-02-03', valorFechamento: 121 },
      ],
      '2026-02-02',
      '2026-02-28'
    );

    const inicio = pontos.find((ponto) => ponto.day === '2026-02-02');
    const seguinte = pontos.find((ponto) => ponto.day === '2026-02-03');
    expect(inicio?.rentabilidadeBruta).toBeCloseTo(0);
    expect(inicio?.valorBruto).toBeCloseTo(1100);
    expect(seguinte?.rentabilidadeBruta).toBeCloseTo(121 / 110 - 1);
    expect(seguinte?.valorBruto).toBeCloseTo(1210);
  });

  it('usa o último fechamento vigente quando a data inicial não tem pregão', () => {
    const pontos = calcularPosicao(
      [compra(1, '2024-01-10T10:00:00', 10, 1000)],
      [
        { day: '2026-01-30', valorFechamento: 110 },
        { day: '2026-02-02', valorFechamento: 121 },
      ],
      '2026-02-01',
      '2026-02-28'
    );

    const segunda = pontos.find((ponto) => ponto.day === '2026-02-02');
    expect(pontos.some((ponto) => ponto.day === '2026-01-30')).toBe(false);
    expect(segunda?.rentabilidadeBruta).toBeCloseTo(121 / 110 - 1);
  });

  it('registra a primeira compra pelo preço pago quando o período começa zerado', () => {
    const pontos = calcularPosicao(
      [compra(1, '2024-02-15T10:00:00', 10, 1000)],
      [
        { day: '2024-01-02', valorFechamento: 80 },
        { day: '2024-02-15', valorFechamento: 150 },
        { day: '2024-02-16', valorFechamento: 120 },
      ],
      '2024-01-01',
      '2024-03-01'
    );

    const compraDia = pontos.find((ponto) => ponto.day === '2024-02-15');
    const seguinte = pontos.find((ponto) => ponto.day === '2024-02-16');
    expect(compraDia?.valorBruto).toBeCloseTo(1000);
    expect(compraDia?.rentabilidadeBruta).toBeCloseTo(0);
    expect(seguinte?.valorBruto).toBeCloseTo(1200);
    expect(seguinte?.rentabilidadeBruta).toBeCloseTo(0.2);
  });

  it('reabre depois da venda integral pelo preço da nova compra', () => {
    const pontos = calcularPosicao(
      [
        compra(1, '2024-02-01T10:00:00', 10, 1000),
        venda(2, '2024-02-10T10:00:00', 10, 1500),
        compra(3, '2024-03-05T10:00:00', 4, 800),
      ],
      [
        { day: '2024-02-01', valorFechamento: 90 },
        { day: '2024-02-10', valorFechamento: 155 },
        { day: '2024-03-05', valorFechamento: 250 },
        { day: '2024-03-06', valorFechamento: 220 },
      ],
      '2024-02-01',
      '2024-03-31'
    );

    const recompra = pontos.find((ponto) => ponto.day === '2024-03-05');
    const depois = pontos.find((ponto) => ponto.day === '2024-03-06');
    expect(recompra?.valorBruto).toBeCloseTo(800);
    expect(recompra?.rentabilidadeBruta).toBeCloseTo(0);
    expect(depois?.rentabilidadeBruta).toBeCloseTo(220 / 200 - 1);
  });
});

describe('montarGrafico', () => {
  it('mantém as extremidades do período configurado', () => {
    const grafico = montarGrafico(
      [
        {
          codInvestimento: 7,
          transacoes: [compra(1, '2024-02-15T10:00:00', 10, 1000)],
          cotacoes: [
            { day: '2024-02-15', valorFechamento: 150 },
            { day: '2024-02-16', valorFechamento: 120 },
          ],
        },
      ],
      'gross',
      'value',
      '2024-01-01',
      '2024-03-01'
    );

    expect(grafico.dados[0].day).toBe('2024-01-01');
    expect(grafico.dados[0].total).toBeNull();
    expect(grafico.dados[grafico.dados.length - 1].day).toBe('2024-03-01');
    expect(grafico.dados[grafico.dados.length - 1].total).toBeNull();
    expect(grafico.dados.find((ponto) => ponto.day === '2024-02-15')?.[chaveSerie(7)]).toBeCloseTo(1000);
    expect(grafico.codigos).toEqual([7]);
  });

  it('não liga a linha de novo depois da venda integral', () => {
    const grafico = montarGrafico(
      [
        {
          codInvestimento: 3,
          transacoes: [
            compra(1, '2024-02-01T10:00:00', 10, 1000),
            venda(2, '2024-02-10T10:00:00', 10, 1500),
          ],
          cotacoes: [
            { day: '2024-02-10', valorFechamento: 155 },
            { day: '2024-02-11', valorFechamento: 160 },
          ],
        },
      ],
      'gross',
      'value',
      '2024-02-01',
      '2024-02-28'
    );

    const saida = grafico.dados.find((ponto) => ponto.day === '2024-02-10');
    const depois = grafico.dados.find((ponto) => ponto.day === '2024-02-11');
    expect(saida?.[chaveSerie(3)]).toBeCloseTo(1500);
    expect(saida?.total).toBeCloseTo(1500);
    expect(depois?.[chaveSerie(3)]).toBeNull();
    expect(depois?.total).toBeNull();
  });

  it('não religa a série quando a recompra acontece depois, sem cotações no meio', () => {
    const grafico = montarGrafico(
      [
        {
          codInvestimento: 3,
          transacoes: [
            compra(1, '2024-02-01T10:00:00', 10, 1000),
            venda(2, '2024-02-10T10:00:00', 10, 1500),
            compra(3, '2024-03-05T10:00:00', 4, 800),
          ],
          cotacoes: [{ day: '2024-02-10', valorFechamento: 155 }],
        },
      ],
      'gross',
      'value',
      '2024-02-01',
      '2024-03-31'
    );

    expect(grafico.dados.find((ponto) => ponto.day === '2024-02-11')?.[chaveSerie(3)]).toBeNull();
    expect(grafico.dados.find((ponto) => ponto.day === '2024-03-05')?.[chaveSerie(3)]).toBeCloseTo(800);
  });

  it('não joga o custo de uma recompra para o dia seguinte à venda', () => {
    const grafico = montarGrafico(
      [
        {
          codInvestimento: 3,
          transacoes: [
            compra(1, '2025-01-20T10:00:00', 10, 1000),
            venda(2, '2025-01-24T10:00:00', 10, 1100),
            compra(3, '2025-11-10T10:00:00', 4, 2000),
          ],
          cotacoes: [
            { day: '2025-01-23', valorFechamento: 108 },
            { day: '2025-01-26', valorFechamento: 90 },
          ],
        },
      ],
      'gross',
      'return',
      '2025-01-01',
      '2025-12-31'
    );

    expect(grafico.dados.find((ponto) => ponto.day === '2025-01-24')?.total).toBeCloseTo(0.1);
    expect(grafico.dados.find((ponto) => ponto.day === '2025-01-25')?.[chaveSerie(3)]).toBeNull();
    expect(grafico.dados.find((ponto) => ponto.day === '2025-01-25')?.total).toBeCloseTo(0.1);
    expect(grafico.dados.find((ponto) => ponto.day === '2025-01-26')?.total).toBeCloseTo(0.1);
    expect(grafico.dados.find((ponto) => ponto.day === '2025-11-10')?.total).toBeCloseTo(3100 / 3000 - 1);
  });

  it('pondera a rentabilidade da carteira pelo fechamento inicial', () => {
    const grafico = montarGrafico(
      [
        {
          codInvestimento: 3,
          transacoes: [compra(1, '2024-01-10T10:00:00', 10, 1000)],
          cotacoes: [
            { day: '2026-02-02', valorFechamento: 110 },
            { day: '2026-02-03', valorFechamento: 121 },
          ],
        },
      ],
      'gross',
      'return',
      '2026-02-02',
      '2026-02-28'
    );

    expect(grafico.dados.find((ponto) => ponto.day === '2026-02-02')?.total).toBeCloseTo(0);
    expect(grafico.dados.find((ponto) => ponto.day === '2026-02-03')?.[chaveSerie(3)]).toBeCloseTo(0.1);
    expect(grafico.dados.find((ponto) => ponto.day === '2026-02-03')?.total).toBeCloseTo(0.1);
  });

  it('mantém as vendas no acumulado depois que a linha do ativo para', () => {
    const grafico = montarGrafico(
      [
        {
          codInvestimento: 3,
          transacoes: [
            compra(1, '2024-01-10T10:00:00', 10, 800),
            venda(2, '2026-02-03T10:00:00', 10, 1500),
          ],
          cotacoes: [
            { day: '2026-02-02', valorFechamento: 100 },
            { day: '2026-02-03', valorFechamento: 140 },
            { day: '2026-02-04', valorFechamento: 90 },
          ],
        },
      ],
      'gross',
      'return',
      '2026-02-02',
      '2026-02-28'
    );

    expect(grafico.dados.find((ponto) => ponto.day === '2026-02-03')?.[chaveSerie(3)]).toBeCloseTo(0.5);
    expect(grafico.dados.find((ponto) => ponto.day === '2026-02-04')?.[chaveSerie(3)]).toBeNull();
    expect(grafico.dados.find((ponto) => ponto.day === '2026-02-03')?.total).toBeCloseTo(0.5);
    expect(grafico.dados.find((ponto) => ponto.day === '2026-02-04')?.total).toBeCloseTo(0.5);
  });

  it('soma vendas, valor de mercado e compras de todos os ativos', () => {
    const grafico = montarGrafico(
      [
        {
          codInvestimento: 3,
          transacoes: [
            compra(1, '2024-01-10T10:00:00', 10, 800),
            venda(2, '2026-02-03T10:00:00', 4, 600),
          ],
          cotacoes: [
            { day: '2026-02-02', valorFechamento: 100 },
            { day: '2026-02-03', valorFechamento: 110 },
          ],
        },
        {
          codInvestimento: 7,
          transacoes: [compra(3, '2026-02-03T10:00:00', 5, 500)],
          cotacoes: [{ day: '2026-02-03', valorFechamento: 140 }],
        },
      ],
      'gross',
      'return',
      '2026-02-02',
      '2026-02-28'
    );

    const dia = grafico.dados.find((ponto) => ponto.day === '2026-02-03');
    expect(dia?.[chaveSerie(7)]).toBeCloseTo(0);
    expect(dia?.total).toBeCloseTo(1760 / 1500 - 1);
  });

  it('não zera o acumulado no dia de uma compra adicional', () => {
    const grafico = montarGrafico(
      [
        {
          codInvestimento: 3,
          transacoes: [
            compra(1, '2024-01-10T10:00:00', 10, 1000),
            compra(2, '2026-02-03T10:00:00', 10, 1300),
          ],
          cotacoes: [
            { day: '2026-02-02', valorFechamento: 100 },
            { day: '2026-02-03', valorFechamento: 130 },
          ],
        },
      ],
      'gross',
      'return',
      '2026-02-02',
      '2026-02-28'
    );

    const dia = grafico.dados.find((ponto) => ponto.day === '2026-02-03');
    expect(dia?.[chaveSerie(3)]).toBeCloseTo(0.15);
    expect(dia?.total).toBeCloseTo(2600 / 2300 - 1);
  });

  it('desconta as taxas do período no acumulado líquido', () => {
    const grafico = montarGrafico(
      [
        {
          codInvestimento: 3,
          transacoes: [
            compra(1, '2024-01-10T10:00:00', 10, 800),
            venda(2, '2026-02-03T10:00:00', 10, 1500, 30),
          ],
          cotacoes: [
            { day: '2026-02-02', valorFechamento: 100 },
            { day: '2026-02-03', valorFechamento: 140 },
          ],
        },
      ],
      'net',
      'return',
      '2026-02-02',
      '2026-02-28'
    );

    expect(grafico.dados.find((ponto) => ponto.day === '2026-02-03')?.total).toBeCloseTo(0.47);
  });
});
