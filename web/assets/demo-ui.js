/* =========================================================================
   Digest · demo · interfaz (parte 1)
   Componentes comunes, pantalla de alta, Hoy, Medicación y Comidas.
   ========================================================================= */

/* ======================= Componentes comunes =========================== */

DG.pantalla = (html) => {
  const cont = document.getElementById("appScreen");
  cont.innerHTML = html;
  cont.scrollTop = 0;
  // Sin perfil todavía no hay barra inferior: la app abre la alta a pantalla completa.
  document.getElementById("movil").classList.toggle("sin-nav", !DG.estado.perfil);
};

DG.cabecera = (titulo, sub) => `
  <header class="app-cab">
    <h2>${titulo}</h2>
    ${sub ? `<span>${sub}</span>` : ""}
  </header>`;

DG.vacio = (titulo, detalle) => `
  <div class="app-vacio">
    <strong>${titulo}</strong>
    ${detalle ? `<span>${detalle}</span>` : ""}
  </div>`;

DG.chipsHtml = (html) => `<div class="app-chips">${html}</div>`;

DG.snackTimer = null;
DG.snack = (html) => {
  const el = document.getElementById("movilSnack");
  el.innerHTML = html;
  el.classList.add("visible");
  clearTimeout(DG.snackTimer);
  DG.snackTimer = setTimeout(() => el.classList.remove("visible"), 4200);
};

DG.abrirHoja = (html, alMontar) => {
  const capa = document.getElementById("movilCapa");
  const hoja = document.getElementById("movilHoja");
  hoja.innerHTML = html;
  capa.hidden = false;
  hoja.scrollTop = 0;
  const cerrar = hoja.querySelector("[data-cerrar]");
  if (cerrar) cerrar.addEventListener("click", DG.cerrarHoja);
  capa.onclick = (ev) => { if (ev.target === capa) DG.cerrarHoja(); };
  if (alMontar) alMontar(hoja);
};

DG.cerrarHoja = () => {
  document.getElementById("movilCapa").hidden = true;
  document.getElementById("movilHoja").innerHTML = "";
};

DG.tituloHoja = (titulo) => `
  <div class="app-fila" style="margin-bottom:14px">
    <h3 style="font-size:1.12rem;margin:0;color:#10262a">${titulo}</h3>
    <button class="app-boton--texto app-boton" data-cerrar>Cerrar</button>
  </div>`;

/** Marca un paso de la guía de la web como hecho. */
DG.marcarPaso = (paso) => {
  if (DG.estado.pasos[paso]) return;
  DG.estado.pasos[paso] = true;
  DG.guardar();
  DG.pintarPasos();
};

DG.pintarPasos = () => {
  document.querySelectorAll("#listaPasos li").forEach((li) => {
    li.classList.toggle("hecho", !!DG.estado.pasos[li.dataset.paso]);
  });
};

DG.irA = (seccion) => {
  DG.estado.seccion = seccion;
  DG.guardar();
  document.querySelectorAll("#appNav button").forEach((b) => {
    b.classList.toggle("activo", b.dataset.seccion === seccion);
  });
  DG.pintar();
};

/** Renderiza la pantalla que corresponde al estado actual. */
DG.pintar = () => {
  if (!DG.estado.perfil) { DG.renderAlta(); return; }
  DG.prepararTomas();
  switch (DG.estado.seccion) {
    case "medicacion": DG.renderMedicacion(); break;
    case "comidas": DG.renderComidas(); break;
    case "diario": DG.renderDiario(); break;
    case "perfil": DG.renderPerfil(); break;
    default: DG.renderHoy();
  }
  DG.pintarPasos();
};

/* ============================== Alta =================================== */

