import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { dashboardApi } from '../services/api';
import { DashboardPosicaoResponse, InvestimentoSerie, PosicaoDiaria } from '../types';
import AppLayout from '../components/AppLayout';
import './DashboardPage.css';

type AmountBasis = 'gross' | 'net';
type MetricKind = 'value' | 'return';

const PERIOD_MONTHS_MAX = 60;
const PERIOD_MONTHS_DEFAULT = 24;
const TOTAL_KEY = 'total';
const SERIES_COLORS = [
  '#667eea',
  '#48bb78',
  '#ed8936',
  '#f56565',
  '#38b2ac',
  '#9f7aea',
  '#d69e2e',
  '#4299e1',
  '#ed64a6',
  '#68d391',
];

const currencyFmt = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
});

const compactCurrencyFmt = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
  maximumFractionDigits: 0,
});

const percentFmt = new Intl.NumberFormat('pt-BR', {
  style: 'percent',
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

const getUserId = (): number => {
  try {
    const raw = localStorage.getItem('user');
    if (!raw) return 1;
    const user = JSON.parse(raw);
    return user.idUsuario || 1;
  } catch {
    return 1;
  }
};

const toISODate = (date: Date): string => {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
};

const shiftMonth = (base: Date, months: number): Date => {
  return new Date(base.getFullYear(), base.getMonth() + months, 1);
};

const dateFromOffset = (offset: number, isEnd: boolean): Date => {
  const now = new Date();
  if (isEnd && offset >= PERIOD_MONTHS_MAX) {
    return now;
  }
  const firstOfMonth = shiftMonth(now, -(PERIOD_MONTHS_MAX - offset));
  if (!isEnd) {
    return firstOfMonth;
  }
  return new Date(firstOfMonth.getFullYear(), firstOfMonth.getMonth() + 1, 0);
};

const formatMonthLabel = (date: Date): string => {
  return date.toLocaleDateString('pt-BR', { month: 'short', year: 'numeric' });
};

const seriesKey = (codInvestimento: number): string => `inv_${codInvestimento}`;

const toNumber = (value?: number | null): number | null => {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return null;
  }
  return Number(value);
};

const amountOf = (pos: PosicaoDiaria, basis: AmountBasis): number | null => {
  return toNumber(basis === 'gross' ? pos.grossTotalAmount : pos.netTotalAmount);
};

const variationOf = (pos: PosicaoDiaria, basis: AmountBasis): number | null => {
  return toNumber(basis === 'gross' ? pos.grossVariation : pos.netVariation);
};

/** Multiplier 1.05 → 0.05 (5%). Same idea as variation relative to 1. */
const variationToRate = (variation: number | null): number | null => {
  if (variation === null) return null;
  return variation - 1;
};

const costBasisOf = (pos: PosicaoDiaria): number => {
  const qty = toNumber(pos.currentQty) || 0;
  const avg = toNumber(pos.currentAverageCost) || 0;
  return qty * avg;
};

interface ChartPoint {
  day: string;
  [key: string]: string | number | null;
}

const buildChartData = (
  investimentos: InvestimentoSerie[],
  basis: AmountBasis,
  metric: MetricKind
): ChartPoint[] => {
  const days = new Set<string>();
  const byInvDay = new Map<number, Map<string, PosicaoDiaria>>();

  investimentos.forEach((inv) => {
    const dayMap = new Map<string, PosicaoDiaria>();
    inv.posicoes.forEach((pos) => {
      if (!pos.day) return;
      days.add(pos.day);
      dayMap.set(pos.day, pos);
    });
    byInvDay.set(inv.codInvestimento, dayMap);
  });

  const sortedDays = Array.from(days).sort();
  const lastPos = new Map<number, PosicaoDiaria>();

  return sortedDays.map((day) => {
    const point: ChartPoint = { day };
    let totalAmount = 0;
    let totalCost = 0;
    let hasTotal = false;

    investimentos.forEach((inv) => {
      const row = byInvDay.get(inv.codInvestimento)?.get(day);
      if (row) {
        lastPos.set(inv.codInvestimento, row);
      }
      const pos = lastPos.get(inv.codInvestimento);
      if (!pos) return;

      const key = seriesKey(inv.codInvestimento);
      if (metric === 'value') {
        const value = amountOf(pos, basis);
        point[key] = value;
        if (value !== null) {
          totalAmount += value;
          hasTotal = true;
        }
      } else {
        point[key] = variationToRate(variationOf(pos, basis));
        const amount = amountOf(pos, basis);
        const cost = costBasisOf(pos);
        if (amount !== null && cost > 0) {
          totalAmount += amount;
          totalCost += cost;
          hasTotal = true;
        }
      }
    });

    if (metric === 'value') {
      point[TOTAL_KEY] = hasTotal ? totalAmount : null;
    } else {
      point[TOTAL_KEY] = totalCost > 0 ? totalAmount / totalCost - 1 : null;
    }

    return point;
  });
};

const latestStats = (
  investimentos: InvestimentoSerie[],
  basis: AmountBasis
): { totalValue: number | null; returnRate: number | null; prevValue: number | null } => {
  const points = buildChartData(investimentos, basis, 'value');
  const returnPoints = buildChartData(investimentos, basis, 'return');
  if (points.length === 0) {
    return { totalValue: null, returnRate: null, prevValue: null };
  }

  const last = points[points.length - 1];
  const lastValue = typeof last[TOTAL_KEY] === 'number' ? (last[TOTAL_KEY] as number) : null;
  const lastReturnPoint = returnPoints[returnPoints.length - 1];
  const returnRate =
    lastReturnPoint && typeof lastReturnPoint[TOTAL_KEY] === 'number'
      ? (lastReturnPoint[TOTAL_KEY] as number)
      : null;

  const lastDay = last.day;
  const target = new Date(`${lastDay}T00:00:00`);
  target.setDate(target.getDate() - 30);
  const targetIso = toISODate(target);
  const prev =
    points.filter((p) => p.day <= targetIso).pop() ||
    (points.length > 1 ? points[0] : undefined);
  const prevValue =
    prev && typeof prev[TOTAL_KEY] === 'number' ? (prev[TOTAL_KEY] as number) : null;

  return { totalValue: lastValue, returnRate, prevValue };
};

const DashboardPage: React.FC = () => {
  const [data, setData] = useState<DashboardPosicaoResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [basis, setBasis] = useState<AmountBasis>('net');
  const [metric, setMetric] = useState<MetricKind>('value');
  const [startOffset, setStartOffset] = useState(PERIOD_MONTHS_MAX - PERIOD_MONTHS_DEFAULT);
  const [endOffset, setEndOffset] = useState(PERIOD_MONTHS_MAX);
  const navigate = useNavigate();

  const inicio = toISODate(dateFromOffset(Math.min(startOffset, endOffset), false));
  const fim = toISODate(dateFromOffset(Math.max(startOffset, endOffset), true));

  useEffect(() => {
    let cancelled = false;
    const handle = window.setTimeout(async () => {
      setLoading(true);
      setError(null);
      try {
        const res = await dashboardApi.getPosicoes(getUserId(), inicio, fim);
        if (!cancelled) {
          setData(res.data);
        }
      } catch (err) {
        if (!cancelled) {
          setError('Não foi possível carregar os dados da carteira.');
          setData(null);
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }, 350);

    return () => {
      cancelled = true;
      window.clearTimeout(handle);
    };
  }, [inicio, fim]);

  const investimentos = data?.investimentos || [];

  const chartData = useMemo(
    () => buildChartData(investimentos, basis, metric),
    [investimentos, basis, metric]
  );

  const stats = useMemo(() => latestStats(investimentos, basis), [investimentos, basis]);

  const valueChange =
    stats.totalValue !== null && stats.prevValue && stats.prevValue !== 0
      ? (stats.totalValue - stats.prevValue) / stats.prevValue
      : null;

  const handleAddTransaction = () => {
    navigate('/transacao');
  };

  const handleStartChange = (value: number) => {
    setStartOffset(Math.min(value, endOffset));
  };

  const handleEndChange = (value: number) => {
    setEndOffset(Math.max(value, startOffset));
  };

  const startPct = (Math.min(startOffset, endOffset) / PERIOD_MONTHS_MAX) * 100;
  const endPct = (Math.max(startOffset, endOffset) / PERIOD_MONTHS_MAX) * 100;

  const formatAxis = (value: number) => {
    if (metric === 'return') {
      return percentFmt.format(value);
    }
    return compactCurrencyFmt.format(value/100);
  };

  const formatTooltip = (value: number) => {
    if (metric === 'return') {
      return percentFmt.format(value);
    }
    return currencyFmt.format(value/100);
  };

  const formatXAxis = (day: string) => {
    const date = new Date(`${day}T00:00:00`);
    return date.toLocaleDateString('pt-BR', { month: 'short', year: '2-digit' });
  };

  return (
    <AppLayout>
      <div className="dashboard-container">
        <main className="dashboard-main">
          <div className="dashboard-grid">
            <div className="chart-card">
              <div className="card-header">
                <div>
                  <h2 className="card-title">Desempenho da carteira</h2>
                  <p className="card-subtitle">
                    Investimentos ativos de {formatMonthLabel(dateFromOffset(Math.min(startOffset, endOffset), false))} até{' '}
                    {formatMonthLabel(dateFromOffset(Math.max(startOffset, endOffset), true))}
                  </p>
                </div>
              </div>

              <div className="chart-toolbar">
                <label className="chart-control">
                  <span>Base</span>
                  <select
                    className="form-select"
                    value={basis}
                    onChange={(e) => setBasis(e.target.value as AmountBasis)}
                  >
                    <option value="gross">Valores brutos</option>
                    <option value="net">Valores líquidos</option>
                  </select>
                </label>

                <label className="chart-control">
                  <span>Métrica</span>
                  <select
                    className="form-select"
                    value={metric}
                    onChange={(e) => setMetric(e.target.value as MetricKind)}
                  >
                    <option value="value">Valores</option>
                    <option value="return">Rentabilidade</option>
                  </select>
                </label>
              </div>

              <div className="period-control">
                <div className="period-control-header">
                  <span>Período</span>
                  <strong>
                    {formatMonthLabel(dateFromOffset(Math.min(startOffset, endOffset), false))} —{' '}
                    {formatMonthLabel(dateFromOffset(Math.max(startOffset, endOffset), true))}
                  </strong>
                </div>
                <div className="range-slider">
                  <div
                    className="range-slider-track"
                    style={{
                      background: `linear-gradient(to right, #e2e8f0 ${startPct}%, #667eea ${startPct}%, #764ba2 ${endPct}%, #e2e8f0 ${endPct}%)`,
                    }}
                  />
                  <input
                    type="range"
                    min={0}
                    max={PERIOD_MONTHS_MAX}
                    value={startOffset}
                    onChange={(e) => handleStartChange(Number(e.target.value))}
                    aria-label="Início do período"
                  />
                  <input
                    type="range"
                    min={0}
                    max={PERIOD_MONTHS_MAX}
                    value={endOffset}
                    onChange={(e) => handleEndChange(Number(e.target.value))}
                    aria-label="Fim do período"
                  />
                </div>
                <div className="period-scale">
                  <span>{formatMonthLabel(dateFromOffset(0, false))}</span>
                  <span>{formatMonthLabel(dateFromOffset(PERIOD_MONTHS_MAX, true))}</span>
                </div>
              </div>

              <div className="chart-container">
                {loading ? (
                  <div className="loading-spinner">
                    <div className="spinner"></div>
                    <p>Carregando dados da carteira...</p>
                  </div>
                ) : error ? (
                  <div className="empty-chart">
                    <p>{error}</p>
                  </div>
                ) : chartData.length === 0 ? (
                  <div className="empty-chart">
                    <p>Nenhum investimento ativo no período selecionado.</p>
                  </div>
                ) : (
                  <ResponsiveContainer width="100%" height={400}>
                    <LineChart data={chartData} margin={{ top: 8, right: 12, left: 8, bottom: 8 }}>
                      <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                      <XAxis
                        dataKey="day"
                        stroke="#4a5568"
                        fontSize={12}
                        minTickGap={32}
                        tickFormatter={formatXAxis}
                      />
                      <YAxis
                        stroke="#4a5568"
                        fontSize={12}
                        width={88}
                        tickFormatter={formatAxis}
                      />
                      <Tooltip
                        formatter={(value: number, name: string) => [formatTooltip(value), name]}
                        labelFormatter={(label) =>
                          new Date(`${label}T00:00:00`).toLocaleDateString('pt-BR')
                        }
                        contentStyle={{
                          backgroundColor: 'white',
                          border: '1px solid #e2e8f0',
                          borderRadius: '8px',
                          boxShadow: '0 4px 6px rgba(0, 0, 0, 0.1)',
                        }}
                      />
                      <Legend />
                      <Line
                        type="monotone"
                        dataKey={TOTAL_KEY}
                        name="Carteira"
                        stroke="#2d3748"
                        strokeWidth={3}
                        dot={false}
                        activeDot={{ r: 6 }}
                        connectNulls
                      />
                      {investimentos.map((inv, index) => (
                        <Line
                          key={inv.codInvestimento}
                          type="monotone"
                          dataKey={seriesKey(inv.codInvestimento)}
                          name={inv.descricao}
                          stroke={SERIES_COLORS[index % SERIES_COLORS.length]}
                          strokeWidth={2}
                          dot={false}
                          activeDot={{ r: 5 }}
                          connectNulls
                        />
                      ))}
                    </LineChart>
                  </ResponsiveContainer>
                )}
              </div>
            </div>

            <div className="stats-grid">
              <div className="stat-card">
                <div className="stat-icon">📈</div>
                <div className="stat-content">
                  <h3>{basis === 'gross' ? 'Valor bruto' : 'Valor líquido'}</h3>
                  <p className="stat-value">
                    {stats.totalValue !== null ? currencyFmt.format(stats.totalValue/100) : '—'}
                  </p>
                  <p
                    className={`stat-change ${
                      valueChange === null ? '' : valueChange >= 0 ? 'positive' : 'negative'
                    }`}
                  >
                    {valueChange === null
                      ? 'Sem comparação no período'
                      : `${valueChange >= 0 ? '+' : ''}${percentFmt.format(valueChange)} em 30 dias`}
                  </p>
                </div>
              </div>

              <div className="stat-card">
                <div className="stat-icon">💰</div>
                <div className="stat-content">
                  <h3>Investimentos</h3>
                  <p className="stat-value">{investimentos.length}</p>
                  <p className="stat-change">Ativos no período</p>
                </div>
              </div>

              <div className="stat-card">
                <div className="stat-icon">📊</div>
                <div className="stat-content">
                  <h3>{basis === 'gross' ? 'Rentabilidade bruta' : 'Rentabilidade líquida'}</h3>
                  <p className="stat-value">
                    {stats.returnRate !== null
                      ? `${stats.returnRate >= 0 ? '+' : ''}${percentFmt.format(stats.returnRate)}`
                      : '—'}
                  </p>
                  <p
                    className={`stat-change ${
                      stats.returnRate === null
                        ? ''
                        : stats.returnRate >= 0
                        ? 'positive'
                        : 'negative'
                    }`}
                  >
                    Sobre o custo médio da carteira
                  </p>
                </div>
              </div>
            </div>
          </div>
        </main>

        <button className="fab" onClick={handleAddTransaction} title="Add new transaction">
          +
        </button>
      </div>
    </AppLayout>
  );
};

export default DashboardPage;
