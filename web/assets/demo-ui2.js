/* =========================================================================
   Digest · demo · interfaz (parte 2)
   Diario, Perfil, hojas modales (comida, medicación, cuerpo, documentos,
   directorio, informe), avisos simulados y arranque de la demo.
   ========================================================================= */

/* =============================== Diario ================================= */

DG.renderDiario = () => {
  const est = DG.estado;
  const cond = DG.CONDICIONES[est.perfil.condicion];
  const dia = DG.hoy();

  const resumenSintomas = Object.keys(dia.sintomas).length
    ? DG.chipsHtml(Object.entries(dia.sintomas).map(([id, g]) => {
        const s = DG.SINTOMAS.find((x) => x.id === id);
        return `<button type="button" class="chip marcado js-sintoma-diario" data-sint="${id}"
          style="background:${DG.GRAVEDAD[g - 1]};border-color:transparent;color:#fff">
          ${s ? s.nombre : id}<span class="gravedad">${g}</span></button>`;
      }).join(""))
    : `<div class="app-vacio"><strong>Hoy no has marcado ningún síntoma.</strong><span>Se marcan desde la pestaña Hoy.</span></div>`;

  const grupo = (titulo, campo, opciones) => `
    <div class="app-tarjeta">
      <h3>${titulo}</h3>
      <div class="app-segsel">
        ${opciones.map((o) => `<button type="button" class="${dia.diario[campo] === o ? "activo" : ""} js-diario" data-campo="${campo}" data-valor="${o}">${o}</button>`).join("")}
      </div>
    </div>`;

  const ml = dia.enteral.reduce((a, b) => a + (Number(b.volumen) || 0), 0);

  DG.pantalla(`
    <div class="pantalla">
      ${DG.cabecera("Diario", DG.fechaLarga(DG.hoyISO()))}

      <div class="app-tarjeta">
        <h3>Registro del cuerpo</h3>
        <p>Apunta deposiciones, dolor y nutrición enteral. Es lo que el médico revisa para ver cómo evoluciona la enfermedad.</p>
        <div class="app-segsel">
          <button type="button" class="js-deposicion">Deposición</button>
          <button type="button" class="js-dolor">Dolor</button>
          <button type="button" class="js-enteral">Nutrición enteral</button>
        </div>
        <div class="app-fila" style="font-size:.84rem;color:#4e5d5e">
          <span>Deposiciones de hoy: <strong>${dia.deposiciones.length}</strong></span>
          <span>Hoy: <strong>${ml} ml</strong></span>
        </div>
      </div>

      <div class="app-seccion"><h3>Resumen de hoy</h3><span>${Object.keys(dia.sintomas).length} síntomas</span></div>
      ${resumenSintomas}
      <p class="app-ayuda">Pulsa para cambiar la intensidad.</p>

      ${grupo("¿La medicación te está mejorando?", "mejora", DG.MEJORA)}
      ${grupo("¿Cómo te sientes en general?", "sensacion", DG.SENSACION)}
      ${grupo(cond.control, "control", cond.opciones)}
      ${grupo("Hidratación", "hidratacion", DG.HIDRATACION)}

      <div class="app-campo">
        <label for="notasDia">Notas del día</label>
        <textarea class="app-textarea" id="notasDia" rows="3" placeholder="Lo que quieras recordar de hoy…">${dia.diario.notas || ""}</textarea>
      </div>

      <div class="app-tarjeta app-tarjeta--tonal">
        <h3>Informe para tu médico</h3>
        <p>Genera un resumen del periodo para llevarlo a la consulta. No sale de tu móvil: tú decides a quién se lo mandas.</p>
        <div class="app-fila">
          <div class="app-segsel" id="selDias">
            <button type="button" class="activo" data-dias="7">7 días</button>
            <button type="button" data-dias="15">15 días</button>
            <button type="button" data-dias="30">30 días</button>
          </div>
          <div class="app-segsel" id="selFormato">
            <button type="button" class="activo" data-fmt="texto">Texto</button>
            <button type="button" data-fmt="csv">CSV</button>
          </div>
        </div>
        <button class="app-boton app-boton--ancho" id="btnInformeDiario">Generar informe</button>
      </div>
    </div>`);

  const cont = document.getElementById("appScreen");

  cont.querySelectorAll(".js-sintoma-diario").forEach((b) => b.addEventListener("click", () => {
    const g = dia.sintomas[b.dataset.sint] || 0;
    if (g >= 5) delete dia.sintomas[b.dataset.sint];
    else dia.sintomas[b.dataset.sint] = g + 1;
    DG.guardar(); DG.renderDiario();
    DG.snack(DG.mensajeRegistro());
  }));

  cont.querySelectorAll(".js-diario").forEach((b) => b.addEventListener("click", () => {
    dia.diario[b.dataset.campo] = dia.diario[b.dataset.campo] === b.dataset.valor ? null : b.dataset.valor;
    DG.guardar(); DG.renderDiario();
  }));

  cont.querySelector("#notasDia")?.addEventListener("input", (ev) => {
    dia.diario.notas = ev.target.value;
    DG.guardar();
  });

  cont.querySelector(".js-deposicion")?.addEventListener("click", DG.hojaDeposicion);
  cont.querySelector(".js-dolor")?.addEventListener("click", DG.hojaDolor);
  cont.querySelector(".js-enteral")?.addEventListener("click", DG.hojaEnteral);

  let diasInforme = 7, formatoInforme = "texto";
  cont.querySelectorAll("#selDias button").forEach((b) => b.addEventListener("click", () => {
    cont.querySelectorAll("#selDias button").forEach((x) => x.classList.toggle("activo", x === b));
    diasInforme = Number(b.dataset.dias);
  }));
  cont.querySelectorAll("#selFormato button").forEach((b) => b.addEventListener("click", () => {
    cont.querySelectorAll("#selFormato button").forEach((x) => x.classList.toggle("activo", x === b));
    formatoInforme = b.dataset.fmt;
  }));
  cont.querySelector("#btnInformeDiario")?.addEventListener("click", () => DG.hojaInforme(diasInforme, formatoInforme));
};