DG.renderAlta = () => {
  const est = DG.estado;
  const cond = est.condicionPrevia;
  const meta = DG.CONDICIONES[cond];

  const tarjetas = Object.entries(DG.CONDICIONES).map(([id, c]) => `
    <button type="button" class="app-opcion ${id === cond ? "activo" : ""}" data-cond="${id}" style="flex-direction:column;align-items:flex-start;gap:3px">
      <strong>${c.nombre}</strong>
      <small>${c.desc}</small>
    </button>`).join("");

  const zonas = meta.inflamatoria ? `
    <div class="grupo-titulo" style="margin-top:6px">¿Qué zona está afectada?</div>
    <div style="display:grid;gap:2px">
      ${meta.zonas.map((z) => `
        <label class="app-check">
          <input type="checkbox" data-zona="${z}" ${(est.zonasTemp || []).includes(z) ? "checked" : ""}>
          ${z}
        </label>`).join("")}
    </div>
    <div class="grupo-titulo" style="margin-top:8px">Antecedentes quirúrgicos</div>
    <p class="app-ayuda" style="margin:0 0 4px">Una resección o una ostomía cambian el tránsito intestinal, y con él lo que debes anotar.</p>
    <label class="app-check">
      <input type="checkbox" id="altaOstomia" ${est.ostomiaTemp ? "checked" : ""}>
      Tengo una ostomía o una resección intestinal
    </label>` : `
    <div class="app-banner app-banner--ok" style="margin-top:6px">
      <span>✓</span>
      <span><strong>Sin zonas ni cirugía que preguntar</strong>El colon irritable no es una enfermedad inflamatoria: estos apartados no aparecen.</span>
    </div>`;

  DG.pantalla(`
    <div class="pantalla">
      ${DG.cabecera("Digest", "Tu diario de medicación, comidas y síntomas para la enfermedad inflamatoria intestinal o el colon irritable.")}
      <div class="grupo-titulo">¿Cuál es tu diagnóstico?</div>
      <div style="display:grid;gap:9px">${tarjetas}</div>

      <div class="app-campo app-campo--doble">
        <div class="app-campo">
          <label for="altaAnio">Año del diagnóstico</label>
          <input class="app-input" id="altaAnio" inputmode="numeric" placeholder="2019" value="${est.anioTemp || ""}">
        </div>
        <div></div>
      </div>
      <div class="app-campo app-campo--doble">
        <div class="app-campo">
          <label for="altaPeso">Peso (kg)</label>
          <input class="app-input" id="altaPeso" inputmode="decimal" placeholder="66,8" value="${est.pesoTemp || ""}">
        </div>
        <div class="app-campo">
          <label for="altaAltura">Altura (cm)</label>
          <input class="app-input" id="altaAltura" inputmode="decimal" placeholder="168" value="${est.alturaTemp || ""}">
        </div>
      </div>

      ${zonas}

      <button class="app-boton app-boton--ancho" id="btnEmpezar" style="margin-top:8px">Empezar</button>
      <p class="app-ayuda" style="text-align:center">Todo se guarda en este navegador; no sale de tu equipo.</p>
    </div>`);

  const cont = document.getElementById("appScreen");

  cont.querySelectorAll("[data-cond]").forEach((btn) => {
    btn.addEventListener("click", () => {
      est.condicionPrevia = btn.dataset.cond;
      est.zonasTemp = [];
      est.ostomiaTemp = false;
      DG.guardar();
      DG.renderAlta();
      DG.notaDiagnostico();
    });
  });

  cont.querySelectorAll("[data-zona]").forEach((chk) => {
    chk.addEventListener("change", () => {
      est.zonasTemp = est.zonasTemp || [];
      est.zonasTemp = chk.checked
        ? [...new Set([...est.zonasTemp, chk.dataset.zona])]
        : est.zonasTemp.filter((z) => z !== chk.dataset.zona);
    });
  });

  const ostomia = cont.querySelector("#altaOstomia");
  if (ostomia) ostomia.addEventListener("change", () => { est.ostomiaTemp = ostomia.checked; });

  cont.querySelector("#btnEmpezar").addEventListener("click", () => {
    if (!est.condicionPrevia) return;
    const anio = cont.querySelector("#altaAnio").value.trim();
    const peso = parseFloat(cont.querySelector("#altaPeso").value.replace(",", "."));
    const altura = parseFloat(cont.querySelector("#altaAltura").value.replace(",", "."));

    est.perfil = {
      condicion: est.condicionPrevia,
      anio,
      peso: isNaN(peso) ? null : peso,
      altura: isNaN(altura) ? null : altura,
      afectaciones: est.zonasTemp || [],
      ostomia: est.ostomiaTemp || false,
    };
    if (est.perfil.peso) {
      est.pesoHistorico = [{ fecha: DG.hoyISO(), peso: est.perfil.peso }];
    }
    est.zonasTemp = [];
    est.ostomiaTemp = null;
    est.seccion = "hoy";
    DG.guardar();
    DG.marcarPaso("alta");
    DG.snack("Perfil guardado. Bienvenida a Digest.");
    DG.irA("hoy");
  });
};

