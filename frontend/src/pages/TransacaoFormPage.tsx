import React, { useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { tipoInvestimentoApi, transacaoApi } from '../services/api';
import AppLayout from '../components/AppLayout';
import { TipoInvestimento, TransacaoCreateDTO } from '../types';
import './TransacaoFormPage.css';

const ACAO = 'acao';

const TIPOS_RENDA_FIXA = [
  'LCA CDI (Ex: 100% CDI)',
  'LCI CDI (Ex: 100% CDI)',
  'CDB CDI (Ex: 100% CDI)',
];

const parseReais = (texto: string): number | null => {
  let limpo = texto.replace(/R\$/gi, '').replace(/\s/g, '');
  if (!limpo) return null;
  if (limpo.endsWith(',')) limpo = limpo.slice(0, -1);
  if (!limpo) return null;
  if (limpo.includes(',')) {
    limpo = limpo.replace(/\./g, '').replace(',', '.');
  } else if (/^\d{1,3}(\.\d{3})+$/.test(limpo)) {
    limpo = limpo.replace(/\./g, '');
  }
  if (!/^\d+(\.\d{1,2})?$/.test(limpo)) return null;
  const valor = Number(limpo);
  return Number.isFinite(valor) ? valor : null;
};

const formatReais = (valor: number): string => {
  const [inteiro, centavos] = valor.toFixed(2).split('.');
  if (centavos === '00') return `R$ ${inteiro}`;
  return `R$ ${inteiro},${centavos}`;
};

const reaisParaCentavos = (reais: number): number => Math.round(reais * 100);

const dataHoje = (): string => {
  const agora = new Date();
  const mes = String(agora.getMonth() + 1).padStart(2, '0');
  const dia = String(agora.getDate()).padStart(2, '0');
  return `${agora.getFullYear()}-${mes}-${dia}`;
};

const toDateInput = (value?: string): string => {
  if (!value) return '';
  return value.slice(0, 10);
};

const toApiDateTime = (value?: string): string | undefined => {
  if (!value) return undefined;
  return value.length === 10 ? `${value}T00:00:00` : value;
};

const TransacaoFormPage: React.FC = () => {
  const { id } = useParams();
  const isEdit = Boolean(id);

  const [formData, setFormData] = useState<TransacaoCreateDTO>({
    dataTransacao: dataHoje(),
    dataVencimento: '',
    instituicao: '',
    valor: 0,
    quantidade: 0,
    codInvestimento: 0,
    tipoTransacao: 'buy',
    idUsuario: 1
  });

  const [categoria, setCategoria] = useState('');
  const [acaoQuery, setAcaoQuery] = useState('');
  const [acaoSugestoes, setAcaoSugestoes] = useState<TipoInvestimento[]>([]);
  const [acaoAberta, setAcaoAberta] = useState(false);
  const [acaoDestaque, setAcaoDestaque] = useState(0);
  const [buscandoAcao, setBuscandoAcao] = useState(false);
  const [acaoSelecionada, setAcaoSelecionada] = useState<TipoInvestimento | null>(null);
  const [valorTexto, setValorTexto] = useState('');
  const [erroTipo, setErroTipo] = useState('');
  const [vinculandoTipo, setVinculandoTipo] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const acaoRef = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();

  const percentualCdi = categoria !== '' && categoria !== ACAO;

  useEffect(() => {
    const loadTransaction = async () => {
      if (!isEdit || !id) return;

      setLoading(true);
      setError('');

      try {
        const res = await transacaoApi.getById(Number(id));
        const t = res.data;
        const descricao = t.descricaoInvestimento || '';
        const reais = Number(t.valor) / 100;

        setValorTexto(Number.isFinite(reais) && reais !== 0 ? formatReais(reais) : '');
        setFormData({
          dataTransacao: toDateInput(t.dataTransacao),
          dataVencimento: toDateInput(t.dataVencimento),
          instituicao: t.instituicao,
          valor: Number.isFinite(reais) ? reais : 0,
          quantidade: t.quantidade,
          codInvestimento: t.codInvestimento ?? 0,
          tipoTransacao: t.tipoTransacao ?? 'buy',
          idUsuario: t.idUsuario ?? 1,
        });

        if (TIPOS_RENDA_FIXA.indexOf(descricao) >= 0) {
          setCategoria(descricao);
        } else if (t.codInvestimento) {
          const acao: TipoInvestimento = {
            codInvestimento: t.codInvestimento,
            descricao: descricao || 'Ação',
            incideIof: 'N',
            incideIr: 'N',
          };
          setCategoria(ACAO);
          setAcaoQuery(acao.descricao);
          setAcaoSelecionada(acao);
        }
      } catch (e) {
        setError('Failed to load transaction for editing.');
      } finally {
        setLoading(false);
      }
    };

    loadTransaction();
  }, [isEdit, id]);

  useEffect(() => {
    if (categoria !== ACAO) {
      return;
    }
    const termo = acaoQuery.trim();
    if (acaoSelecionada && acaoSelecionada.descricao === acaoQuery) {
      setAcaoSugestoes([]);
      setAcaoAberta(false);
      return;
    }
    if (termo.length < 1) {
      setAcaoSugestoes([]);
      setAcaoAberta(false);
      return;
    }

    let cancelado = false;
    const timer = window.setTimeout(async () => {
      setBuscandoAcao(true);
      try {
        const res = await tipoInvestimentoApi.buscarAcoes(termo);
        if (cancelado) return;
        const lista = res.data || [];
        setAcaoSugestoes(lista);
        setAcaoDestaque(0);
        setAcaoAberta(true);
      } catch (e) {
        if (!cancelado) {
          setAcaoSugestoes([]);
          setAcaoAberta(true);
        }
      } finally {
        if (!cancelado) setBuscandoAcao(false);
      }
    }, 250);

    return () => {
      cancelado = true;
      window.clearTimeout(timer);
    };
  }, [acaoQuery, acaoSelecionada, categoria]);

  useEffect(() => {
    const fechar = (event: MouseEvent) => {
      if (acaoRef.current && !acaoRef.current.contains(event.target as Node)) {
        setAcaoAberta(false);
      }
    };
    document.addEventListener('mousedown', fechar);
    return () => document.removeEventListener('mousedown', fechar);
  }, []);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: name === 'quantidade'
        ? Number(value)
        : value
    }));
  };

  const handleCategoriaChange = async (value: string) => {
    setCategoria(value);
    setErroTipo('');
    setAcaoQuery('');
    setAcaoSugestoes([]);
    setAcaoAberta(false);
    setAcaoSelecionada(null);

    if (value === '' || value === ACAO) {
      setFormData(prev => ({ ...prev, codInvestimento: 0 }));
      return;
    }

    setVinculandoTipo(true);
    try {
      const res = await tipoInvestimentoApi.getByDescricao(value);
      const tipo = (res.data || [])[0];
      if (!tipo) {
        setErroTipo('Tipo de investimento não encontrado.');
        setFormData(prev => ({ ...prev, codInvestimento: 0 }));
        return;
      }
      setFormData(prev => ({ ...prev, codInvestimento: tipo.codInvestimento }));
    } catch (e) {
      setErroTipo('Não foi possível vincular este tipo de investimento.');
      setFormData(prev => ({ ...prev, codInvestimento: 0 }));
    } finally {
      setVinculandoTipo(false);
    }
  };

  const selecionarAcao = (acao: TipoInvestimento) => {
    setAcaoSelecionada(acao);
    setAcaoQuery(acao.descricao);
    setAcaoSugestoes([]);
    setAcaoAberta(false);
    setFormData(prev => ({ ...prev, codInvestimento: acao.codInvestimento }));
  };

  const handleAcaoQueryChange = (value: string) => {
    setAcaoQuery(value);
    if (!acaoSelecionada || acaoSelecionada.descricao !== value) {
      setAcaoSelecionada(null);
      setFormData(prev => ({ ...prev, codInvestimento: 0 }));
    }
  };

  const handleAcaoKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (!acaoAberta || acaoSugestoes.length === 0) {
      return;
    }
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setAcaoDestaque(atual => (atual + 1) % acaoSugestoes.length);
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setAcaoDestaque(atual => (atual - 1 + acaoSugestoes.length) % acaoSugestoes.length);
    } else if (e.key === 'Enter') {
      e.preventDefault();
      selecionarAcao(acaoSugestoes[acaoDestaque]);
    } else if (e.key === 'Escape') {
      setAcaoAberta(false);
    }
  };

  const handleValorChange = (texto: string) => {
    const semMoeda = texto.replace(/R\$/gi, '').replace(/\s/g, '');
    if (semMoeda !== '' && !/^\d*,?\d{0,2}$/.test(semMoeda)) {
      return;
    }
    setValorTexto(semMoeda);
    const reais = parseReais(semMoeda);
    setFormData(prev => ({ ...prev, valor: reais ?? 0 }));
  };

  const handleValorBlur = () => {
    const reais = parseReais(valorTexto);
    if (reais === null) {
      setValorTexto('');
      setFormData(prev => ({ ...prev, valor: 0 }));
      return;
    }
    setValorTexto(formatReais(reais));
    setFormData(prev => ({ ...prev, valor: reais }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      if (
        !formData.instituicao ||
        !formData.valor ||
        !formData.quantidade ||
        !categoria ||
        !formData.codInvestimento ||
        !formData.tipoTransacao
      ) {
        setError(categoria === ACAO && !formData.codInvestimento
          ? 'Selecione uma ação na lista.'
          : 'Please fill in all required fields');
        setLoading(false);
        return;
      }

      const payload = {
        ...formData,
        dataTransacao: toApiDateTime(formData.dataTransacao),
        dataVencimento: toApiDateTime(formData.dataVencimento),
        valor: reaisParaCentavos(formData.valor),
      };

      if (isEdit && id) {
        await transacaoApi.update(Number(id), payload as any);
      } else {
        await transacaoApi.create(payload as any);
      }

      navigate('/transactions');
    } catch (submitError) {
      setError(isEdit ? 'Failed to update transaction. Please try again.' : 'Failed to create transaction. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    navigate('/transactions');
  };

  return (
    <AppLayout>
      <div className="transacao-form-container">
        <div className="transacao-form-card">
          <div className="form-header">
            <h1>{isEdit ? 'Editar Transação' : 'Adicionar Nova Transação'}</h1>
            <p>Registre sua transação</p>
          </div>

          <form onSubmit={handleSubmit} className="transacao-form">
            <div className="form-row">
              <div className="form-group">
                <label htmlFor="categoriaInvestimento" className="form-label">
                  Tipo de Investimento *
                </label>
                <select
                  id="categoriaInvestimento"
                  value={categoria}
                  onChange={(e) => handleCategoriaChange(e.target.value)}
                  className="form-select"
                  required
                >
                  <option value="">Selecione o tipo de investimento</option>
                  {TIPOS_RENDA_FIXA.map(tipo => (
                    <option key={tipo} value={tipo}>
                      {tipo}
                    </option>
                  ))}
                  <option value={ACAO}>Ação</option>
                </select>
                {erroTipo && <p className="field-hint field-hint-error">{erroTipo}</p>}
              </div>
            </div>

            {categoria === ACAO && (
              <div className="form-row">
                <div className="form-group form-group-span acao-search" ref={acaoRef}>
                  <label htmlFor="acao" className="form-label">
                    Ação *
                  </label>
                  <input
                    type="text"
                    id="acao"
                    value={acaoQuery}
                    onChange={(e) => handleAcaoQueryChange(e.target.value)}
                    onFocus={() => {
                      if (acaoSugestoes.length > 0) setAcaoAberta(true);
                    }}
                    onKeyDown={handleAcaoKeyDown}
                    className="form-input"
                    placeholder="Digite o código ou o nome da ação"
                    autoComplete="off"
                    role="combobox"
                    aria-expanded={acaoAberta}
                    aria-controls="acao-sugestoes"
                    aria-autocomplete="list"
                    required
                  />
                  {acaoAberta && (
                    <ul id="acao-sugestoes" className="acao-suggestions" role="listbox">
                      {buscandoAcao && acaoSugestoes.length === 0 && (
                        <li className="acao-suggestion acao-suggestion-muted">Buscando...</li>
                      )}
                      {!buscandoAcao && acaoSugestoes.length === 0 && (
                        <li className="acao-suggestion acao-suggestion-muted">Nenhuma ação encontrada</li>
                      )}
                      {acaoSugestoes.map((acao, index) => (
                        <li
                          key={acao.codInvestimento}
                          role="option"
                          aria-selected={index === acaoDestaque}
                          className={`acao-suggestion${index === acaoDestaque ? ' active' : ''}`}
                          onMouseDown={(event) => {
                            event.preventDefault();
                            selecionarAcao(acao);
                          }}
                          onMouseEnter={() => setAcaoDestaque(index)}
                        >
                          {acao.descricao}
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              </div>
            )}

            <div className="form-row">
              <div className="form-group">
                <label htmlFor="dataTransacao" className="form-label">
                  Data da Transação *
                </label>
                <input
                  type="date"
                  id="dataTransacao"
                  name="dataTransacao"
                  value={formData.dataTransacao}
                  onChange={handleInputChange}
                  className="form-input"
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="dataVencimento" className="form-label">
                  Data de Vencimento
                </label>
                <input
                  type="date"
                  id="dataVencimento"
                  name="dataVencimento"
                  value={formData.dataVencimento}
                  onChange={handleInputChange}
                  className="form-input"
                />
              </div>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label htmlFor="instituicao" className="form-label">
                  Instituição *
                </label>
                <input
                  type="text"
                  id="instituicao"
                  name="instituicao"
                  value={formData.instituicao}
                  onChange={handleInputChange}
                  className="form-input"
                  placeholder="e.g., Banco do Brasil, Nubank"
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="tipoTransacao" className="form-label">
                  Tipo Transação *
                </label>
                <select
                  id="tipoTransacao"
                  name="tipoTransacao"
                  value={formData.tipoTransacao}
                  onChange={handleInputChange}
                  className="form-select"
                  required
                >
                  <option value="buy">Compra</option>
                  <option value="sell">Venda</option>
                </select>
              </div>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label htmlFor="valor" className="form-label">
                  Valor (R$) *
                </label>
                <input
                  type="text"
                  id="valor"
                  name="valor"
                  inputMode="decimal"
                  value={valorTexto}
                  onChange={(e) => handleValorChange(e.target.value)}
                  onBlur={handleValorBlur}
                  className="form-input"
                  placeholder="R$"
                  autoComplete="off"
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="quantidade" className="form-label">
                  {percentualCdi ? '% do CDI *' : 'Quantidade *'}
                </label>
                <input
                  type="number"
                  id="quantidade"
                  name="quantidade"
                  value={formData.quantidade}
                  onChange={handleInputChange}
                  className="form-input"
                  placeholder={percentualCdi ? '100' : '0'}
                  step="0.01"
                  min="0"
                  required
                />
              </div>
            </div>

            {error && (
              <div className="error-message">
                {error}
              </div>
            )}

            <div className="form-actions">
              <button
                type="button"
                onClick={handleCancel}
                className="btn btn-secondary"
              >
                Cancelar
              </button>
              <button
                type="submit"
                className="btn btn-primary"
                disabled={loading || vinculandoTipo}
              >
                {loading
                  ? isEdit
                    ? 'Editando...'
                    : 'Criando...'
                  : isEdit
                    ? 'Editar Transação'
                    : 'Criar Transação'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </AppLayout>
  );
};

export default TransacaoFormPage;