/* =============================== Perfil ================================= */

DG.renderPerfil = () => {
  const est = DG.estado;
  const p = est.perfil;
  const cond = DG.CONDICIONES[p.condicion];
  const imc = p.peso && p.altura
    ? (p.peso / Math.pow(p.altura / 100, 2)).toFixed(1).replace(".", ",")
    : null;

  const diasConsulta = est.ultimaConsulta ? DG.diasEntre(DG.hoyISO(), est.ultimaConsulta) : null;

  const docs = est.documentos.length
    ? est.documentos.map((d) => `
      <div class="app-doc">
        <div class="app-doc__ico">${d.formato}</div>
        <div class="app-doc__txt">
          <strong>${d.nombre}</strong>
          <span>${d.tipo} · ${d.fecha} · ${d.tamano}</span>
        </div>
        <button class="app-boton app-boton--texto js-borrar-doc" data-doc="${d.id}">Borrar</button>
      </div>`).join("")
    : DG.vacio("Todavía no has adjuntado ningún informe", "Añade la última colonoscopia, la analítica o el informe de la biopsia para tenerlo a mano en la consulta.");

  const cirugias = est.cirugias.length
    ? est.cirugias.map((c) => `
      <div class="app-linea"><span><strong>${c.tipo}</strong></span><span>${c.fecha || ""}</span></div>`).join("")
    : `<p class="app-ayuda">No hay intervenciones registradas.</p>`;

  DG.pantalla(`
    <div class="pantalla">
      ${DG.cabecera("Perfil", `${cond.nombre}${p.afectaciones.length ? " · " + p.afectaciones.join(", ") : ""}`)}

      <div class="app-tarjeta">
        <div class="app-fila">
          <div class="app-columna">
            <span>Condición</span>
            <strong>${cond.nombre}</strong>
          </div>
          <button class="app-boton app-boton--texto js-cambiar-cond">Cambiar</button>
        </div>
        ${p.ostomia ? `<div class="app-banner app-banner--aviso"><span>!</span><span>Tienes una ostomía registrada.</span></div>` : ""}
        <p>${cond.nota}</p>
      </div>

      <div class="app-tarjeta">
        <h3>Peso y altura</h3>
        <div class="app-campo app-campo--doble">
          <div class="app-campo">
            <label for="pfPeso">Peso (kg)</label>
            <input class="app-input" id="pfPeso" inputmode="decimal" value="${p.peso != null ? String(p.peso).replace(".", ",") : ""}">
          </div>
          <div class="app-campo">
            <label for="pfAltura">Altura (cm)</label>
            <input class="app-input" id="pfAltura" inputmode="decimal" value="${p.altura ?? ""}">
          </div>
        </div>
        ${imc ? `<p class="app-ayuda">IMC: ${imc}</p>` : ""}
        <button class="app-boton app-boton--tonal" id="guardarMedidas">Guardar medidas</button>
      </div>

      <div class="app-tarjeta">
        <h3>Consultas</h3>
        <p>${est.ultimaConsulta ? `Última consulta: hace ${diasConsulta} días (${DG.fmtFecha(est.ultimaConsulta)})` : "Todavía no has registrado ninguna consulta"}</p>
        <div class="app-fila">
          <div class="app-campo" style="flex:1">
            <label for="pfIntervalo">Cada cuántos meses te revisas</label>
            <select class="app-select" id="pfIntervalo">
              ${[3, 6, 9, 12].map((m) => `<option value="${m}" ${est.intervaloMeses === m ? "selected" : ""}>${m} meses</option>`).join("")}
            </select>
          </div>
        </div>
        <div class="app-segsel">
          <button type="button" id="regConsulta">Registrar consulta de hoy</button>
          <button type="button" id="guardarIntervalo">Guardar intervalo</button>
        </div>
      </div>

      <div class="app-tarjeta">
        <div class="app-fila"><h3>Cirugías</h3><button class="app-boton app-boton--texto js-add-cirugia">Añadir</button></div>
        <p class="app-ayuda">Una resección ileocecal o una ostomía cambian el tránsito intestinal y las necesidades de hidratación.</p>
        ${cirugias}
      </div>

      <div class="app-seccion" style="border-top:1px solid #e6eeee;padding-top:12px"><h3>Mis informes médicos</h3></div>
      <p class="app-ayuda">Adjunta los análisis, endoscopias y pruebas que te han hecho. Se quedan en el móvil y solo se mandan cuando tú lo decides.</p>
      <div class="app-docs">${docs}</div>
      <button class="app-boton app-boton--tonal app-boton--ancho js-add-doc">Añadir informe</button>

      <div class="app-tarjeta app-tarjeta--tonal">
        <h3>Profesionales</h3>
        <p>Gastroenterología y nutrición, priorizando a los especializados en enfermedad inflamatoria intestinal.</p>
        <button class="app-boton app-boton--ancho js-directorio-perfil">Abrir directorio</button>
      </div>
    </div>`);

  const cont = document.getElementById("appScreen");

  cont.querySelector("#guardarMedidas")?.addEventListener("click", () => {
    const peso = parseFloat(cont.querySelector("#pfPeso").value.replace(",", "."));
    const altura = parseFloat(cont.querySelector("#pfAltura").value.replace(",", "."));
    p.peso = isNaN(peso) ? null : peso;
    p.altura = isNaN(altura) ? null : altura;
    if (p.peso) {
      est.pesoHistorico = est.pesoHistorico || [];
      const hoy = est.pesoHistorico.find((w) => w.fecha === DG.hoyISO());
      if (hoy) hoy.peso = p.peso; else est.pesoHistorico.push({ fecha: DG.hoyISO(), peso: p.peso });
    }
    DG.guardar(); DG.renderPerfil();
    DG.snack("Medidas guardadas.");
  });

  cont.querySelector("#guardarIntervalo")?.addEventListener("click", () => {
    est.intervaloMeses = Number(cont.querySelector("#pfIntervalo").value);
    DG.guardar(); DG.renderPerfil();
    DG.snack("Intervalo de revisión guardado.");
  });

  cont.querySelector("#regConsulta")?.addEventListener("click", () => {
    est.ultimaConsulta = DG.hoyISO();
    DG.guardar(); DG.renderPerfil();
    DG.snack("Consulta de hoy registrada.");
  });

  cont.querySelector(".js-cambiar-cond")?.addEventListener("click", DG.hojaCondicion);
  cont.querySelector(".js-add-cirugia")?.addEventListener("click", DG.hojaCirugia);
  cont.querySelector(".js-add-doc")?.addEventListener("click", DG.hojaDocumento);
  cont.querySelector(".js-directorio-perfil")?.addEventListener("click", DG.hojaDirectorio);

  cont.querySelectorAll(".js-borrar-doc").forEach((b) => b.addEventListener("click", () => {
    const doc = est.documentos.find((d) => d.id === b.dataset.doc);
    est.documentos = est.documentos.filter((d) => d.id !== b.dataset.doc);
    DG.guardar(); DG.renderPerfil();
    DG.snack(`«${doc ? doc.nombre : "Documento"}» eliminado del móvil.`);
  }));
};