/* =============================== Hoy =================================== */

DG.renderHoy = () => {
  const est = DG.estado;
  const perfil = est.perfil;
  const cond = DG.CONDICIONES[perfil.condicion];
  const dia = DG.hoy();
  dia.peso = dia.peso ?? perfil.peso;

  const zonas = perfil.afectaciones.length ? ` · ${perfil.afectaciones.join(", ")}` : "";
  const vencidas = dia.tomas.filter((t) => t.estado === "pendiente" && t.hora < DG.horaAhora()).length;
  const proxima = dia.tomas.find((t) => t.estado === "pendiente");

  const avisoPermisos = est.avisoPermisos ? `
    <div class="app-tarjeta app-tarjeta--tonal">
      <div class="app-fila">
        <h3>Activa los avisos</h3>
        <button class="app-boton--texto app-boton" id="cerrarPermisos">Cerrar</button>
      </div>
      <p>Sin permiso de notificaciones no recibirás los avisos de hora de toma. Sin permiso de alarma exacta pueden llegar con retraso.</p>
      <button class="app-boton app-boton--tonal" id="ajustesPermisos">Abrir ajustes</button>
    </div>` : "";

  const tarjetaProxima = proxima ? (() => {
    const vencida = proxima.hora < DG.horaAhora();
    return `
      <div class="app-tarjeta ${vencida ? "app-tarjeta--alerta" : "app-tarjeta--primaria"}">
        <span class="grupo-titulo" style="color:inherit;opacity:.75">${vencida ? "Se te está pasando" : "Ahora te toca"}</span>
        <h3 style="font-size:1.2rem">${proxima.nombre}</h3>
        ${proxima.dosis ? `<p>${proxima.dosis}</p>` : ""}
        <p style="font-size:1.05rem;font-weight:700;color:inherit">${proxima.hora}</p>
        <button class="app-boton app-boton--tonal js-tomar" data-toma="${proxima.id}" style="align-self:flex-start">Ya la he tomado</button>
      </div>`;
  })() : "";

  const filasTomas = dia.tomas.length
    ? dia.tomas.map((t) => `
      <div class="app-tarjeta">
        <div class="app-fila">
          <div class="app-columna">
            <strong class="${t.estado === "omitida" ? "texto-tachado" : ""}">${t.nombre}</strong>
            <span>${t.hora}${t.dosis ? " · " + t.dosis : ""}</span>
          </div>
          <div class="app-acciones">
            ${t.estado === "pendiente"
              ? `<button class="app-boton app-boton--texto js-omitir" data-toma="${t.id}">Omitir</button>
                 <button class="app-boton app-boton--tonal js-tomar" data-toma="${t.id}">Tomada</button>`
              : `<span class="app-estado estado-${t.estado}">${t.estado === "tomada" ? "Tomada" : "Omitida"}</span>
                 <button class="app-boton app-boton--texto js-deshacer" data-toma="${t.id}">Deshacer</button>`}
          </div>
        </div>
      </div>`).join("")
    : DG.vacio("Aún no tienes medicación programada", "Añádela en la pestaña Medicación para empezar a recibir avisos.");

  const comidasHoy = dia.comidas.length
    ? dia.comidas.map((c) => `
      <div class="app-linea">
        <span><strong>${c.tipo}</strong>${c.alimentos ? " · " + c.alimentos : ""}</span>
        <span>${c.hora}</span>
      </div>`).join("")
    : DG.vacio("Todavía no has registrado ninguna comida");

  const pendientes = dia.comidas.filter((c) => c.pendiente);
  const controlHtml = pendientes.map((c) => `
    <div class="app-tarjeta" style="background:#eef7f7;border-color:#cfe6e6">
      <div class="app-fila">
        <h3>Control de la comida</h3>
        <span class="app-estado estado-pendiente">Pendiente</span>
      </div>
      <p>Han pasado 2 horas desde que comiste ${c.tipo.toLowerCase()}. ¿Has notado algún cambio?</p>
      <div class="app-acciones">
        <button class="app-boton app-boton--tonal js-control" data-comida="${c.id}" data-resp="si">Sí, he notado cambios</button>
        <button class="app-boton app-boton--fantasma js-control" data-comida="${c.id}" data-resp="no">No, nada nuevo</button>
      </div>
    </div>`).join("");

  const chips = DG.categoriasDe(perfil.condicion).map(([cat, lista]) => `
    <div class="grupo-titulo">${cat}</div>
    ${DG.chipsHtml(lista.map((s) => {
      const g = dia.sintomas[s.id];
      const estilo = g ? `style="background:${DG.GRAVEDAD[g - 1]};border-color:transparent;color:#fff"` : "";
      return `<button type="button" class="chip ${g ? "marcado" : ""} js-sintoma" data-sint="${s.id}" ${estilo}>
        ${s.nombre}${g ? `<span class="gravedad">${g}</span>` : ""}
      </button>`;
    }).join(""))}`).join("");

  const senales = DG.senales();
  const senalesHtml = senales.length
    ? senales.map((s) => `
      <div class="senal senal--${s.nivel}">
        <span class="senal__ico">${s.nivel === "urgente" ? "!!" : "i"}</span>
        <span><strong>${s.titulo}</strong>${s.motivo}</span>
      </div>`).join("")
    : `<div class="senal senal--normal"><span class="senal__ico">✓</span><span><strong>Nada que adelantar</strong>Con lo registrado hoy no se detecta ninguna señal de alerta.</span></div>`;

  DG.pantalla(`
    <div class="pantalla">
      <header class="app-cab">
        <h2>${DG.fechaLarga(DG.hoyISO())}</h2>
        <span>${cond.nombre}${zonas}</span>
      </header>

      ${avisoPermisos}
      ${tarjetaProxima}

      <div class="app-seccion">
        <h3>Tomas de hoy</h3>
        ${vencidas ? `<span>${vencidas} sin tomar</span>` : ""}
      </div>
      <div class="app-lista">${filasTomas}</div>

      <div class="app-seccion" style="margin-top:14px;border-top:1px solid #e6eeee;padding-top:12px"><h3>Comidas de hoy</h3></div>
      <div class="app-lista">${comidasHoy}</div>
      <button class="app-boton app-boton--tonal app-boton--ancho js-nueva-comida">Registrar comida</button>

      <div class="app-seccion" style="margin-top:14px;border-top:1px solid #e6eeee;padding-top:12px"><h3>¿Cómo estás hoy?</h3></div>
      ${controlHtml}
      ${chips}
      <p class="app-ayuda">Pulsa un síntoma para marcarlo y vuelve a pulsar para aumentar la intensidad. En el máximo, el siguiente toque lo quita.</p>

      <div class="app-seccion" style="margin-top:14px;border-top:1px solid #e6eeee;padding-top:12px">
        <h3>Señales de alerta</h3>
        <span>${senales.length}</span>
      </div>
      <div class="app-senales">${senalesHtml}</div>
      <p class="app-ayuda">Digest no diagnostica. Estas señales señalan situaciones que no deberían esperar: ante una urgencia, acude a un servicio médico.</p>
      <button class="app-boton app-boton--fantasma app-boton--ancho js-directorio">Ver profesionales</button>

      <div class="app-tarjeta app-tarjeta--tonal" style="margin-top:6px">
        <h3>Compartir mi historia con el médico</h3>
        <p>Se mandan juntos el resumen de tus síntomas y todos los informes que hayas adjuntado. Eliges el periodo y el destino.</p>
        ${est.documentos.length ? "" : `<p style="color:#8c6b00"><strong>No has adjuntado ningún informe, así que se manda solo el resumen.</strong></p>`}
        <button class="app-boton app-boton--ancho js-historia">Compartir con el médico</button>
      </div>
    </div>`);

  DG.bindHoy();
};

