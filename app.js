(function () {
  'use strict';

  const CHAVE = 'lembrete-remedios-v1';
  const SONECA_MIN = 10;
  const LEN_ANEL = 2 * Math.PI * 34;
  const ROTULOS = {
    pendente: 'Pendente',
    tomado: 'Tomado',
    pulado: 'Pulado',
    adiado: 'Adiado',
    atrasado: 'Atrasado'
  };
  const ICONES = {
    pendente: '⏳',
    tomado: '✅',
    pulado: '⏭️',
    adiado: '🔁',
    atrasado: '⚠️'
  };
  const CORES_CARTAO = [
    'linear-gradient(135deg,#6d5efc,#8b7bff)',
    'linear-gradient(135deg,#ff5d8f,#ff8a65)',
    'linear-gradient(135deg,#10b981,#34d399)',
    'linear-gradient(135deg,#f59e0b,#fbbf24)',
    'linear-gradient(135deg,#0ea5e9,#38bdf8)',
    'linear-gradient(135deg,#a855f7,#d946ef)'
  ];

  // ---------- Utilidades ----------
  const $ = (id) => document.getElementById(id);
  const pad = (n) => String(n).padStart(2, '0');
  const diaDe = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
  const horaDe = (d) => `${pad(d.getHours())}:${pad(d.getMinutes())}`;
  const chaveDose = (remedioId, hora) => `${remedioId}|${hora}`;
  const formatarDia = (dia) => dia.split('-').reverse().slice(0, 2).join('/');
  const corDe = (id) => {
    let soma = 0;
    for (const c of id) soma += c.charCodeAt(0);
    return CORES_CARTAO[soma % CORES_CARTAO.length];
  };

  function el(tag, attrs, filhos) {
    const e = document.createElement(tag);
    for (const k in attrs || {}) {
      if (k === 'texto') e.textContent = attrs[k];
      else if (k === 'on') {
        for (const ev in attrs.on) e.addEventListener(ev, attrs.on[ev]);
      } else e.setAttribute(k, attrs[k]);
    }
    (filhos || []).forEach((f) =>
      e.appendChild(typeof f === 'string' ? document.createTextNode(f) : f)
    );
    return e;
  }

  function toast(msg) {
    const t = $('toast');
    t.textContent = msg;
    t.classList.add('visivel');
    clearTimeout(toast.timer);
    toast.timer = setTimeout(() => t.classList.remove('visivel'), 2200);
  }

  // ---------- Armazenamento ----------
  const vazio = () => ({ remedios: [], registros: {}, avisados: {}, adiados: [] });

  function carregar() {
    try {
      const bruto = localStorage.getItem(CHAVE);
      if (bruto) return Object.assign(vazio(), JSON.parse(bruto));
    } catch (e) {
      /* armazenamento indisponível ou dado corrompido: começa vazio */
    }
    return vazio();
  }

  let estado = carregar();

  function salvar() {
    try {
      localStorage.setItem(CHAVE, JSON.stringify(estado));
      return true;
    } catch (e) {
      alert('Não foi possível salvar os dados neste aparelho.');
      return false;
    }
  }

  function registrarDose(dia, chave, status, info) {
    const anterior = estado.registros[dia] && estado.registros[dia][chave];
    estado.registros[dia] = estado.registros[dia] || {};
    estado.registros[dia][chave] = {
      status,
      em: new Date().toISOString(),
      nome: info.nome,
      hora: info.hora
    };
    // Dá baixa no estoque só uma vez por dose
    if (status === 'tomado' && !(anterior && anterior.status === 'tomado')) {
      baixarEstoque(info.remedioId);
    }
    salvar();
    renderizar();
  }

  // ---------- Estoque ----------
  const LIMITE_ALERTA_DIAS = 5;

  function diasRestantes(r) {
    const doses = Math.max(1, r.horarios.length);
    return Math.floor(r.estoque.quantidade / doses);
  }

  function baixarEstoque(remedioId) {
    const r = estado.remedios.find((x) => x.id === remedioId);
    if (!r || !r.estoque || r.estoque.quantidade <= 0) return;
    r.estoque.quantidade -= 1;
    if (r.estoque.quantidade === 0) {
      toast('Acabou o estoque de ' + r.nome + ' ⚠️');
    } else if (diasRestantes(r) <= LIMITE_ALERTA_DIAS) {
      toast('Atenção: ' + r.nome + ' está acabando (' + r.estoque.quantidade + ' restantes)');
    }
  }

  function pedirQuantidade(r, modo) {
    const msg = modo === 'somar'
      ? 'Quantos comprimidos você acabou de comprar?'
      : 'Quantos comprimidos restam agora na caixa?';
    const v = parseInt(prompt(msg, ''), 10);
    if (!Number.isFinite(v) || v < 0) return;
    if (!r.estoque) {
      const por = parseInt(prompt('Quantos comprimidos vêm em cada cartela?', '10'), 10);
      r.estoque = { quantidade: 0, porCartela: Number.isFinite(por) && por > 0 ? por : 10 };
    }
    r.estoque.quantidade = modo === 'somar' ? r.estoque.quantidade + v : v;
    salvar();
    renderizar();
    toast('Estoque de ' + r.nome + ': ' + r.estoque.quantidade + ' comprimidos');
  }

  // Desenha a cartela: as bolinhas ainda cheias são os comprimidos que restam
  function desenharCartela(e) {
    const por = e.porCartela;
    const q = e.quantidade;
    const emUso = q === 0 ? 0 : (q % por || por);
    const fechadas = Math.floor((q - emUso) / por);

    const grade = el('div', { class: 'cartela', style: 'grid-template-columns: repeat(' + Math.min(por, 7) + ', 1fr)' });
    for (let i = 0; i < por; i++) {
      grade.appendChild(el('span', { class: 'comp' + (i < emUso ? ' cheio' : ' vazio') }));
    }

    const blocos = el('div', { class: 'cartelas' });
    for (let i = 0; i < Math.min(fechadas, 3); i++) {
      blocos.appendChild(el('span', { class: 'cartela-fechada' }));
    }
    if (fechadas > 0) {
      blocos.appendChild(el('span', { class: 'fechadas-txt', texto: fechadas + (fechadas === 1 ? ' cartela fechada' : ' cartelas fechadas') }));
    }
    return el('div', { class: 'area-cartela' }, [grade, blocos]);
  }

  function desenharEstoque(r) {
    if (!r.estoque) {
      return el('div', { class: 'estoque vazio-estoque' }, [
        el('span', { texto: 'Controle de estoque desligado' }),
        el('button', { class: 'btn-sec pequeno', type: 'button', texto: 'Informar quantidade', on: { click: () => pedirQuantidade(r, 'definir') } })
      ]);
    }
    const q = r.estoque.quantidade;
    const dias = diasRestantes(r);
    const alerta = q === 0 || dias <= LIMITE_ALERTA_DIAS;

    return el('div', { class: 'estoque' + (alerta ? ' alerta' : '') }, [
      el('div', { class: 'estoque-topo' }, [
        el('div', {}, [
          el('strong', { class: 'qtd-num', texto: String(q) }),
          el('span', { class: 'qtd-txt', texto: q === 1 ? ' comprimido restante' : ' comprimidos restantes' })
        ]),
        el('span', {
          class: 'dias-txt',
          texto: q === 0 ? 'Acabou' : 'Dá para ~' + dias + (dias === 1 ? ' dia' : ' dias')
        })
      ]),
      desenharCartela(r.estoque),
      el('div', { class: 'estoque-acoes' }, [
        el('button', { class: 'btn-sec pequeno', type: 'button', texto: '+ Comprei', on: { click: () => pedirQuantidade(r, 'somar') } }),
        el('button', { class: 'btn-sec pequeno', type: 'button', texto: 'Corrigir contagem', on: { click: () => pedirQuantidade(r, 'definir') } })
      ])
    ]);
  }

  function statusDose(dia, chave, hora) {
    const reg = estado.registros[dia] && estado.registros[dia][chave];
    if (reg) return reg.status;
    if (estado.adiados.some((a) => a.chave === chave && a.dia === dia)) return 'adiado';
    if (hora < horaDe(new Date())) return 'atrasado';
    return 'pendente';
  }

  // ---------- Verificação de horários ----------
  let filaAlarme = [];
  let alarmeAtual = null;
  let ultimaHora = '';

  function verificarHorarios(agora) {
    const dia = diaDe(agora);
    const hora = horaDe(agora);
    const agoraMs = agora.getTime();
    let mudou = false;

    // Sonecas que venceram
    const vencidas = estado.adiados.filter((a) => a.quando <= agoraMs);
    if (vencidas.length) {
      estado.adiados = estado.adiados.filter((a) => a.quando > agoraMs);
      vencidas
        .filter((a) => !(estado.registros[a.dia] && estado.registros[a.dia][a.chave]))
        .forEach((a) => filaAlarme.push({ ...a, soneca: true }));
      mudou = true;
    }

    // Horários de hoje
    Object.keys(estado.avisados).forEach((d) => {
      if (d !== dia) delete estado.avisados[d];
    });
    estado.avisados[dia] = estado.avisados[dia] || [];
    const avisados = estado.avisados[dia];

    estado.remedios.forEach((r) => {
      r.horarios.forEach((h) => {
        const chave = chaveDose(r.id, h);
        if (h !== hora || avisados.includes(chave)) return;
        avisados.push(chave);
        mudou = true;
        const jaFeito = estado.registros[dia] && estado.registros[dia][chave];
        if (!jaFeito) {
          filaAlarme.push({ chave, remedioId: r.id, nome: r.nome, dose: r.dose, dia, hora: h, soneca: false });
        }
      });
    });

    if (mudou) salvar();
    if (!alarmeAtual && filaAlarme.length) proximoAlarme();
    return mudou;
  }

  function tick() {
    const agora = new Date();
    const hora = horaDe(agora);
    const mudouHora = hora !== ultimaHora;
    ultimaHora = hora;
    const mudou = verificarHorarios(agora);
    if (mudou || mudouHora) renderizar();
  }

  // ---------- Alarme ----------
  let audioCtx = null;
  let somTimer = null;

  function garantirAudio() {
    try {
      if (!audioCtx) {
        const Ctx = window.AudioContext || window.webkitAudioContext;
        if (Ctx) audioCtx = new Ctx();
      }
      if (audioCtx && audioCtx.state === 'suspended') audioCtx.resume();
    } catch (e) {
      audioCtx = null;
    }
  }

  function bipe() {
    if (!audioCtx || audioCtx.state !== 'running') return;
    const osc = audioCtx.createOscillator();
    const ganho = audioCtx.createGain();
    osc.type = 'sine';
    osc.frequency.value = 880;
    ganho.gain.setValueAtTime(0.0001, audioCtx.currentTime);
    ganho.gain.exponentialRampToValueAtTime(0.3, audioCtx.currentTime + 0.02);
    ganho.gain.exponentialRampToValueAtTime(0.0001, audioCtx.currentTime + 0.4);
    osc.connect(ganho);
    ganho.connect(audioCtx.destination);
    osc.start();
    osc.stop(audioCtx.currentTime + 0.4);
  }

  function iniciarSom() {
    pararSom();
    garantirAudio();
    const tocar = () => {
      bipe();
      if (navigator.vibrate) navigator.vibrate([300, 150, 300]);
    };
    tocar();
    somTimer = setInterval(tocar, 1200);
  }

  function pararSom() {
    if (somTimer) {
      clearInterval(somTimer);
      somTimer = null;
    }
    if (navigator.vibrate) navigator.vibrate(0);
  }

  function avisarSistema(a) {
    if (!('Notification' in window) || Notification.permission !== 'granted') return;
    const titulo = 'Hora do remédio: ' + a.nome;
    const opcoes = {
      body: (a.dose ? a.dose + ' · ' : '') + a.hora,
      tag: a.chave + '|' + a.dia,
      icon: 'icon.svg'
    };
    const reserva = () => {
      try { new Notification(titulo, opcoes); } catch (e) { /* sem suporte */ }
    };
    if (navigator.serviceWorker && navigator.serviceWorker.ready) {
      navigator.serviceWorker.ready
        .then((reg) => reg.showNotification(titulo, opcoes))
        .catch(reserva);
    } else {
      reserva();
    }
  }

  function proximoAlarme() {
    alarmeAtual = filaAlarme.shift() || null;
    if (!alarmeAtual) {
      pararSom();
      $('alarme').hidden = true;
      return;
    }
    const a = alarmeAtual;
    $('alarme-nome').textContent = a.nome;
    $('alarme-dose').textContent = a.dose || '';
    $('alarme-hora').textContent = a.soneca
      ? 'Lembrete adiado · horário original ' + a.hora
      : 'Agendado para ' + a.hora;
    $('alarme').hidden = false;
    iniciarSom();
    avisarSistema(a);
  }

  $('alarme-tomei').addEventListener('click', () => {
    const a = alarmeAtual;
    if (a) {
      registrarDose(a.dia, a.chave, 'tomado', a);
      toast('Dose registrada. Muito bem! 💪');
    }
    proximoAlarme();
  });

  $('alarme-pular').addEventListener('click', () => {
    const a = alarmeAtual;
    if (a) registrarDose(a.dia, a.chave, 'pulado', a);
    proximoAlarme();
  });

  $('alarme-soneca').addEventListener('click', () => {
    const a = alarmeAtual;
    if (a) {
      estado.adiados.push({
        chave: a.chave,
        remedioId: a.remedioId,
        nome: a.nome,
        dose: a.dose,
        dia: a.dia,
        hora: a.hora,
        quando: Date.now() + SONECA_MIN * 60000
      });
      salvar();
      toast('Vou te lembrar em ' + SONECA_MIN + ' minutos ⏰');
    }
    proximoAlarme();
    renderizar();
  });

  // ---------- Ativar alertas ----------
  function atualizarBotaoAtivar() {
    const ativo = 'Notification' in window && Notification.permission === 'granted';
    $('ativar').textContent = ativo ? '🔔 Alertas ativos' : '🔔 Ativar alertas';
  }

  $('ativar').addEventListener('click', async () => {
    garantirAudio();
    bipe();
    if ('Notification' in window && Notification.permission === 'default') {
      try { await Notification.requestPermission(); } catch (e) { /* ignorado */ }
    }
    atualizarBotaoAtivar();
    toast('Alertas prontos para tocar 🔔');
  });

  // ---------- Folha de cadastro ----------
  let horariosNovos = [];

  function abrirSheet() {
    $('sheet').classList.add('aberto');
    setTimeout(() => $('nome').focus(), 250);
  }

  function fecharSheet() {
    $('sheet').classList.remove('aberto');
  }

  $('abrir-form').addEventListener('click', abrirSheet);
  $('fechar-sheet').addEventListener('click', fecharSheet);
  $('sheet').addEventListener('click', (ev) => {
    if (ev.target.id === 'sheet') fecharSheet();
  });
  document.addEventListener('keydown', (ev) => {
    if (ev.key === 'Escape') fecharSheet();
  });

  function adicionarHorario(h) {
    if (!h) return;
    if (!horariosNovos.includes(h)) horariosNovos.push(h);
    horariosNovos.sort();
    renderHorariosNovos();
  }

  function renderHorariosNovos() {
    const lista = $('horarios-novos');
    lista.textContent = '';
    horariosNovos.forEach((h) => {
      lista.appendChild(
        el('li', {}, [
          h,
          el('button', {
            type: 'button',
            title: 'Remover horário',
            'aria-label': 'Remover ' + h,
            texto: '×',
            on: {
              click: () => {
                horariosNovos = horariosNovos.filter((x) => x !== h);
                renderHorariosNovos();
              }
            }
          })
        ])
      );
    });
  }

  document.querySelectorAll('[data-hora]').forEach((b) =>
    b.addEventListener('click', () => adicionarHorario(b.dataset.hora))
  );

  $('btn-horario').addEventListener('click', () => {
    const h = $('hora-nova').value;
    if (!h) return toast('Escolha um horário primeiro');
    adicionarHorario(h);
    $('hora-nova').value = '';
  });

  $('form-remedio').addEventListener('submit', (ev) => {
    ev.preventDefault();
    const nome = $('nome').value.trim();
    const dose = $('dose').value.trim();
    if (!nome) return toast('Digite o nome do remédio');
    if (!horariosNovos.length) return toast('Adicione pelo menos um horário');

    const qtd = parseInt($('qtd').value, 10);
    const por = parseInt($('por-cartela').value, 10);

    estado.remedios.push({
      id: Date.now().toString(36) + Math.random().toString(36).slice(2, 6),
      nome,
      dose,
      horarios: horariosNovos.slice(),
      estoque: Number.isFinite(qtd) && qtd >= 0
        ? { quantidade: qtd, porCartela: Number.isFinite(por) && por > 0 ? por : 10 }
        : null
    });
    if (!salvar()) return;

    horariosNovos = [];
    ev.target.reset();
    renderHorariosNovos();
    fecharSheet();
    renderizar();
    toast('Remédio salvo! 💊');
  });

  function removerRemedio(id) {
    if (!confirm('Remover este remédio? O histórico de doses será mantido.')) return;
    estado.remedios = estado.remedios.filter((r) => r.id !== id);
    estado.adiados = estado.adiados.filter((a) => !a.chave.startsWith(id + '|'));
    salvar();
    renderizar();
  }

  // ---------- Renderização ----------
  function renderSaudacao() {
    const h = new Date().getHours();
    $('saudacao').textContent = h < 12 ? 'Bom dia ☀️' : h < 18 ? 'Boa tarde 🌤️' : 'Boa noite 🌙';
    const data = new Date().toLocaleDateString('pt-BR', {
      weekday: 'long',
      day: 'numeric',
      month: 'long'
    });
    $('data').textContent = data.charAt(0).toUpperCase() + data.slice(1);
  }

  function renderResumo(total, tomadas) {
    const pct = total ? Math.round((tomadas / total) * 100) : 0;
    $('anel').style.strokeDashoffset = String(LEN_ANEL * (1 - pct / 100));
    $('anel-num').textContent = pct + '%';
    if (!total) {
      $('resumo-titulo').textContent = 'Nenhuma dose hoje';
      $('resumo-sub').textContent = 'Cadastre um remédio para começar';
    } else if (tomadas === total) {
      $('resumo-titulo').textContent = 'Tudo em dia! 🎉';
      $('resumo-sub').textContent = tomadas + ' de ' + total + ' doses tomadas';
    } else {
      $('resumo-titulo').textContent = tomadas + ' de ' + total + ' doses tomadas';
      $('resumo-sub').textContent = 'Faltam ' + (total - tomadas) + ' hoje';
    }
  }

  function renderHoje() {
    const lista = $('lista-hoje');
    lista.textContent = '';
    const dia = diaDe(new Date());

    const itens = [];
    estado.remedios.forEach((r) =>
      r.horarios.forEach((h) => itens.push({ r, h, chave: chaveDose(r.id, h) }))
    );
    itens.sort((a, b) => a.h.localeCompare(b.h));

    const tomadas = itens.filter((i) => statusDose(dia, i.chave, i.h) === 'tomado').length;
    renderResumo(itens.length, tomadas);

    if (!itens.length) {
      lista.appendChild(
        el('div', { class: 'vazio-card' }, [
          el('div', { class: 'emoji', texto: '🕒' }),
          el('strong', { texto: 'Sua agenda está vazia' }),
          el('span', { texto: 'Toque no + para cadastrar o primeiro remédio.' })
        ])
      );
      return;
    }

    itens.forEach(({ r, h, chave }) => {
      const st = statusDose(dia, chave, h);
      const feito = st === 'tomado' || st === 'pulado';
      const info = { nome: r.nome, hora: h, remedioId: r.id };

      const acao = feito
        ? el('span', { class: 'selo', texto: ICONES[st] + ' ' + ROTULOS[st] })
        : el('div', { class: 'dose-acoes' }, [
            el('button', {
              class: 'btn-ok',
              type: 'button',
              texto: '✓ Tomei',
              on: { click: () => registrarDose(dia, chave, 'tomado', info) }
            }),
            el('button', {
              class: 'btn-ghost',
              type: 'button',
              texto: 'Pular',
              on: { click: () => registrarDose(dia, chave, 'pulado', info) }
            })
          ]);

      lista.appendChild(
        el('article', { class: 'dose status-' + st }, [
          el('div', { class: 'hora-grande', texto: h }),
          el('div', { class: 'dose-info' }, [
            el('strong', { texto: r.nome }),
            el('span', { texto: (r.dose ? r.dose + ' · ' : '') + ROTULOS[st] })
          ]),
          acao
        ])
      );
    });
  }

  function renderRemedios() {
    const grade = $('lista-remedios');
    grade.textContent = '';

    if (!estado.remedios.length) {
      grade.appendChild(
        el('div', { class: 'vazio-card' }, [
          el('div', { class: 'emoji', texto: '💊' }),
          el('strong', { texto: 'Nenhum remédio cadastrado' }),
          el('span', { texto: 'Cadastre seus remédios e nunca mais esqueça um horário.' }),
          el('button', { type: 'button', texto: 'Cadastrar agora', on: { click: abrirSheet } })
        ])
      );
      return;
    }

    estado.remedios.forEach((r) => {
      grade.appendChild(
        el('article', { class: 'card-remedio' }, [
          el('div', { class: 'card-cabeca', style: 'background:' + corDe(r.id) }, [
            el('div', { class: 'card-topo' }, [
              el('span', { class: 'pilula', texto: '💊' }),
              el('button', {
                class: 'lixo',
                type: 'button',
                title: 'Remover remédio',
                'aria-label': 'Remover ' + r.nome,
                texto: '🗑',
                on: { click: () => removerRemedio(r.id) }
              })
            ]),
            el('h3', { texto: r.nome }),
            el('p', { class: 'dose-txt', texto: r.dose || 'Dose não informada' }),
            el('div', { class: 'horas' }, r.horarios.map((h) => el('span', { texto: h })))
          ]),
          desenharEstoque(r)
        ])
      );
    });
  }

  function renderHistorico() {
    const lista = $('historico');
    lista.textContent = '';
    let total = 0;

    for (let i = 0; i < 7; i++) {
      const d = new Date();
      d.setDate(d.getDate() - i);
      const dia = diaDe(d);
      const regs = Object.values(estado.registros[dia] || {});
      regs.sort((a, b) => (a.hora || '').localeCompare(b.hora || ''));

      regs.forEach((r) => {
        total++;
        lista.appendChild(
          el('li', { class: 'status-' + r.status }, [
            el('span', { texto: formatarDia(dia) + ' · ' + (r.hora || '') + ' · ' + (r.nome || 'Remédio removido') }),
            el('span', { texto: ICONES[r.status] + ' ' + ROTULOS[r.status] + ' às ' + horaDe(new Date(r.em)) })
          ])
        );
      });
    }

    if (!total) {
      lista.appendChild(el('li', { class: 'vazio', texto: 'Nenhum registro nos últimos 7 dias.' }));
    }
  }

  function renderizar() {
    renderSaudacao();
    renderHoje();
    renderRemedios();
    renderHistorico();
  }

  // ---------- Início ----------
  if ('serviceWorker' in navigator) {
    window.addEventListener('load', () => {
      navigator.serviceWorker.register('sw.js').catch(() => {});
    });
  }

  atualizarBotaoAtivar();
  renderHorariosNovos();
  renderizar();
  tick();
  setInterval(tick, 10000);
  document.addEventListener('visibilitychange', () => {
    if (!document.hidden) tick();
  });
})();