/* ============================== Hojas ================================== */

DG.hojaComida = () => {
  const recientes = [];
  DG.diasOrdenados().reverse().forEach((d) => d.comidas.forEach((c) => {
    if (c.alimentos && !recientes.includes(c.alimentos) && recientes.length < 4) recientes.push(c.alimentos);
  }));

  DG.abrirHoja(`
    ${DG.tituloHoja("Registrar comida")}
    <div class="app-campo">
      <label>Tipo de comida</label>
      <div class="app-segsel" id="tipoComida">
        ${DG.TIPOS_COMIDA.map((t, i) => `<button type="button" class="${i === 0 ? "activo" : ""}" data-tipo="${t}">${t}</button>`).join("")}
      </div>
    </div>
    <div class="app-campo" style="margin-top:12px">
      <label for="horaComida">Hora</label>
      <input class="app-input" type="time" id="horaComida" value="${DG.horaAhora()}">
    </div>
    <div class="app-campo" style="margin-top:12px">
      <label for="alimentos">¿Qué has comido?</label>
      <input class="app-input" id="alimentos" placeholder="Pollo con arroz, sopa, yogur…">
    </div>
    ${recientes.length ? `
      <div class="grupo-titulo" style="margin-top:12px">Repetir con un toque</div>
      ${DG.chipsHtml(recientes.map((r, i) => `<button type="button" class="chip js-usar" data-i="${i}">${r}</button>`).join(""))}` : ""}
    <button class="app-boton app-boton--ancho" id="guardarComida" style="margin-top:16px">Guardar</button>
  `, (hoja) => {
    let tipo = DG.TIPOS_COMIDA[0];
    hoja.querySelectorAll("#tipoComida button").forEach((b) => b.addEventListener("click", () => {
      hoja.querySelectorAll("#tipoComida button").forEach((x) => x.classList.toggle("activo", x === b));
      tipo = b.dataset.tipo;
    }));
    hoja.querySelectorAll(".js-usar").forEach((b) => b.addEventListener("click", () => {
      hoja.querySelector("#alimentos").value = recientes[Number(b.dataset.i)];
    }));
    hoja.querySelector("#guardarComida").addEventListener("click", () => {
      const hora = hoja.querySelector("#horaComida").value || DG.horaAhora();
      const alimentos = hoja.querySelector("#alimentos").value.trim();
      DG.registrarComida({ tipo, hora, alimentos });
    });
  });
};