DG.bindHoy = () => {
  const cont = document.getElementById("appScreen");

  cont.querySelector("#cerrarPermisos")?.addEventListener("click", () => {
    DG.estado.avisoPermisos = false; DG.guardar(); DG.renderHoy();
  });
  cont.querySelector("#ajustesPermisos")?.addEventListener("click", () => {
    DG.snack("En la app real esto abre los ajustes de Android.");
  });

  cont.querySelectorAll(".js-tomar").forEach((b) => b.addEventListener("click", () => {
    const t = DG.hoy().tomas.find((x) => x.id === b.dataset.toma);
    if (t) t.estado = "tomada";
    DG.guardar(); DG.marcarPaso("toma"); DG.renderHoy();
    DG.snack("Toma registrada.");
  }));

  cont.querySelectorAll(".js-omitir").forEach((b) => b.addEventListener("click", () => {
    const t = DG.hoy().tomas.find((x) => x.id === b.dataset.toma);
    if (t) t.estado = "omitida";
    DG.guardar(); DG.marcarPaso("toma"); DG.renderHoy();
    DG.snack("Toma omitida. Puedes deshacerlo desde aquí.");
  }));

  cont.querySelectorAll(".js-deshacer").forEach((b) => b.addEventListener("click", () => {
    const t = DG.hoy().tomas.find((x) => x.id === b.dataset.toma);
    if (t) t.estado = "pendiente";
    DG.guardar(); DG.renderHoy();
  }));

  cont.querySelectorAll(".js-sintoma").forEach((b) => b.addEventListener("click", () => {
    const dia = DG.hoy();
    const actual = dia.sintomas[b.dataset.sint] || 0;
    if (actual >= 5) delete dia.sintomas[b.dataset.sint];
    else dia.sintomas[b.dataset.sint] = actual + 1;
    DG.guardar(); DG.marcarPaso("sintoma");
    DG.renderHoy();
    DG.snack(DG.mensajeRegistro());
  }));

  cont.querySelectorAll(".js-control").forEach((b) => b.addEventListener("click", () => {
    const c = DG.hoy().comidas.find((x) => x.id === b.dataset.comida);
    if (c) { c.control = b.dataset.resp; c.pendiente = false; }
    DG.guardar(); DG.marcarPaso("comida");
    DG.renderHoy();
    DG.snack(b.dataset.resp === "si"
      ? "Guardado. Revisa la lista de síntomas y márcalos si siguen."
      : "Guardado. Sin cambios registrados.");
  }));

  cont.querySelector(".js-nueva-comida")?.addEventListener("click", DG.hojaComida);
  cont.querySelector(".js-directorio")?.addEventListener("click", DG.hojaDirectorio);
  cont.querySelector(".js-historia")?.addEventListener("click", DG.hojaHistoria);
};

