export interface Usuario {
  idUsuario: number;
  nome: string;
  token: string;
}

export interface UsuarioCreateDTO {
  nome: string;
  token: string;
}

export interface Transacao {
  idTransacao?: number;
  dataTransacao: string;
  dataVencimento?: string;
  instituicao: string;
  // Expected values: "buy" or "sell"
  tipoTransacao?: string;
  valor: number;
  quantidade: number;
  codInvestimento?: number;
  idUsuario?: number;
  descricaoInvestimento?: string;
}

export interface TransacaoCreateDTO {
  dataTransacao: string;
  dataVencimento?: string;
  instituicao: string;
  // Expected values: "buy" or "sell"
  tipoTransacao: string;
  valor: number;
  quantidade: number;
  codInvestimento: number;
  idUsuario: number;
}

export interface TransacaoImportItem {
  dataTransacao: string;
  instituicao: string;
  tipoTransacao: string;
  valor: number;
  valorUnitario: number;
  quantidade: number;
  ticker: string;
  produto: string;
}

export interface TransacaoImportRequest {
  idUsuario: number;
  transacoes: TransacaoImportItem[];
}

export interface ApuracaoIndice {
  dataApuracao: string;
  valorFechamento: number;
}

export interface TipoInvestimento {
  codInvestimento: number;
  descricao: string;
  incideIof: string;
  incideIr: string;
}

export interface Indice {
  codIndice: number;
  descricao: string;
  tipoIndice: string;
}

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

export interface InvestimentoSerie {
  codInvestimento: number;
  descricao: string;
  transacoes: TransacaoSerie[];
  cotacoes: CotacaoDia[];
}

export type PanelBase = 'LIQUIDO' | 'BRUTO';
export type PanelMetrica = 'VALOR' | 'RENTABILIDADE';
export type PanelExibicaoTransacoes = 'AGRUPADA' | 'INDIVIDUAIS';

export interface Panel {
  idPanel: number;
  idUsuario: number;
  descricao: string;
  base: PanelBase;
  metrica: PanelMetrica;
  exibicaoTransacoes?: PanelExibicaoTransacoes;
  filtroTransacoes: boolean;
  quantidadeTransacoes: number;
  idsTransacao: number[];
}

export interface PanelCreate {
  idUsuario: number;
  descricao: string;
}

export interface PanelUpdate {
  descricao: string;
  base: PanelBase;
  metrica: PanelMetrica;
  exibicaoTransacoes: PanelExibicaoTransacoes;
  filtroTransacoes: boolean;
  idsTransacao: number[];
}

export interface PanelPosicaoResponse {
  inicio: string;
  fim: string;
  investimentos: InvestimentoSerie[];
}

export interface Dashboard {
  idDashboard: number;
  idUsuario: number;
  descricao: string;
  idPanelEsquerdaSuperior: number | null;
  idPanelDireitaSuperior: number | null;
  idPanelEsquerdaInferior: number | null;
  idPanelDireitaInferior: number | null;
}

export interface DashboardWrite {
  idUsuario: number;
  descricao: string;
  idPanelEsquerdaSuperior: number | null;
  idPanelDireitaSuperior: number | null;
  idPanelEsquerdaInferior: number | null;
  idPanelDireitaInferior: number | null;
}