DG.hojaMedicamento = () => {
  const horasComunes = ["06:00", "08:00", "09:00", "12:00", "14:00", "18:00", "20:00", "22:00"];

  DG.abrirHoja(`
    ${DG.tituloHoja("Nueva medicación")}
    <div class="app-campo">
      <label for="medNombre">Nombre del fármaco</label>
      <input class="app-input" id="medNombre" placeholder="Mesalazina 1 g">
    </div>
    <div class="app-campo app-campo--doble" style="margin-top:12px">
      <div class="app-campo">
        <label for="medDosis">Dosis</label>
        <input class="app-input" id="medDosis" placeholder="2,5 g · 1 comprimido">
      </div>
      <div class="app-campo">
        <label for="medGrupo">Grupo</label>
        <select class="app-select" id="medGrupo">${DG.GRUPOS.map((g) => `<option>${g}</option>`).join("")}</select>
      </div>
    </div>
    <div class="app-campo app-campo--doble" style="margin-top:12px">
      <div class="app-campo">
        <label for="medPresent">Presentación</label>
        <select class="app-select" id="medPresent">${DG.PRESENTACIONES.map((p) => `<option>${p}</option>`).join("")}</select>
      </div>
      <div class="app-campo">
        <label for="medInstr">Indicaciones</label>
        <input class="app-input" id="medInstr" placeholder="Con comida · En ayunas">
      </div>
    </div>
    <div class="app-campo" style="margin-top:12px">
      <label>Horarios de la toma (varios al día)</label>
      <div class="app-segsel" id="medHoras">
        ${horasComunes.map((h) => `<button type="button" data-hora="${h}">${h}</button>`).join("")}
      </div>
      <div class="app-fila" style="margin-top:8px">
        <input class="app-input" type="time" id="medHoraExtra">
        <button class="app-boton app-boton--tonal" id="addHora">Añadir</button>
      </div>
    </div>
    <p class="app-ayuda">Por defecto, todos los días. Los avisos se reprograman al abrir la app y tras cada reinicio.</p>
    <div class="app-fila" style="margin-top:14px">
      <button class="app-boton app-boton--fantasma" data-cerrar>Cancelar</button>
      <button class="app-boton" id="guardarMed" style="flex:1">Guardar</button>
    </div>
  `, (hoja) => {
    const horas = new Set(["08:00"]);
    const pintarHoras = () => hoja.querySelectorAll("#medHoras button").forEach((b) => {
      b.classList.toggle("activo", horas.has(b.dataset.hora));
    });
    pintarHoras();

    hoja.querySelectorAll("#medHoras button").forEach((b) => b.addEventListener("click", () => {
      horas.has(b.dataset.hora) ? horas.delete(b.dataset.hora) : horas.add(b.dataset.hora);
      pintarHoras();
    }));

    hoja.querySelector("#addHora").addEventListener("click", () => {
      const v = hoja.querySelector("#medHoraExtra").value;
      if (v) { horas.add(v); pintarHoras(); }
    });

    hoja.querySelector("#guardarMed").addEventListener("click", () => {
      const nombre = hoja.querySelector("#medNombre").value.trim();
      if (!nombre) { DG.snack("Falta el nombre del fármaco."); return; }
      const lista = [...horas].sort();
      if (!lista.length) { DG.snack("Elige al menos una hora de toma."); return; }

      DG.estado.medicamentos.push({
        id: DG.uid(),
        nombre,
        dosis: hoja.querySelector("#medDosis").value.trim(),
        grupo: hoja.querySelector("#medGrupo").value,
        presentacion: hoja.querySelector("#medPresent").value,
        instrucciones: hoja.querySelector("#medInstr").value.trim(),
        horarios: lista,
      });
      DG.guardar();
      DG.cerrarHoja();
      DG.renderMedicacion();
      DG.snack("Medicación guardada. Ya genera sus avisos.");
    });
  });
};

DG.hojaDeposicion = () => {
  DG.abrirHoja(`
    ${DG.tituloHoja("Registrar deposición")}
    <div class="app-campo">
      <label>Consistencia (escala de Bristol)</label>
      <div class="app-bristol" id="bristolSel">
        ${DG.BRISTOL.map((b) => `<button type="button" data-b="${b.n}" style="background:${b.color}">${b.n}</button>`).join("")}
      </div>
      <div class="app-bristol-info" id="bristolInfo">Elige un tipo de la escala del 1 al 7.</div>
    </div>
    <div class="app-campo" style="margin-top:14px">
      <label>Sangre</label>
      <div class="app-segsel" id="sangreSel">
        ${DG.SANGRE.map((s, i) => `<button type="button" class="${i === 0 ? "activo" : ""}" data-sangre="${s}">${s}</button>`).join("")}
      </div>
    </div>
    <button class="app-boton app-boton--ancho" id="guardarDep" style="margin-top:18px" disabled>Guardar</button>
  `, (hoja) => {
    let bristol = null, sangre = "Ninguna";
    const info = hoja.querySelector("#bristolInfo");
    const btnGuardar = hoja.querySelector("#guardarDep");

    hoja.querySelectorAll("#bristolSel button").forEach((b) => b.addEventListener("click", () => {
      bristol = Number(b.dataset.b);
      hoja.querySelectorAll("#bristolSel button").forEach((x) => x.classList.toggle("activo", x === b));
      const d = DG.BRISTOL[bristol - 1];
      info.innerHTML = `<strong>Bristol ${d.n} · ${d.nombre}.</strong> ${d.desc}`;
      btnGuardar.disabled = false;
    }));

    hoja.querySelectorAll("#sangreSel button").forEach((b) => b.addEventListener("click", () => {
      sangre = b.dataset.sangre;
      hoja.querySelectorAll("#sangreSel button").forEach((x) => x.classList.toggle("activo", x === b));
    }));

    btnGuardar.addEventListener("click", () => {
      if (!bristol) return;
      DG.hoy().deposiciones.push({ id: DG.uid(), hora: DG.horaAhora(), bristol, sangre });
      DG.guardar();
      DG.cerrarHoja();
      DG.marcarPaso("cuerpo");
      DG.renderDiario();
      DG.snack(DG.mensajeRegistro());
    });
  });
};