/* ============================ Medicación =============================== */

DG.renderMedicacion = () => {
  const meds = DG.estado.medicamentos;

  const tarjetas = meds.length
    ? meds.map((m) => `
      <div class="app-tarjeta">
        <div class="app-fila">
          <div class="app-columna">
            <strong>${m.nombre}</strong>
            <span>${m.dosis || "Sin dosis"}${m.instrucciones ? " · " + m.instrucciones : ""}</span>
          </div>
          <button class="app-boton app-boton--texto js-borrar-med" data-med="${m.id}">Borrar</button>
        </div>
        <div class="app-chips">
          ${m.horarios.map((h) => `<span class="chip" style="background:#d6f6f8;border-color:#a9e6ea;color:#0b3b3d">${h}</span>`).join("")}
        </div>
        <span class="grupo-titulo">${m.presentacion}${m.grupo ? " · " + m.grupo : ""}</span>
      </div>`).join("")
    : DG.vacio("No hay medicación registrada", "Pulsa + para añadir tu primer fármaco o suplemento.");

  DG.pantalla(`
    <div class="pantalla">
      <div class="app-fila">
        ${DG.cabecera("Medicación", meds.length ? `${meds.length} fármacos o suplementos en la agenda` : "Agenda de fármacos y suplementos")}
        <button class="app-boton js-nuevo-med" style="width:42px;height:42px;padding:0;font-size:1.3rem;border-radius:50%">+</button>
      </div>
      <div class="app-lista">${tarjetas}</div>
      <div class="app-banner app-banner--aviso">
        <span>!</span>
        <span><strong>Avisos de toma</strong>Cada horario genera una notificación con acciones <em>Tomada</em> y <em>Omitir</em>, y un refuerzo a los 20 minutos si sigue pendiente.</span>
      </div>
    </div>`);

  const cont = document.getElementById("appScreen");
  cont.querySelector(".js-nuevo-med")?.addEventListener("click", () => DG.hojaMedicamento());
  cont.querySelectorAll(".js-borrar-med").forEach((b) => b.addEventListener("click", () => {
    DG.estado.medicamentos = DG.estado.medicamentos.filter((m) => m.id !== b.dataset.med);
    DG.hoy().tomas = DG.hoy().tomas.filter((t) => t.medId !== b.dataset.med);
    DG.guardar(); DG.renderMedicacion();
    DG.snack("Medicación eliminada de la agenda.");
  }));
};

