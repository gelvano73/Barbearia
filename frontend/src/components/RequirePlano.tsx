/**
 * Redireciona quando o plano atual não libera o recurso.
 */
import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { temRecurso, type PlanoRecurso } from '../data/planos'
import type { ReactNode } from 'react'

export default function RequirePlano({
  recurso,
  children,
  fallback = '/assinatura',
}: {
  recurso: PlanoRecurso
  children: ReactNode
  fallback?: string
}) {
  const { auth } = useAuth()
  if (!temRecurso(auth?.plano as string | undefined, recurso)) {
    return <Navigate to={fallback} replace />
  }
  return <>{children}</>
}