DG.hojaDolor = () => {
  DG.abrirHoja(`
    ${DG.tituloHoja("Registrar dolor abdominal")}
    <div class="app-campo">
      <label>Intensidad: <span id="intVal">3</span> de 5</label>
      <div class="app-intens" id="intSel">
        ${[1, 2, 3, 4, 5].map((n) => `<button type="button" class="${n === 3 ? "activo" : ""}" data-n="${n}" style="${n === 3 ? `background:${DG.GRAVEDAD[2]};border-color:transparent` : ""}">${n}</button>`).join("")}
      </div>
    </div>
    <div class="app-campo" style="margin-top:14px">
      <label>Cómo es el dolor</label>
      <div style="display:grid;gap:7px" id="tipoSel">
        ${DG.TIPOS_DOLORES.map(([t, pista], i) => `
          <button type="button" class="app-opcion ${i === 0 ? "activo" : ""}" data-tipo="${t}">
            <span class="marcador"></span>
            <span>${t}<small>${pista}</small></span>
          </button>`).join("")}
      </div>
    </div>
    <div class="app-campo" style="margin-top:14px">
      <label for="zonaDolor">Dónde lo notas</label>
      <select class="app-select" id="zonaDolor">${DG.ZONAS_DOLOR.map((z) => `<option>${z}</option>`).join("")}</select>
    </div>
    <button class="app-boton app-boton--ancho" id="guardarDolor" style="margin-top:18px">Guardar</button>
  `, (hoja) => {
    let intensidad = 3, tipo = DG.TIPOS_DOLORES[0][0];

    hoja.querySelectorAll("#intSel button").forEach((b) => b.addEventListener("click", () => {
      intensidad = Number(b.dataset.n);
      hoja.querySelectorAll("#intSel button").forEach((x) => {
        const n = Number(x.dataset.n);
        x.classList.toggle("activo", x === b);
        x.style.cssText = x === b ? `background:${DG.GRAVEDAD[n - 1]};border-color:transparent;color:#fff` : "";
      });
      hoja.querySelector("#intVal").textContent = intensidad;
    }));

    hoja.querySelectorAll("#tipoSel button").forEach((b) => b.addEventListener("click", () => {
      tipo = b.dataset.tipo;
      hoja.querySelectorAll("#tipoSel button").forEach((x) => x.classList.toggle("activo", x === b));
    }));

    hoja.querySelector("#guardarDolor").addEventListener("click", () => {
      DG.hoy().dolores.push({
        id: DG.uid(),
        intensidad,
        tipo,
        zona: hoja.querySelector("#zonaDolor").value,
      });
      const dia = DG.hoy();
      dia.sintomas.dolor_abdominal = intensidad;
      DG.guardar();
      DG.cerrarHoja();
      DG.marcarPaso("cuerpo");
      DG.renderDiario();
      DG.snack(DG.mensajeRegistro());
    });
  });
};

DG.hojaEnteral = () => {
  DG.abrirHoja(`
    ${DG.tituloHoja("Registrar nutrición enteral")}
    <div class="app-campo">
      <label for="entProducto">Producto</label>
      <input class="app-input" id="entProducto" placeholder="Fórmula estándar 1,5 kcal/ml">
    </div>
    <div class="app-campo app-campo--doble" style="margin-top:12px">
      <div class="app-campo">
        <label for="entVolumen">Volumen (ml)</label>
        <input class="app-input" id="entVolumen" inputmode="numeric" placeholder="1500">
      </div>
      <div class="app-campo">
        <label for="entTipo">Tipo</label>
        <select class="app-select" id="entTipo">${DG.TIPOS_ENTERAL.map((t) => `<option>${t}</option>`).join("")}</select>
      </div>
    </div>
    <button class="app-boton app-boton--ancho" id="guardarEnteral" style="margin-top:16px">Guardar</button>
  `, (hoja) => {
    hoja.querySelector("#guardarEnteral").addEventListener("click", () => {
      DG.hoy().enteral.push({
        id: DG.uid(),
        producto: hoja.querySelector("#entProducto").value.trim() || "Fórmula",
        volumen: hoja.querySelector("#entVolumen").value.trim() || "0",
        tipo: hoja.querySelector("#entTipo").value,
      });
      DG.guardar();
      DG.cerrarHoja();
      DG.marcarPaso("cuerpo");
      DG.renderDiario();
      DG.snack("Nutrición enteral registrada.");
    });
  });
};

DG.hojaDocumento = () => {
  DG.abrirHoja(`
    ${DG.tituloHoja("Añadir informe")}
    <div class="app-banner app-banner--ok">
      <span>✓</span>
      <span>El archivo se <strong>copia dentro de la carpeta privada de la app</strong>: sigue disponible aunque borres el original de Descargas.</span>
    </div>
    <div class="app-campo" style="margin-top:12px">
      <label for="docNombre">Nombre</label>
      <input class="app-input" id="docNombre" placeholder="Colonoscopia 2024">
    </div>
    <div class="app-campo app-campo--doble" style="margin-top:12px">
      <div class="app-campo">
        <label for="docTipo">Tipo de informe</label>
        <select class="app-select" id="docTipo">${DG.TIPOS_DOCS.map((t) => `<option>${t}</option>`).join("")}</select>
      </div>
      <div class="app-campo">
        <label for="docFecha">Fecha de la prueba</label>
        <input class="app-input" id="docFecha" placeholder="12/03/2024">
      </div>
    </div>
    <div class="app-campo" style="margin-top:12px">
      <label for="docNotas">Notas</label>
      <textarea class="app-textarea" id="docNotas" rows="2" placeholder="Qué pidió el médico, qué concluyeron…"></textarea>
    </div>
    <button class="app-boton app-boton--ancho" id="guardarDoc" style="margin-top:16px">Guardar</button>
  `, (hoja) => {
    hoja.querySelector("#guardarDoc").addEventListener("click", () => {
      const nombre = hoja.querySelector("#docNombre").value.trim();
      if (!nombre) { DG.snack("Ponle un nombre al informe."); return; }
      DG.estado.documentos.push({
        id: DG.uid(),
        nombre,
        tipo: hoja.querySelector("#docTipo").value,
        fecha: hoja.querySelector("#docFecha").value.trim() || "Sin fecha",
        notas: hoja.querySelector("#docNotas").value.trim(),
        tamano: (Math.round(Math.random() * 1800) / 1000).toFixed(1).replace(".", ",") + " MB",
        formato: "PDF",
      });
      DG.guardar();
      DG.cerrarHoja();
      DG.renderPerfil();
      DG.snack("Informe adjuntado.");
    });
  });
};