/* ============================== Comidas ================================ */

DG.renderComidas = () => {
  const dia = DG.hoy();

  // Repetir con un toque: alimentos anotados últimamente, sin repetir.
  const recientes = [];
  DG.diasOrdenados().reverse().forEach((d) => {
    d.comidas.forEach((c) => {
      if (c.alimentos && !recientes.some((r) => r.alimentos === c.alimentos) && recientes.length < 4) {
        recientes.push({ tipo: c.tipo, alimentos: c.alimentos, hora: c.hora });
      }
    });
  });

  const lista = dia.comidas.length
    ? dia.comidas.map((c) => `
      <div class="app-tarjeta">
        <div class="app-fila">
          <div class="app-columna">
            <strong>${c.tipo} · ${c.hora}</strong>
            <span>${c.alimentos || "Sin detalle de alimentos"}</span>
          </div>
          ${c.control === "si"
            ? `<span class="app-estado estado-omitida">Cambios</span>`
            : (c.control === "sin"
              ? `<span class="app-estado estado-tomada">Sin cambios</span>`
              : (c.pendiente
                ? `<span class="app-estado estado-pendiente">Control pendiente</span>`
                : `<span class="app-estado estado-pendiente">Sin controlar</span>`))}
        </div>
      </div>`).join("")
    : DG.vacio("Sin comidas registradas", "Anotar lo que comes ayuda a ver qué alimentos se relacionan con los síntomas.");

  DG.pantalla(`
    <div class="pantalla">
      <div class="app-fila">
        ${DG.cabecera("Comidas", "Tipo, hora y lo que se ha comido, no solo la categoría")}
        <button class="app-boton js-nueva-comida" style="width:42px;height:42px;padding:0;font-size:1.3rem;border-radius:50%">+</button>
      </div>

      ${recientes.length ? `
        <div class="grupo-titulo">Lo último que anotaste</div>
        ${DG.chipsHtml(recientes.map((r, i) => `<button type="button" class="chip js-repetir" data-rep="${i}">${r.alimentos}</button>`).join(""))}` : ""}

      <div class="app-seccion" style="margin-top:6px"><h3>Comidas de hoy</h3></div>
      <div class="app-lista">${lista}</div>

      ${dia.comidas.some((c) => c.pendiente) ? `
        <div class="app-banner app-banner--aviso">
          <span>!</span>
          <span><strong>Hay un control pendiente</strong>Vuelve a la pestaña Hoy y responde si has notado algún cambio desde que comiste.</span>
        </div>` : ""}
    </div>`);

  const cont = document.getElementById("appScreen");
  cont.querySelector(".js-nueva-comida")?.addEventListener("click", DG.hojaComida);
  cont.querySelectorAll(".js-repetir").forEach((b) => b.addEventListener("click", () => {
    const r = recientes[Number(b.dataset.rep)];
    DG.registrarComida({ tipo: r.tipo, hora: DG.horaAhora(), alimentos: r.alimentos, repetida: true });
  }));
};

/** Guarda una comida, programa su control y lanza el aviso simulado. */
DG.registrarComida = ({ tipo, hora, alimentos, repetida }) => {
  const dia = DG.hoy();
  dia.comidas.push({ id: DG.uid(), tipo, hora, alimentos, control: null, pendiente: true });
  DG.guardar();
  DG.cerrarHoja();
  DG.marcarPaso("comida");
  DG.irA("hoy");
  DG.snack(repetida ? `Comida repetida: ${tipo} de las ${hora}.` : `Comida registrada: ${tipo} de las ${hora}.`);

  clearTimeout(DG.timerControl);
  DG.timerControl = setTimeout(() => {
    DG.mostrarAviso({
      titulo: "¿Cómo llevas la comida?",
      cuerpo: `Han pasado 2 horas desde que comiste ${tipo.toLowerCase()}. ¿Has notado algún cambio?`,
      tipo: "control",
      comidaId: dia.comidas[dia.comidas.length - 1].id,
    });
  }, 3800);
};
