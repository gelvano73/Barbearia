/**
 * Validação de e-mail real no frontend (espelha regras do backend).
 * Formato + bloqueio de descartáveis/exemplo. DNS é validado na API.
 */

/** === Formato e domínio === */
const FORMATO =
  /^[a-zA-Z0-9](?:[a-zA-Z0-9._%+-]{0,62}[a-zA-Z0-9])?@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)+$/

const DOMINIOS_BLOQUEADOS = new Set([
  'example.com',
  'example.org',
  'example.net',
  'test.com',
  'test.org',
  'localhost',
  'invalid',
  'email.com',
  'seuemail.com',
  'emailfalso.com',
  'ficticio.com',
  'mailinator.com',
  'guerrillamail.com',
  'guerrillamail.net',
  'sharklasers.com',
  'tempmail.com',
  'temp-mail.org',
  'temp-mail.io',
  'throwawaymail.com',
  'yopmail.com',
  'yopmail.fr',
  'trashmail.com',
  'discard.email',
  '10minutemail.com',
  '10minutemail.net',
  'minutemail.com',
  'fakeinbox.com',
  'getnada.com',
  'maildrop.cc',
  'mailnesia.com',
  'dispostable.com',
  'tempail.com',
  'emailondeck.com',
  'tmpmail.org',
  'tmpmail.net',
  'moakt.com',
  'tempmailo.com',
  'burnermail.io',
])

/** === Normalização === */
export function normalizarEmail(email: string) {
  return (email || '').trim().toLowerCase()
}

/** === Validação === */
export function emailRealOk(email: string) {
  const e = normalizarEmail(email)
  if (!e || e.length > 150 || e.includes('..') || e.startsWith('.') || e.includes('@.')) {
    return false
  }
  if (!FORMATO.test(e)) {
    return false
  }
  const dominio = e.split('@')[1]
  if (!dominio || !dominio.includes('.')) {
    return false
  }
  const tld = dominio.slice(dominio.lastIndexOf('.') + 1)
  if (tld.length < 2 || !/^[a-z]+$/i.test(tld)) {
    return false
  }
  if (DOMINIOS_BLOQUEADOS.has(dominio)) {
    return false
  }
  for (const bloqueado of DOMINIOS_BLOQUEADOS) {
    if (dominio.endsWith(`.${bloqueado}`)) {
      return false
    }
  }
  return true
}

/** === Mensagens === */
export const MSG_EMAIL_INVALIDO =
  'Informe um e-mail real e ativo (não use temporários, fake nem example.com).'