DG.hojaCirugia = () => {
  const tipos = ["Resección ileocecal", "Resección ileal", "Hemicolectomía", "Colectomía", "Ileostomía (ostomía de íleon)", "Colostomía (ostomía de colon)", "Drenaje de absceso", "Estenoplastia / dilatación", "Otras"];
  DG.abrirHoja(`
    ${DG.tituloHoja("Añadir intervención")}
    <div class="app-campo">
      <label for="cirTipo">Tipo de intervención</label>
      <select class="app-select" id="cirTipo">${tipos.map((t) => `<option>${t}</option>`).join("")}</select>
    </div>
    <div class="app-campo" style="margin-top:12px">
      <label for="cirFecha">Año</label>
      <input class="app-input" id="cirFecha" placeholder="2022" inputmode="numeric">
    </div>
    <button class="app-boton app-boton--ancho" id="guardarCir" style="margin-top:16px">Guardar</button>
  `, (hoja) => {
    hoja.querySelector("#guardarCir").addEventListener("click", () => {
      const tipo = hoja.querySelector("#cirTipo").value;
      DG.estado.cirugias.push({ id: DG.uid(), tipo, fecha: hoja.querySelector("#cirFecha").value.trim() });
      if (tipo.includes("ostomía")) DG.estado.perfil.ostomia = true;
      DG.guardar();
      DG.cerrarHoja();
      DG.renderPerfil();
      DG.snack("Intervención registrada.");
    });
  });
};

DG.hojaCondicion = () => {
  DG.abrirHoja(`
    ${DG.tituloHoja("Cambiar diagnóstico")}
    <p class="app-ayuda" style="margin-top:0">La condición decide qué zonas se preguntan, qué síntomas se ofrecen y cómo se llama el control diario.</p>
    <div style="display:grid;gap:8px;margin-top:10px">
      ${Object.entries(DG.CONDICIONES).map(([id, c]) => `
        <button type="button" class="app-opcion ${DG.estado.perfil.condicion === id ? "activo" : ""}" data-nueva="${id}" style="flex-direction:column;align-items:flex-start;gap:3px">
          <strong>${c.nombre}</strong><small>${c.desc}</small>
        </button>`).join("")}
    </div>
  `, (hoja) => {
    hoja.querySelectorAll("[data-nueva]").forEach((b) => b.addEventListener("click", () => {
      const nueva = b.dataset.nueva;
      const p = DG.estado.perfil;
      p.condicion = nueva;
      // Al cambiar de diagnóstico no se arrastran datos de la anterior.
      if (!DG.CONDICIONES[nueva].inflamatoria) { p.afectaciones = []; p.ostomia = false; }
      DG.estado.condicionPrevia = nueva;
      DG.guardar();
      DG.cerrarHoja();
      DG.renderPerfil();
      DG.notaDiagnostico();
      DG.snack(`Diagnóstico cambiado a ${DG.CONDICIONES[nueva].nombre}.`);
    }));
  });
};

DG.hojaDirectorio = () => {
  const pinta = (filtro = "") => {
    const q = filtro.toLowerCase();
    const lista = DG.PROFESIONALES.filter((p) =>
      !q || [p.nombre, p.centro, p.ciudad, p.esp].some((x) => x.toLowerCase().includes(q)));
    return lista.length
      ? lista.map((p) => `
        <div class="app-tarjeta">
          <div class="app-fila">
            <div class="app-columna">
              <strong>${p.nombre}</strong>
              <span>${p.esp} · ${p.centro}</span>
              <span>${p.ciudad}${p.online ? " · admite cita online" : ""}</span>
            </div>
            <button class="app-boton app-boton--tonal js-llamar" data-tel="${p.tel}">Llamar</button>
          </div>
          ${p.eii ? `<span class="app-estado estado-tomada">Especializado en EII</span>` : ""}
        </div>`).join("")
      : `<div class="app-vacio"><strong>No hay profesionales que coincidan.</strong></div>`;
  };

  DG.abrirHoja(`
    ${DG.tituloHoja("Profesionales")}
    <div class="app-banner app-banner--aviso">
      <span>!</span>
      <span>Directorio de ejemplo. Los nombres, teléfonos y centros <strong>no son reales</strong>: sirven para probar la app.</span>
    </div>
    <input class="app-input" id="buscarProf" placeholder="Buscar por nombre, centro o ciudad" style="margin-top:12px">
    <div class="app-lista" id="listaProf" style="margin-top:12px">${pinta()}</div>
  `, (hoja) => {
    const lista = hoja.querySelector("#listaProf");
    const enlazar = () => lista.querySelectorAll(".js-llamar").forEach((b) => b.addEventListener("click", () => {
      DG.snack(`En la app real: llamar al ${b.dataset.tel}`);
    }));
    enlazar();
    hoja.querySelector("#buscarProf").addEventListener("input", (ev) => {
      lista.innerHTML = pinta(ev.target.value);
      enlazar();
    });
  });
};

