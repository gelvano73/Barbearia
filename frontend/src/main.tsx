/**
 * Ponto de entrada da aplicação React.
 * Monta o app no #root com StrictMode, roteador e provedor de autenticação.
 */
import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import App from './App'
import { AuthProvider } from './context/AuthContext'
import './styles/global.css'

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
