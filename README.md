# InvestTrack

Aplicação para acompanhar uma **carteira de investimentos** focada em **ações da B3** e **renda fixa**. O produto combina uma API com histórico de fechamentos de mercado e um frontend React personalizável para visualizar posição, rentabilidade e movimentações.

## O que o produto faz

- Consolida compras e vendas da carteira (digitadas ou importadas do extrato B3).
- Cruza cada ativo com a série de **preços de fechamento** (ações via arquivos COTAHIST; renda fixa via tabela própria).
- Calcula, dia a dia, quantidade, custo médio, valor de mercado e variação — bruta ou líquida de taxas.
- Exibe o resultado em um dashboard com período, métrica e transações configuráveis.

Não é um home broker: o objetivo é **acompanhar o que você já possui**, não executar ordens.

## Arquitetura

O sistema é um monolito de dados com frontend desacoplado. Três camadas se comunicam por HTTP/JSON.

```text
┌─────────────────────────────────────────────────────────────┐
│  Frontend (React 18 + TypeScript)                           │
│  CRA · React Router · Axios · Recharts · xlsx               │
│  http://localhost:3000                                      │
└────────────────────────────┬────────────────────────────────┘
                             │ REST  /api/*
                             ▼
┌─────────────────────────────────────────────────────────────┐
│  API (Spring Boot 2.2 · Java 8 · JPA)                       │
│  Controllers → Services → Repositories                      │
│  http://localhost:6868                                      │
└──────────────┬──────────────────────────────┬───────────────┘
               │ JDBC                         │ CALL get_posicao_diaria
               ▼                              ▼
┌──────────────────────────┐    ┌─────────────────────────────┐
│  MySQL 5.7               │    │  Stored procedure           │
│  transacao               │    │  posição diária por ativo   │
│  tipo_investimento       │    └─────────────────────────────┘
│  indice                  │
│  apuracao_indice         │  ← fechamentos B3 (COTAHIST)
│  apuracao_indice_fixa    │  ← curva de renda fixa
└──────────────────────────┘
```

### Papel de cada parte

| Camada | Responsabilidade |
| --- | --- |
| **Frontend** | Interface, rotas, importação B3 no browser, filtros do gráfico e formatação em BRL. Não calcula preço de mercado. |
| **API** | CRUD de usuários, transações, índices e apurações. Importa lote de trades. Expõe a série do dashboard. |
| **Ingestão COTAHIST** | No startup, lê arquivos `data/COTAHIST_A*.TXT` e grava abertura/máximo/mínimo/fechamento em `apuracao_indice`. |
| **Procedure `get_posicao_diaria`** | Recalcula a posição de um ativo no intervalo: aplica transações em ordem, busca o fechamento do dia (ações ou fallback de renda fixa) e devolve valor bruto/líquido e variação. |

O frontend aponta para `http://localhost:6868/api` (`src/services/api.ts`). A API e o MySQL sobem juntos com Docker Compose na raiz do repositório.

## Domínio

```text
Usuario 1──* Transacao *──1 TipoInvestimento ──1 Indice
                                              │
                         ApuracaoIndice ──────┤  (ações / índices B3)
                         ApuracaoIndiceFixa ──┘  (renda fixa)
```

- **TipoInvestimento** — papel, CDB, Tesouro etc. Leva flags de IOF/IR e aponta para o índice de preço usado na marcação.
- **Indice** — ticker ou índice de correção (`tipo_indice` distingue a família do ativo).
- **ApuracaoIndice** — série diária de mercado (COTAHIST): abertura, máximos, mínimos e **fechamento**.
- **ApuracaoIndiceFixa** — série de fechamento para renda fixa, usada quando não há cotação B3.
- **Transacao** — compra (`buy`) ou venda (`sell`), com data, instituição, quantidade, valor unitário/total e taxas.

A procedure marca a carteira assim:

1. Processa transações até o fim do período (compras sobem quantidade e custo médio; vendas só baixam quantidade).
2. Para cada dia, lê `valor_fechamento` em `apuracao_indice`; se não houver, usa `apuracao_indice_fixa`.
3. `gross_total_amount = fechamento × quantidade`
4. `net_total_amount = bruto − taxas acumuladas`
5. Variação bruta/líquida em relação ao custo médio.

Transações anteriores ao início do gráfico entram no saldo de abertura, mas não geram pontos.

## Frontend: telas e personalização

SPA Create React App. Rotas em `src/App.tsx`, layout com menu em `AppLayout`.

