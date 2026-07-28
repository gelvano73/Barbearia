/**
 * Declarações de tipos do Vite para variáveis de ambiente do frontend.
 * Expõe VITE_API_URL tipada em import.meta.env.
 */
/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
