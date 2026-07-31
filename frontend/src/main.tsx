/**
 * Ponto de entrada da aplicação React.
 * Monta o app no #root com StrictMode, roteador e provedor de autenticação.
 */
import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import * as Sentry from '@sentry/react'
import App from './App'
import { AuthProvider } from './context/AuthContext'
import './styles/global.css'

const sentryDsn = import.meta.env.VITE_SENTRY_DSN as string | undefined
if (sentryDsn) {
  Sentry.init({
    dsn: sentryDsn,
    environment: (import.meta.env.VITE_SENTRY_ENVIRONMENT as string) || import.meta.env.MODE,
    tracesSampleRate: 0.1,
  })
}

const root = document.getElementById('root')
if (!root) {
  throw new Error('Elemento #root não encontrado')
}

ReactDOM.createRoot(root).render(
  <React.StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <App />
      </AuthProvider>
    </BrowserRouter>
  </React.StrictMode>
)