| Rota | Tela | Função |
| --- | --- | --- |
| `/login` | Login | Sessão simplificada em `localStorage` (usuário demo). |
| `/dashboard` | Dashboard | Gráfico da carteira, período, base bruta/líquida, valor vs rentabilidade, seleção de transações. |
| `/transactions` | Lista | CRUD visual das transações. |
| `/transacao` e `/transacao/:id` | Formulário | Inclusão/edição manual (ações ou renda fixa). |
| `/upload` | Upload B3 | Lê `.xlsx` da B3 no browser (`xlsx` + `utils/b3Import.ts`) e envia o lote para `/transacoes/import`. |

### Como o dashboard é montado

1. `dashboardApi.getPosicoes(usuario, inicio, fim, idsExcluidos)` busca as séries.
2. `buildChartData` agrega as posições diárias no cliente (carteira + cada ativo).
3. O usuário escolhe **Base** (bruto/líquido), **Métrica** (valor/rentabilidade) e o intervalo (até 60 meses).
4. **Selecionar transações** abre um popup: desmarcar uma compra também desmarca vendas do mesmo ativo que deixariam saldo negativo (`utils/transactionSelection.ts`). Os IDs excluídos voltam na query do gráfico.

### Onde personalizar a UI

O visual foi pensado para ser ajustado sem reescrever regras de negócio.

| O quê | Onde |
| --- | --- |
| Paleta, botões, inputs | Variáveis `:root` e classes globais em `src/App.css` |
| Layout do menu | `src/components/AppLayout.css` |
| Dashboard, gráfico, modal | `DashboardPage.css`, `SelecionarTransacoesModal.css` |
| Cores das séries do gráfico | `SERIES_COLORS` em `DashboardPage.tsx` |
| Tipos e contratos da API | `src/types/index.ts` |
| Cliente HTTP | `src/services/api.ts` (`API_BASE_URL`) |
| Parser do extrato B3 | `src/utils/b3Import.ts` |

Especificidade CSS: classes de página (ex.: `.chart-control .btn.select-tx-btn`) precisam ser mais específicas que `.btn` / `.btn-secondary` de `App.css`, porque o global é injetado depois no bundle.

### Estrutura de pastas

```text
frontend/src/
├── App.tsx                 # rotas
├── App.css                 # tema global
├── components/
│   ├── AppLayout.tsx       # shell (menu + conteúdo)
│   └── SelecionarTransacoesModal.tsx
├── pages/
│   ├── LoginPage.tsx
│   ├── DashboardPage.tsx
│   ├── TransacoesListPage.tsx
│   ├── TransacaoFormPage.tsx
│   └── UploadPage.tsx
├── services/api.ts         # Axios → Spring
├── types/index.ts
└── utils/
    ├── b3Import.ts
    └── transactionSelection.ts
```

## API de fechamentos e carteira

Base: `http://localhost:6868/api`. CORS liberado para o frontend.

| Recurso | Uso |
| --- | --- |
| `GET /dashboard/posicoes` | Série do gráfico. Query: `idUsuario`, `inicio`, `fim`, `idsExcluidos` (IDs separados por vírgula). |
| `GET/POST/PUT/DELETE /transacoes` | Movimentações da carteira. |
| `POST /transacoes/import` | Lote vindo do upload B3 (ticker → tipo de investimento). |
| `GET /apuracoes-indice` | Fechamentos (e demais OHLC) por período ou por índice. |
| `GET /indices` | Cadastro de índices / tickers (`tipoIndice` filtra a família). |
| `GET /tipos-investimento` | Ativos da carteira (ações e renda fixa). |
| `GET/POST /usuarios` | Usuário da sessão. |

A ingestão COTAHIST (`IndiceFileIngestionJob`) roda na subida da API e preenche `apuracao_indice`. Renda fixa permanece em `apuracao_indice_fixa` para o fallback da procedure.

## Como rodar

### API e banco (raiz do repositório)

```bash
docker compose up
```

MySQL na porta `3307`, API na `6868` (ver `.env`).

### Frontend

```bash
cd frontend
npm install
npm start
```

Abre [http://localhost:3000](http://localhost:3000). Alternativa: `./start.sh`.

Requisitos: Node.js 16+.

### Scripts

| Comando | Efeito |
| --- | --- |
| `npm start` | Dev server com hot reload |
| `npm run build` | Build de produção |
| `npm test` | Test runner do CRA |

## Stack

- **UI:** React 18, TypeScript, React Router 6, Recharts, Axios, SheetJS (`xlsx`)
- **API:** Spring Boot, Spring Data JPA, stored procedure MySQL
- **Dados de mercado:** COTAHIST (B3) + apurações de renda fixa
- **Infra:** Docker Compose (MySQL 5.7 + app Java)