DG.hojaInforme = (dias = 7, formato = "texto") => {
  const texto = DG.generarInforme(DG.estado, formato, dias);
  DG.marcarPaso("informe");
  DG.abrirHoja(`
    ${DG.tituloHoja("Informe para tu médico")}
    <div class="app-fila" style="margin-bottom:10px">
      <div class="app-segsel" id="infDias">
        ${[7, 15, 30].map((n) => `<button type="button" class="${n === dias ? "activo" : ""}" data-dias="${n}">${n} días</button>`).join("")}
      </div>
      <div class="app-segsel" id="infFmt">
        ${["texto", "csv"].map((f) => `<button type="button" class="${f === formato ? "activo" : ""}" data-fmt="${f}">${f === "texto" ? "Texto" : "CSV"}</button>`).join("")}
      </div>
    </div>
    <pre class="app-informe" id="salidaInforme">${texto}</pre>
    <div class="app-fila" style="margin-top:14px">
      <button class="app-boton app-boton--fantasma" id="copiarInf">Copiar</button>
      <button class="app-boton app-boton--tonal" id="descargarInf">Descargar</button>
      <button class="app-boton" id="compartirInf">Compartir</button>
    </div>
    <p class="app-ayuda">En la app real esto abre el menú de compartir de Android con el resumen y los informes adjuntos.</p>
  `, (hoja) => {
    let d = dias, f = formato;
    const regenerar = () => {
      hoja.querySelector("#salidaInforme").textContent = DG.generarInforme(DG.estado, f, d);
      DG.marcarPaso("informe");
    };
    hoja.querySelectorAll("#infDias button").forEach((b) => b.addEventListener("click", () => {
      d = Number(b.dataset.dias);
      hoja.querySelectorAll("#infDias button").forEach((x) => x.classList.toggle("activo", x === b));
      regenerar();
    }));
    hoja.querySelectorAll("#infFmt button").forEach((b) => b.addEventListener("click", () => {
      f = b.dataset.fmt;
      hoja.querySelectorAll("#infFmt button").forEach((x) => x.classList.toggle("activo", x === b));
      regenerar();
    }));
    hoja.querySelector("#copiarInf").addEventListener("click", async () => {
      try { await navigator.clipboard.writeText(hoja.querySelector("#salidaInforme").textContent); DG.snack("Informe copiado."); }
      catch (_) { DG.snack("No se pudo copiar en este navegador."); }
    });
    hoja.querySelector("#descargarInf").addEventListener("click", () => {
      const blob = new Blob([hoja.querySelector("#salidaInforme").textContent], { type: "text/plain;charset=utf-8" });
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = f === "csv" ? "digest-sintomas.csv" : "digest-informe.txt";
      a.click();
      URL.revokeObjectURL(url);
    });
    hoja.querySelector("#compartirInf").addEventListener("click", () => {
      const docs = DG.estado.documentos.length;
      DG.cerrarHoja();
      DG.snack(docs
        ? `Historia preparada: resumen de ${d} días + ${docs} informe(s) adjunto(s).`
        : "Sin informes adjuntos: se manda solo el resumen.");
    });
  });
};

DG.hojaHistoria = () => {
  const docs = DG.estado.documentos.length;
  DG.abrirHoja(`
    ${DG.tituloHoja("Compartir mi historia con el médico")}
    <p class="app-ayuda" style="margin-top:0">Se mandan juntos el resumen de tus síntomas y todos los informes que hayas adjuntado. Eliges el periodo y el destino.</p>
    <div class="app-segsel" id="histDias" style="margin-top:10px">
      <button type="button" class="activo" data-dias="7">7 días</button>
      <button type="button" data-dias="15">15 días</button>
      <button type="button" data-dias="30">30 días</button>
    </div>
    <div class="app-lista" style="margin-top:14px">
      <div class="app-linea"><span>Resumen escrito</span><span>siempre</span></div>
      <div class="app-linea"><span>Informes adjuntos</span><span>${docs ? docs + " archivo(s)" : "ninguno"}</span></div>
    </div>
    ${docs ? "" : `<div class="app-banner app-banner--aviso" style="margin-top:12px"><span>!</span><span>No has adjuntado ningún informe, así que se manda solo el resumen.</span></div>`}
    <button class="app-boton app-boton--ancho" id="enviarHistoria" style="margin-top:16px">Compartir con el médico</button>
  `, (hoja) => {
    let d = 7;
    hoja.querySelectorAll("#histDias button").forEach((b) => b.addEventListener("click", () => {
      d = Number(b.dataset.dias);
      hoja.querySelectorAll("#histDias button").forEach((x) => x.classList.toggle("activo", x === b));
    }));
    hoja.querySelector("#enviarHistoria").addEventListener("click", () => {
      DG.cerrarHoja();
      setTimeout(() => DG.hojaInforme(d, "texto"), 220);
      DG.snack("Preparando tu historia…");
    });
  });
};

/* ========================= Avisos simulados ============================ */

DG.mostrarAviso = ({ titulo, cuerpo, tipo, tomaId, comidaId }) => {
  const aviso = document.getElementById("movilAviso");
  document.getElementById("avisoTitulo").textContent = titulo;
  document.getElementById("avisoCuerpo").textContent = cuerpo;
  aviso.dataset.tipo = tipo || "";
  if (tomaId) aviso.dataset.toma = tomaId; else delete aviso.dataset.toma;
  if (comidaId) aviso.dataset.comida = comidaId; else delete aviso.dataset.comida;
  aviso.querySelector('[data-aviso="tomada"]').textContent = tipo === "control" ? "Sí, he notado cambios" : "Tomada";
  aviso.querySelector('[data-aviso="omitida"]').textContent = tipo === "control" ? "No, nada nuevo" : "Omitir";
  aviso.classList.add("visible");
};

