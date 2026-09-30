import React, { useEffect, useRef, useState } from 'react';
import { createRoot } from 'react-dom/client';
import './style.css';

const statuses = { contact_lawyer: 'Contato com advogado', sent_to_funder: 'Enviado ao financiador', cannot_fund: 'Não financiável' };
const profiles = { 'agent-a': 'Agente U-A · T-A', 'manager-a': 'Manager · T-A', 'manager-b': 'Manager · T-B', unknown: 'Papel sem acesso', anonymous: 'Sem autenticação' };
const errors = { 401: 'Sessão ausente. Escolha um perfil para continuar.', 403: 'Este perfil não tem permissão para a operação.', 404: 'Lead não encontrado ou sem permissão de acesso.', 422: 'Não foi possível fazer esta transição. Confira os dados e o estado atual.', 503: 'Serviço indisponível. Seus dados foram preservados.' };

function App() {
  const [session, setSession] = useState(null);
  const [leads, setLeads] = useState([]);
  const [id, setId] = useState('101');
  const [version, setVersion] = useState(1);
  const [target, setTarget] = useState('sent_to_funder');
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState(null);
  const [conflict, setConflict] = useState(null);
  const [failure, setFailure] = useState(false);
  const [history, setHistory] = useState([]);
  const lock = useRef(false);
  const confirmed = leads.find(lead => String(lead.id) === id);
  const terminal = confirmed && confirmed.status !== 'contact_lawyer';

  async function api(path, body, extraHeaders = {}) {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 8000);
    try {
      const response = await fetch(path, {
        method: body === undefined ? 'GET' : 'POST',
        credentials: 'same-origin', signal: controller.signal,
        headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': session?.csrf || '', ...extraHeaders },
        ...(body === undefined ? {} : { body: JSON.stringify(body) }),
      });
      return { status: response.status, data: await response.json() };
    } finally { clearTimeout(timeout); }
  }

  useEffect(() => {
    let active = true;
    fetch('/api/session').then(response => response.json()).then(async data => {
      const response = await fetch('/api/leads');
      const rows = await response.json();
      if (active) { setSession(data); setLeads(response.ok ? rows : []); }
    }).catch(() => { if (active) setNotice({ type: 'error', text: 'Não foi possível conectar ao Java. Confira se o servidor está rodando.' }); });
    return () => { active = false; };
  }, []);

  function chooseLead(lead) {
    if (lock.current) return;
    setId(String(lead.id)); setVersion(lead.version); setConflict(null); setNotice(null);
  }

  async function action(callback) {
    if (lock.current) return;
    lock.current = true; setBusy(true);
    try { await callback(); }
    catch { setNotice({ type: 'error', text: 'Não foi possível confirmar a atualização. Seus dados foram preservados; tente novamente com a mesma versão.' }); }
    finally { lock.current = false; setBusy(false); }
  }

  async function submit(event) {
    event.preventDefault();
    await action(async () => {
      setNotice(null); setConflict(null);
      const payload = { id: Number(id), status: target, version };
      const result = await api('/api/leads/update', payload, failure ? { 'X-Demo-Failure': 'before-save' } : {});
      setHistory(old => [{ status: result.status, id, data: result.data }, ...old].slice(0, 5));
      if (result.status === 200) {
        const data = result.data;
        if (data.id !== payload.id || !statuses[data.status] || !Number.isInteger(data.version)) throw new Error('Invalid response');
        setLeads(old => old.map(lead => lead.id === data.id ? { ...lead, ...data } : lead));
        setVersion(data.version);
        setNotice({ type: 'success', text: `Lead ${id} atualizado. Versão confirmada: ${data.version}.` });
      } else if (result.status === 409) {
        setNotice({ type: 'warning', text: 'Este lead mudou desde que você abriu o formulário. Sua escolha foi preservada; revise os dados atuais antes de continuar.' });
        const current = await api('/api/leads');
        if (current.status !== 200) throw new Error('Cannot refresh');
        const record = current.data.find(lead => String(lead.id) === id);
        setConflict(record || { missing: true });
      } else {
        setNotice({ type: 'error', text: errors[result.status] || 'Não foi possível salvar. Seus dados foram preservados.' });
      }
    });
  }

  async function changeProfile(profile) {
    await action(async () => {
      const result = await api('/api/demo/session', { profile });
      if (result.status !== 200) throw new Error('Profile failed');
      setSession(result.data);
      const list = await api('/api/leads');
      setLeads(list.status === 200 ? list.data : []);
      setConflict(null);
      setNotice({ type: 'info', text: 'Perfil de teste alterado. Os campos do formulário foram preservados.' });
    });
  }

  return <>
    <header className="topbar"><div className="brand"><span className="monogram">CP</span><span>CasePayNow <small>Avaliação Full Stack</small></span></div><span className="demo-badge"><span /> Demo local</span></header>
    <main>
      <div className="intro"><div><p className="eyebrow">SEÇÃO C · JAVA + REACT</p><h1>Atualização de leads</h1><p>Edite um registro e acompanhe a resposta do servidor.</p></div><button className="secondary" disabled={busy || !session} onClick={() => action(async () => {
        const reset = await api('/api/demo/reset', {}); if (reset.status !== 200) throw new Error('Reset failed');
        const list = await api('/api/leads'); setLeads(list.status === 200 ? list.data : []);
        setId('101'); setVersion(1); setTarget('sent_to_funder'); setConflict(null); setHistory([]); setFailure(false);
        setNotice({ type: 'info', text: 'Fixtures restauradas para a versão 1.' });
      })}>Restaurar dados</button></div>
      <div className="local-note">Dados fictícios, mantidos em memória. A interface chama a API Java local; não há integração com sistemas reais.</div>
      <div className="workspace">
        <aside className="panel">
          <div className="panel-heading"><h2>Contexto de teste</h2><span className="step">01</span></div>
          <label htmlFor="profile">Perfil fictício</label>
          <select id="profile" value={session?.profile || 'agent-a'} disabled={busy || !session} onChange={e => changeProfile(e.target.value)}>{Object.entries(profiles).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select>
          <p className="hint">Escolha um perfil predefinido para conferir as permissões. Este controle não é um login real.</p>
          <div className="list-heading"><h3>Leads acessíveis</h3><span>{leads.length}</span></div>
          <div className="lead-list">{leads.map(lead => <button key={lead.id} className={`lead-card ${id === String(lead.id) ? 'selected' : ''}`} disabled={busy} onClick={() => chooseLead(lead)}>
            <span className="lead-top"><strong>Lead #{lead.id}</strong><span>v{lead.version}</span></span>
            <span>{lead.tenantId} · Responsável {lead.assignedTo}</span><span className={`status ${lead.status}`}>{statuses[lead.status]}</span>
          </button>)}</div>
          {leads.length === 0 && <p className="empty">Nenhum registro disponível para este perfil.</p>}
          <div className="fixture-note">Fixtures: 101 → U-A / T-A; 102 → U-B / T-A; 201 → U-X / T-B.</div>
        </aside>
        <section className="panel editor">
          <div className="panel-heading"><h2>Editar status</h2><span className="step">02</span></div>
          <form onSubmit={submit}>
            <fieldset disabled={busy || !session}>
              <div className="form-row"><div><label htmlFor="lead-id">ID do lead</label><input id="lead-id" type="number" min="1" step="1" required value={id} onChange={e => { setId(e.target.value); const lead = leads.find(row => String(row.id) === e.target.value); setVersion(lead?.version || 1); setConflict(null); setNotice(null); }} /></div><div><label htmlFor="version">Versão carregada</label><input id="version" value={version} readOnly /></div></div>
              <p className="hint">Um ID de outro responsável ou tenant deve ser negado pelo Java.</p>
              <div className="current"><span>Estado confirmado</span><strong>{confirmed ? statuses[confirmed.status] : 'Não carregado para este perfil'}</strong></div>
              <label htmlFor="target">Novo status</label><select id="target" value={target} onChange={e => setTarget(e.target.value)} disabled={busy || terminal}><option value="sent_to_funder">Enviado ao financiador</option><option value="cannot_fund">Não financiável</option></select>
              {terminal && <p className="hint terminal-note">Este estado é terminal. Restaure os dados para testar outra transição.</p>}
              <button className="primary save" type="submit" disabled={busy || terminal || !!conflict}>{busy ? 'Salvando…' : 'Salvar alteração'}<span aria-hidden="true">→</span></button>
            </fieldset>
          </form>
          {notice && <div role="status" aria-live="polite" className={`notice ${notice.type}`}>{notice.text}</div>}
          {conflict && <div className="conflict"><strong>Revisar conflito</strong><p>{conflict.missing ? 'O registro não está mais acessível.' : `Servidor: ${statuses[conflict.status]} · versão ${conflict.version}. Sua escolha: ${statuses[target]}.`}</p>{!conflict.missing && <button className="secondary" disabled={busy} onClick={() => { setLeads(old => old.map(lead => lead.id === conflict.id ? conflict : lead)); setVersion(conflict.version); setConflict(null); setNotice({ type: 'info', text: 'Dados atuais carregados após sua revisão. Nenhuma alteração foi reenviada.' }); }}>Usar dados atuais após revisão</button>}</div>}
          <details className="scenarios"><summary>Conferir erro e conflito</summary><label className="checkbox"><input type="checkbox" checked={failure} disabled={busy} onChange={e => setFailure(e.target.checked)} /> Simular erro 503 antes de salvar</label><button className="secondary" disabled={busy || !session || !confirmed || terminal} onClick={() => action(async () => {
            const result = await api('/api/demo/conflict', { id: Number(id) });
            if (result.status !== 200) throw new Error('Conflict setup failed');
            setNotice({ type: 'info', text: 'A versão mudou no Java. Salve usando a versão ainda carregada para conferir o conflito 409.' });
          })}>Simular outra edição no servidor</button><p className="hint">Esses controles alteram apenas as fixtures desta sessão local.</p></details>
        </section>
      </div>
      <section className="panel activity"><div className="panel-heading"><h2>Respostas da API</h2><span className="muted">Últimas 5 tentativas</span></div>{history.length === 0 ? <p className="empty">As respostas do Java aparecerão aqui após salvar.</p> : <ol>{history.map((item, index) => <li key={index}><span className={`http-code ${item.status === 200 ? 'good' : ''}`}>{item.status}</span><strong>Lead #{item.id}</strong><code>{JSON.stringify(item.data)}</code></li>)}</ol>}</section>
      <footer><span>Matheus Souza Garcez</span><span>Java executa as regras · React apresenta o resultado</span></footer>
    </main>
  </>;
}

createRoot(document.getElementById('root')).render(<App />);
