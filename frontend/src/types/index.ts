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

export interface PosicaoDiaria {
  day: string;
  currentQty?: number | null;
  currentAverageCost?: number | null;
  boughtQty?: number | null;
  boughtPrice?: number | null;
  boughtTaxes?: number | null;
  soldQty?: number | null;
  soldPrice?: number | null;
  soldTaxes?: number | null;
  valorFechamento?: number | null;
  grossTotalAmount?: number | null;
  netTotalAmount?: number | null;
  grossVariation?: number | null;
  netVariation?: number | null;
}

export interface InvestimentoSerie {
  codInvestimento: number;
  descricao: string;
  posicoes: PosicaoDiaria[];
}

export interface DashboardPosicaoResponse {
  inicio: string;
  fim: string;
  investimentos: InvestimentoSerie[];
}