DG.ocultarAviso = () => document.getElementById("movilAviso").classList.remove("visible");

/* ============================== Arranque =============================== */

DG.notaDiagnostico = () => {
  const el = document.getElementById("notaDiagnostico");
  if (el) el.textContent = DG.CONDICIONES[DG.estado.condicionPrevia].nota;
};

DG.arrancar = () => {
  // Reloj del teléfono
  const pintarHora = () => {
    const el = document.getElementById("movilHora");
    if (el) el.textContent = DG.horaAhora();
  };
  pintarHora();
  setInterval(pintarHora, 30000);

  // Navegación de la demo
  document.querySelectorAll("#appNav button").forEach((b) => {
    b.addEventListener("click", () => DG.irA(b.dataset.seccion));
  });

  // Notificación simulada
  const aviso = document.getElementById("movilAviso");
  document.getElementById("avisoCerrar").addEventListener("click", DG.ocultarAviso);
  aviso.querySelectorAll("[data-aviso]").forEach((b) => {
    b.addEventListener("click", () => {
      const respuesta = b.dataset.aviso;
      if (aviso.dataset.tipo === "control") {
        const c = DG.hoy().comidas.find((x) => x.id === aviso.dataset.comida);
        if (c) { c.control = respuesta === "tomada" ? "si" : "sin"; c.pendiente = false; }
        DG.marcarPaso("comida");
        DG.irA("hoy");
        DG.snack(respuesta === "tomada" ? "Guardado. Revisa tus síntomas de hoy." : "Guardado. Sin cambios.");
      } else {
        const t = DG.hoy().tomas.find((x) => x.id === aviso.dataset.toma);
        if (t) t.estado = respuesta === "tomada" ? "tomada" : "omitida";
        DG.marcarPaso("toma");
        DG.irA(DG.estado.seccion);
        DG.snack(respuesta === "tomada" ? "Toma registrada desde el aviso." : "Toma omitida desde el aviso.");
      }
      DG.guardar();
      DG.ocultarAviso();
    });
  });

  // Panel de control de la web
  document.querySelectorAll("#chipsDiagnostico button").forEach((b) => {
    b.addEventListener("click", () => {
      document.querySelectorAll("#chipsDiagnostico button").forEach((x) => x.classList.toggle("activo", x === b));
      DG.estado.condicionPrevia = b.dataset.condicion;
      if (DG.estado.perfil) {
        const p = DG.estado.perfil;
        p.condicion = b.dataset.condicion;
        if (!DG.CONDICIONES[p.condicion].inflamatoria) { p.afectaciones = []; p.ostomia = false; }
        DG.marcarPaso("alta");
        DG.snack(`Diagnóstico cambiado a ${DG.CONDICIONES[p.condicion].nombre}.`);
      } else {
        DG.renderAlta();
      }
      DG.guardar();
      DG.notaDiagnostico();
      DG.pintar();
    });
  });

  document.getElementById("btnSemilla").addEventListener("click", () => {
    const cond = DG.estado.perfil ? DG.estado.perfil.condicion : DG.estado.condicionPrevia;
    DG.estado = DG.crearSemilla(cond);
    DG.estado.seccion = "hoy";
    DG.estado.pasos = { alta: true, toma: true, sintoma: true, comida: true };
    DG.guardar();
    DG.notaDiagnostico();
    DG.pintar();
    DG.snack("7 días de datos de ejemplo cargados.");
  });

  document.getElementById("btnReiniciar").addEventListener("click", () => {
    if (!window.confirm("¿Borrar todo lo registrado en la demo?")) return;
    DG.reiniciarEstado();
    document.querySelectorAll("#chipsDiagnostico button").forEach((x) => {
      x.classList.toggle("activo", x.dataset.condicion === "colitis_ulcerosa");
    });
    DG.notaDiagnostico();
    DG.pintar();
    DG.snack("Demo reiniciada.");
  });

  document.getElementById("btnAviso").addEventListener("click", () => {
    if (!DG.estado.perfil) { DG.snack("Primero completa el alta."); return; }
    const pendiente = DG.hoy().tomas.find((t) => t.estado === "pendiente");
    if (!pendiente) {
      const med = DG.estado.medicamentos[0];
      DG.snack(med ? "Ya has tomado todo lo de hoy." : "No hay medicación programada.");
      return;
    }
    DG.mostrarAviso({
      titulo: `Toma de ${pendiente.nombre}`,
      cuerpo: `Te toca tomarlo a las ${pendiente.hora}${pendiente.dosis ? " · " + pendiente.dosis : ""}`,
      tipo: "toma",
      tomaId: pendiente.id,
    });
  });

  // Primer pintado
  DG.notaDiagnostico();
  DG.pintar();

  // API para la web
  window.DigestDemo = {
    senales: () => DG.senales(),
    informe: (formato, dias) => {
      const est = DG.estado;
      const hayDatos = Object.values(est.dias).some((d) =>
        Object.keys(d.sintomas).length || d.deposiciones.length || d.comidas.length);
      return hayDatos && est.perfil
        ? DG.generarInforme(est, formato, dias)
        : DG.generarInforme(DG.crearSemilla("colitis_ulcerosa"), formato, dias);
    },
    reiniciar: () => { DG.reiniciarEstado(); DG.pintar(); },
  };
};

document.addEventListener("DOMContentLoaded", DG.arrancar);
