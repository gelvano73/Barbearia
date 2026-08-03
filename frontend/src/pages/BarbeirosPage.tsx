// @ts-nocheck
/**
 * CRUD de barbeiros, criação de conta de acesso e definição de metas.
 * Inclui upload de foto da equipe.
 */
import { useEffect, useState } from 'react'
import Modal from '../components/Modal'
import FotoField from '../components/FotoField'
import { barbeirosApi } from '../services/resources'
import { emailRealOk, MSG_EMAIL_INVALIDO } from '../utils/email'

/** === Estado inicial === */
const empty = { nome: '', telefone: '', especialidade: '' }
const emptyConta = { email: '', senha: '' }
const emptyMeta = { ano: new Date().getFullYear(), mes: new Date().getMonth() + 1, metaAtendimentos: 40, metaComissao: 2000 }

export default function BarbeirosPage() {
  /** === Estado === */
  const [itens, setItens] = useState([])
  const [open, setOpen] = useState(false)
  const [contaOpen, setContaOpen] = useState(false)
  const [metaOpen, setMetaOpen] = useState(false)
  const [selected, setSelected] = useState(null)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState(empty)
  const [fotoUrl, setFotoUrl] = useState('')
  const [conta, setConta] = useState(emptyConta)
  const [meta, setMeta] = useState(emptyMeta)
  const [error, setError] = useState('')
  const [ok, setOk] = useState('')

  /** === Carga de dados === */
  const carregar = async () => {
    const { data } = await barbeirosApi.listar(true)
    setItens(data)
  }

  useEffect(() => {
    carregar().catch(() => setError('Não foi possível carregar barbeiros'))
  }, [])

  /** === CRUD de barbeiros === */
  const abrirNovo = () => {
    setEditing(null)
    setForm(empty)
    setFotoUrl('')
    setError('')
    setOpen(true)
  }

  const abrirEdicao = (item) => {
    setEditing(item)
    setForm({
      nome: item.nome || '',
      telefone: item.telefone || '',
      especialidade: item.especialidade || '',
    })
    setFotoUrl(item.fotoUrl || '')
    setError('')
    setOpen(true)
  }

  const salvar = async (e) => {
    e.preventDefault()
    setError('')
    try {
      if (editing) {
        await barbeirosApi.atualizar(editing.id, form)
      } else {
        await barbeirosApi.criar(form)
      }
      setOpen(false)
      await carregar()
    } catch (err) {
      setError(err.response?.data?.mensagem || 'Erro ao salvar barbeiro')
    }
  }

  const enviarFoto = async (file) => {
    if (!editing?.id) return
    setError('')
    try {
      const { data } = await barbeirosApi.uploadFoto(editing.id, file)
      setFotoUrl(data.fotoUrl || '')
      setEditing(data)
      await carregar()
    } catch (err) {
      setError(err.response?.data?.mensagem || 'Erro ao enviar foto')
    }
  }

  const desativar = async (id) => {
    if (!confirm('Desativar este barbeiro?')) return
    await barbeirosApi.desativar(id)
    await carregar()
  }

  /** === Conta e meta === */
  const criarConta = async (e) => {
    e.preventDefault()
    setError('')
    setOk('')
    if (!emailRealOk(conta.email)) {
      setError(MSG_EMAIL_INVALIDO)
      return
    }
    try {
      await barbeirosApi.criarConta(selected.id, conta)
      setContaOpen(false)
      setOk('Conta criada. Login em /barbeiro/login')
      await carregar()
    } catch (err) {
      setError(err.response?.data?.mensagem || 'Erro ao criar conta')
    }
  }

  const salvarMeta = async (e) => {
    e.preventDefault()
    setError('')
    setOk('')
    try {
      await barbeirosApi.definirMeta(selected.id, {
        ...meta,
        ano: Number(meta.ano),
        mes: Number(meta.mes),
        metaAtendimentos: Number(meta.metaAtendimentos),
        metaComissao: Number(meta.metaComissao),
      })
      setMetaOpen(false)
      setOk('Meta definida')
    } catch (err) {
      setError(err.response?.data?.mensagem || 'Erro ao definir meta')
    }
  }

  return (
    <>
      {/* === Cabeçalho === */}
      <div className="page-header">
        <div>
          <h1>Barbeiros</h1>
          <p>Equipe, contas de acesso e metas</p>
        </div>
        <button className="btn" type="button" onClick={abrirNovo}>
          Novo barbeiro
        </button>
      </div>

      {ok && <div className="panel" style={{ marginBottom: '1rem', color: 'var(--ok)' }}>{ok}</div>}

      {/* === Tabela da equipe === */}
      <div className="panel">
        {itens.length === 0 ? (
          <div className="empty">Nenhum barbeiro cadastrado.</div>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Foto</th>
                <th>Nome</th>
                <th>Telefone</th>
                <th>Especialidade</th>
                <th>Conta</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {itens.map((item) => (
                <tr key={item.id}>
                  <td>
                    <div className="foto-avatar sm">
                      {item.fotoUrl ? <img src={item.fotoUrl} alt={item.nome} /> : <span>—</span>}
                    </div>
                  </td>
                  <td>{item.nome}</td>
                  <td>{item.telefone || '—'}</td>
                  <td>{item.especialidade || '—'}</td>
                  <td>{item.usuarioId ? 'Sim' : 'Não'}</td>
                  <td>
                    <div className="actions-row">
                      <button className="btn secondary small" type="button" onClick={() => abrirEdicao(item)}>
                        Editar
                      </button>
                      {!item.usuarioId && (
                        <button
                          className="btn secondary small"
                          type="button"
                          onClick={() => {
                            setSelected(item)
                            setConta(emptyConta)
                            setError('')
                            setContaOpen(true)
                          }}
                        >
                          Criar conta
                        </button>
                      )}
                      <button
                        className="btn secondary small"
                        type="button"
                        onClick={() => {
                          setSelected(item)
                          setMeta(emptyMeta)
                          setError('')
                          setMetaOpen(true)
                        }}
                      >
                        Meta
                      </button>
                      <button className="btn danger small" type="button" onClick={() => desativar(item.id)}>
                        Desativar
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* === Modal cadastro === */}
      <Modal open={open} title={editing ? 'Editar barbeiro' : 'Novo barbeiro'} onClose={() => setOpen(false)}>
        <form onSubmit={salvar}>
          <div className="form-grid">
            <div className="full">
              <FotoField
                fotoUrl={fotoUrl}
                onUpload={enviarFoto}
                disabled={!editing}
              />
            </div>
            <label className="full">
              Nome
              <input value={form.nome} onChange={(e) => setForm({ ...form, nome: e.target.value })} required />
            </label>
            <label>
              Telefone
              <input value={form.telefone} onChange={(e) => setForm({ ...form, telefone: e.target.value })} />
            </label>
            <label>
              Especialidade
              <input value={form.especialidade} onChange={(e) => setForm({ ...form, especialidade: e.target.value })} />
            </label>
          </div>
          {error && <div className="error" style={{ marginTop: '0.8rem' }}>{error}</div>}
          <div className="modal-actions">
            <button className="btn secondary" type="button" onClick={() => setOpen(false)}>Cancelar</button>
            <button className="btn" type="submit">Salvar</button>
          </div>
        </form>
      </Modal>

      {/* === Modal conta === */}
      <Modal open={contaOpen} title={`Conta — ${selected?.nome || ''}`} onClose={() => setContaOpen(false)}>
        <form onSubmit={criarConta}>
          <div className="form-grid">
            <label className="full">
              Email
              <input type="email" value={conta.email} onChange={(e) => setConta({ ...conta, email: e.target.value })} required />
            </label>
            <label className="full">
              Senha
              <input type="password" value={conta.senha} onChange={(e) => setConta({ ...conta, senha: e.target.value })} required minLength={6} />
            </label>
          </div>
          {error && <div className="error" style={{ marginTop: '0.8rem' }}>{error}</div>}
          <div className="modal-actions">
            <button className="btn secondary" type="button" onClick={() => setContaOpen(false)}>Cancelar</button>
            <button className="btn" type="submit">Criar conta</button>
          </div>
        </form>
      </Modal>

      {/* === Modal meta === */}
      <Modal open={metaOpen} title={`Meta — ${selected?.nome || ''}`} onClose={() => setMetaOpen(false)}>
        <form onSubmit={salvarMeta}>
          <div className="form-grid">
            <label>
              Ano
              <input type="number" value={meta.ano} onChange={(e) => setMeta({ ...meta, ano: e.target.value })} required />
            </label>
            <label>
              Mês
              <input type="number" min={1} max={12} value={meta.mes} onChange={(e) => setMeta({ ...meta, mes: e.target.value })} required />
            </label>
            <label>
              Meta atendimentos
              <input type="number" min={0} value={meta.metaAtendimentos} onChange={(e) => setMeta({ ...meta, metaAtendimentos: e.target.value })} required />
            </label>
            <label>
              Meta comissão (R$)
              <input type="number" min={0} step="0.01" value={meta.metaComissao} onChange={(e) => setMeta({ ...meta, metaComissao: e.target.value })} required />
            </label>
          </div>
          {error && <div className="error" style={{ marginTop: '0.8rem' }}>{error}</div>}
          <div className="modal-actions">
            <button className="btn secondary" type="button" onClick={() => setMetaOpen(false)}>Cancelar</button>
            <button className="btn" type="submit">Salvar meta</button>
          </div>
        </form>
      </Modal>
    </>
  )
}
