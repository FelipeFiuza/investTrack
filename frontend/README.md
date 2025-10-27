# InvestTrack Frontend

A React TypeScript frontend for the InvestTrack investment portfolio tracking application.

## Features

- **Login Page**: Simple authentication with email and password
- **Dashboard**: Interactive line chart showing investment performance over time
- **Transaction Form**: Add new investment transactions
- **Responsive Design**: Works on desktop and mobile devices
- **Modern UI**: Clean, professional interface with gradient color scheme

## Technology Stack

- React 18 with TypeScript
- React Router for navigation
- Recharts for data visualization
- Axios for API communication
- CSS3 with custom properties and gradients

## Getting Started

### Prerequisites

- Node.js (version 16 or higher)
- npm or yarn package manager

### Installation

1. Navigate to the frontend directory:
   ```bash
   cd frontend
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. Start the development server:
   ```bash
   npm start
   ```

4. Open [http://localhost:3000](http://localhost:3000) to view it in the browser.

## Project Structure

```
src/
├── components/          # Reusable UI components
├── pages/             # Page components
│   ├── LoginPage.tsx
│   ├── DashboardPage.tsx
│   └── TransacaoFormPage.tsx
├── services/          # API service layer
│   └── api.ts
├── types/            # TypeScript type definitions
│   └── index.ts
├── App.tsx           # Main app component
├── App.css           # Global styles
├── index.tsx         # App entry point
└── index.css         # Base styles
```

## API Integration

The frontend is configured to communicate with the Spring Boot backend running on `http://localhost:8080`. The API endpoints include:

- `/api/usuarios` - User management
- `/api/transacoes` - Transaction management (to be implemented)
- `/api/apuracoes-indice` - Index calculations
- `/api/tipos-investimento` - Investment types
- `/api/indices` - Market indices

## Color Palette

The application uses a modern gradient color scheme:
- Primary: #667eea (Blue)
- Secondary: #764ba2 (Purple)
- Accent: #f093fb (Pink)
- Text: #2d3748 (Dark Gray)
- Background: #f7fafc (Light Gray)

## Available Scripts

- `npm start` - Runs the app in development mode
- `npm build` - Builds the app for production
- `npm test` - Launches the test runner
- `npm eject` - Ejects from Create React App (one-way operation)

## Development Notes

- The app uses mock data for the dashboard chart as specified
- Authentication is simplified for demo purposes
- The transaction form includes all fields from the Transacao entity
- Responsive design ensures compatibility with mobile devices
- All forms include proper validation and error handling
