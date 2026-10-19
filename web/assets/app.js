/* =========================================================================
   Digest · interacciones de la página
   Navegación, tema, animaciones de entrada y secciones informativas.
   La demo del móvil vive en demo.js y se conecta aquí a través de
   window.DigestDemo.
   ========================================================================= */

(() => {
  "use strict";

  const $ = (sel, ctx = document) => ctx.querySelector(sel);
  const $$ = (sel, ctx = document) => Array.from(ctx.querySelectorAll(sel));

  /* --------------------------- Descarga del APK --------------------------
     El APK vive como archivo adjunto de una release de GitHub, así que no
     se versiona en el repositorio (el .gitignore ignora los *.apk) y no
     depende de permisos ni cuotas de terceros. Para publicar una versión
     nueva: Releases → Draft a new release → etiqueta vX.Y → adjuntar el APK.
     Cambia solo el número de versión de esta constante al actualizar. */
  const APK_URL = "https://github.com/DavidBordalecu/digest/releases/download/v1.7/Digest-v1.7.apk";

  const prepararDescargas = () => {
    $$("[data-apk]").forEach((a) => {
      if (APK_URL) {
        a.href = APK_URL;
        a.removeAttribute("download");
        a.target = "_blank";
        a.rel = "noopener noreferrer";
      } else {
        const nota = document.createElement("small");
        nota.className = "apk-pendiente";
        nota.textContent = "Descarga disponible en breve";
        a.insertAdjacentElement("afterend", nota);
      }
    });
  };
  prepararDescargas();

  /* ------------------------------- Tema -------------------------------- */

  const raiz = document.documentElement;
  const guardarTema = (tema) => {
    raiz.dataset.tema = tema;
    try { localStorage.setItem("digest-web-tema", tema); } catch (_) {}
  };

  try {
    const previo = localStorage.getItem("digest-web-tema");
    if (previo) raiz.dataset.tema = previo;
    else if (window.matchMedia("(prefers-color-scheme: light)").matches) raiz.dataset.tema = "claro";
  } catch (_) {}

  $("#btnTema")?.addEventListener("click", () => {
    guardarTema(raiz.dataset.tema === "oscuro" ? "claro" : "oscuro");
  });

  /* -------------------------- Nav: estado ------------------------------ */

  const nav = $("#nav");
  const barra = $("#barraProgreso span");
  const enlacesNav = $$("#navEnlaces a");

  const alScroll = () => {
    const y = window.scrollY;
    nav.classList.toggle("fijo", y > 20);

    const alto = document.documentElement.scrollHeight - window.innerHeight;
    barra.style.width = `${alto > 0 ? (y / alto) * 100 : 0}%`;

    // Parallax de los orbes del hero
    $$("[data-parallax]").forEach((el) => {
      const f = parseFloat(el.dataset.parallax);
      el.style.translate = `0 ${y * f}px`;
    });
  };
  window.addEventListener("scroll", alScroll, { passive: true });
  alScroll();

  const btnMenu = $("#btnMenu");
  btnMenu?.addEventListener("click", () => {
    const abierta = $("#navEnlaces").classList.toggle("abierto");
    btnMenu.setAttribute("aria-expanded", String(abierta));
  });
  enlacesNav.forEach((a) => a.addEventListener("click", () => {
    $("#navEnlaces").classList.remove("abierto");
    btnMenu?.setAttribute("aria-expanded", "false");
  }));

  // Sección activa en la navegación
  const HAY_OBS = typeof IntersectionObserver === "function";
  const secciones = $$("main section[id]");
  const observadorSeccion = HAY_OBS ? new IntersectionObserver((entradas) => {
    entradas.forEach((e) => {
      if (!e.isIntersecting) return;
      enlacesNav.forEach((a) => {
        a.classList.toggle("activo", a.getAttribute("href") === `#${e.target.id}`);
      });
    });
  }, { rootMargin: "-45% 0px -50% 0px" }) : null;
  if (observadorSeccion) secciones.forEach((s) => observadorSeccion.observe(s));

  /* ------------------------- Reveal y contadores ------------------------ */

  const reveal = HAY_OBS ? new IntersectionObserver((entradas) => {
    entradas.forEach((e) => {
      if (!e.isIntersecting) return;
      e.target.classList.add("visible");
      reveal.unobserve(e.target);
    });
  }, { threshold: 0.12, rootMargin: "0px 0px -40px 0px" }) : null;
  $$(".reveal").forEach((el) => {
    if (reveal) reveal.observe(el);
    else el.classList.add("visible");
  });

  const animarContador = (el) => {
    const meta = parseFloat(el.dataset.contador) || 0;
    const sufijo = el.dataset.suffix || "";
    const dur = 1300;
    const inicio = performance.now();
    const paso = (t) => {
      const p = Math.min(1, (t - inicio) / dur);
      const eased = 1 - Math.pow(1 - p, 3);
      const valor = Math.round(meta * eased);
      el.textContent = valor + sufijo;
      if (p < 1) requestAnimationFrame(paso);
      else el.textContent = meta + sufijo;
    };
    requestAnimationFrame(paso);
  };

  const contadores = HAY_OBS ? new IntersectionObserver((entradas) => {
    entradas.forEach((e) => {
      if (!e.isIntersecting) return;
      animarContador(e.target);
      contadores.unobserve(e.target);
    });
  }, { threshold: 0.6 }) : null;
  $$("[data-contador]").forEach((el) => {
    if (contadores) contadores.observe(el);
    else el.textContent = el.dataset.contador + (el.dataset.suffix || "");
  });

  /* --------------------- Brillo que sigue al cursor -------------------- */

  const moverBrillo = (ev) => {
    const caja = ev.currentTarget.getBoundingClientRect();
    ev.currentTarget.style.setProperty("--mx", `${ev.clientX - caja.left}px`);
    ev.currentTarget.style.setProperty("--my", `${ev.clientY - caja.top}px`);
  };
  $$("[data-tilt], .tarjeta, .principio").forEach((el) => el.addEventListener("pointermove", moverBrillo));

  /* -------------------------- Botones magnéticos ----------------------- */

  const ratonFino = typeof window.matchMedia === "function" && window.matchMedia("(pointer: fine)").matches;
  if (ratonFino) {
    $$(".magnetico").forEach((btn) => {
      btn.addEventListener("pointermove", (ev) => {
        const caja = btn.getBoundingClientRect();
        const x = (ev.clientX - caja.left - caja.width / 2) * 0.18;
        const y = (ev.clientY - caja.top - caja.height / 2) * 0.3;
        btn.style.transform = `translate(${x}px, ${y - 2}px)`;
      });
      btn.addEventListener("pointerleave", () => { btn.style.transform = ""; });
    });
  }

  /* --------------------------- Ripple en el móvil ----------------------- */

  const pantallaMovil = $(".movil__pantalla");
  const anillo = $("#movilTouch");
  pantallaMovil?.addEventListener("pointerdown", (ev) => {
    if (!anillo) return;
    const caja = pantallaMovil.getBoundingClientRect();
    anillo.style.left = `${ev.clientX - caja.left}px`;
    anillo.style.top = `${ev.clientY - caja.top}px`;
    anillo.classList.remove("activo");
    void anillo.offsetWidth;      // reinicia la animación
    anillo.classList.add("activo");
  });

  /* ========================== Funciones (tabs) ========================== */

  const FUNCIONES = {
    hoy: {
      titulo: "Hoy: lo que hay que hacer ahora mismo",
      texto: "Es la pantalla de entrada y reúne lo accionable del día. El orden está pensado para alguien que pasa por aquí varias veces al día: primero la toma, después lo ya hecho y al final el registro del día.",
      puntos: [
        "Fecha y diagnóstico del perfil, con aviso si faltan permisos de notificación o de alarma exacta.",
        "Tomas de hoy pendientes, tomadas u omitidas, con acción directa desde la propia tarjeta.",
        "Cuestionario de síntomas adaptado al diagnóstico: de 1 a 5 por toque y se quita en el máximo.",
        "Control de la comida dos horas después: si el paciente ha notado algo, marcarlo justo debajo.",
        "Señales clínicas y, al final, «Compartir mi historia con el médico».",
      ],
      mini: [
        { t: "Ahora te toca", d: "Mesalazina 1 g · 08:00", c: ["próxima toma", "refuerzo +20 min"] },
        { t: "¿Cómo estás hoy?", d: "16 síntomas según diagnóstico", c: ["1 → 5", "1 toque = marcar"] },
        { t: "Señales de alerta", d: "Explican qué se ha detectado", c: ["urgente", "pronto", "normal"] },
      ],
    },
    medicacion: {
      titulo: "Medicación: la agenda de fármacos y suplementos",
      texto: "Cada medicación se crea una vez y a partir de ahí genera sus tomas del día. Los avisos van por AlarmManager, con acciones directas en la notificación.",
      puntos: [
        "Nombre, dosis, presentación e instrucciones de uso («con comida», «en ayunas»).",
        "Pauta con varios horarios al día y días concretos de la semana.",
        "Aviso a la hora exacta con acciones Tomada y Omitir, y refuerzo a los 20 minutos si sigue pendiente.",
        "En Android 12+ usa alarma exacta si hay permiso; si no, degrada a alarma inexacta en vez de dejar de avisar.",
        "Las alarmas se reprograman al abrir la app y tras cada reinicio del teléfono.",
      ],
      mini: [
        { t: "Mesalazina 1 g", d: "08:00 · 14:00 · 20:00 · con comida", c: ["aminosalicilato", "oral"] },
        { t: "Vitamina D 1000 UI", d: "09:00 · todos los días", c: ["suplemento"] },
        { t: "Estado de la toma", d: "Pendiente → Tomada / Omitida", c: ["deshacer"] },
      ],
    },
    comidas: {
      titulo: "Comidas: qué se comió y qué pasó después",
      texto: "Registrar la comida es la mitad; la otra mitad es la pregunta de dos horas después, que es la que construye la relación entre alimentos y síntomas que el paciente no sabe atribuir por sí solo.",
      puntos: [
        "Tipo de comida y hora, más lo que se ha comido, no solo la categoría.",
        "Las comidas anteriores se ofrecen para repetirlas con un toque.",
        "Dos horas después, la app pregunta si ha habido algún cambio.",
        "Las respuestas se cruzan con la comida registrada para el informe.",
      ],
      mini: [
        { t: "Almuerzo · 13:45", d: "Pollo con arroz y zanahoria", c: ["repetir con 1 toque"] },
        { t: "Control · 15:45", d: "¿Has notado algún cambio?", c: ["Sí, he notado cambios", "No, nada nuevo"] },
        { t: "Hoy", d: "Comidas controladas: 2 de 3 con cambios", c: ["cruce alimento → síntoma"] },
      ],
    },
    diario: {
      titulo: "Diario: el cuerpo y el estado del día",
      texto: "Aquí se anota lo que el médico revisa para ver la evolución: deposiciones con escala de Bristol, dolor, nutrición enteral y, al final, el periodo para el informe.",
      puntos: [
        "Deposiciones con escala de Bristol y cantidad de sangre; episodios de dolor con intensidad, tipo y localización.",
        "Mejora con la medicación, sensación general, hidratación y notas del día.",
        "«Estado del brote» en EII, o «Control de los síntomas» en colon irritable.",
        "Informe en texto o CSV de 7, 15 o 30 días, generado en el propio teléfono.",
      ],
      mini: [
        { t: "Deposición", d: "Bristol 6 · sangre ligera", c: ["1–7", "ninguna → abundante"] },
        { t: "Estado del brote", d: "Remisión · leve · moderado · severo", c: ["o «Control de los síntomas»"] },
        { t: "Informe", d: "Texto o CSV · 7, 15 o 30 días", c: ["adherencia", "tendencias", "alertas"] },
      ],
    },
    perfil: {
      titulo: "Perfil, documentos y profesionales",
      texto: "La ficha del paciente y la carpeta de informes. Los archivos se copian dentro de la app, no se guardan como referencia: siguen disponibles aunque se borre el original de Descargas.",
      puntos: [
        "Diagnóstico, zonas afectadas, peso, altura e IMC, intervalo entre consultas y antecedentes quirúrgicos.",
        "Historial de cirugías y fecha de la última prueba de cada tipo.",
        "Documentos con nombre, tipo, fecha, notas, tamaño y formato: abrir, compartir o borrar.",
        "Directorio de profesionales de gastroenterología y nutrición, priorizando especialistas en EII (datos de ejemplo).",
      ],
      mini: [
        { t: "Colonoscopia 2024", d: "PDF · 1,2 MB · 12 mar 2024", c: ["compartir", "borrar"] },
        { t: "IMC y medidas", d: "Peso, altura e IMC calculado", c: ["guardar"] },
        { t: "Última consulta", d: "hace 140 días · cada 6 meses", c: ["toca revisión al 75%"] },
      ],
    },
  };

  const panelFunciones = $("#funcionesPanel");
  const pintarFuncion = (clave) => {
    const f = FUNCIONES[clave];
    if (!f || !panelFunciones) return;
    panelFunciones.innerHTML = `
      <div class="funcion">
        <div>
          <h3>${f.titulo}</h3>
          <p>${f.texto}</p>
          <ul>${f.puntos.map((p) => `<li>${p}</li>`).join("")}</ul>
        </div>
        <div class="mini">
          <div class="mini__cab"><span>Vista previa</span><span>v1.7</span></div>
          ${f.mini.map((m) => `
            <div class="mini__tarjeta">
              <strong>${m.t}</strong>
              <span>${m.d}</span>
              <div class="mini__chips">${m.c.map((c) => `<i>${c}</i>`).join("")}</div>
            </div>`).join("")}
        </div>
      </div>`;
  };

  $$("#funcionesTabs button").forEach((btn) => {
    btn.addEventListener("click", () => {
      $$("#funcionesTabs button").forEach((b) => b.classList.toggle("activo", b === btn));
      pintarFuncion(btn.dataset.funcion);
    });
  });
  pintarFuncion("hoy");

  /* ========================== Diagnósticos ============================== */

  const DIAG_DETALLE = {
    colitis_ulcerosa: {
      zonas: ["Proctitis", "Colitis izquierda", "Colitis extensa", "Pancolitis"],
      sintomas: ["Diarrea", "Dolor abdominal", "Tenesmo", "Sangre en heces", "Mucosidad", "Distensión", "Fatiga", "Fiebre", "Náuseas", "Pérdida de apetito", "Pérdida de peso", "Desnutrición", "Aftas", "Dolor articular", "Manchas en la piel", "Ojo rojo"],
      control: "Estado del brote: En remisión · Brote leve · Moderado · Severo · No me aplica",
      nota: "Inflamación continua del colon y del recto. El cuestionario insiste en el sangrado, el tenesmo y la mucosidad, que son los signos típicos del colón izquierdo.",
    },
    crohn: {
      zonas: ["Íleítis (íleon)", "Íleo-colítica", "Colitis de Crohn", "Gastroduodenal", "Perianal"],
      sintomas: ["Todo lo de la colitis ulcerosa", "Fisura anal", "Fístula anal", "Absceso perianal", "Dolor o supuración perianal", "Inflamación externa perianal"],
      control: "Estado del brote: En remisión · Brote leve · Moderado · Severo · No me aplica",
      nota: "Puede afectar a cualquier parte del tubo digestivo y con frecuencia de forma discontinua, por eso incluye el íleon y las manifestaciones perianales.",
    },
    indeterminada: {
      zonas: ["Todas las anteriores", "(la más amplia de las dos anteriores)"],
      sintomas: ["Lista completa de Crohn", "Perianales incluidas"],
      control: "Estado del brote: En remisión · Brote leve · Moderado · Severo · No me aplica",
      nota: "Inflamación del colon que no encaja con claridad en colitis ulcerosa ni en Crohn: recibe el cuestionario más amplio de los dos.",
    },
    colon_irritable: {
      zonas: ["No se preguntan zonas", "Ni antecedentes quirúrgicos"],
      sintomas: ["Diarrea", "Dolor abdominal", "Mucosidad", "Distensión", "Fatiga", "Náuseas"],
      control: "Control de los síntomas: Sin molestias · Leves · Moderadas · Intensas · No me aplica",
      nota: "Trastorno funcional sin inflamación. Se excluyen sangre, fiebre, pérdida de peso y manifestaciones extraintestinales: no forman parte del cuadro y obligarían a descartar otra causa.",
    },
  };

  const pintarDiagnostico = (clave) => {
    const d = DIAG_DETALLE[clave];
    if (!d) return;

    $$("#diags [data-col]").forEach((c) => c.classList.toggle("col-activa", c.dataset.col === clave));
    $$("#diagsSel button").forEach((b) => b.classList.toggle("activo", b.dataset.diag === clave));

    const cont = $("#diagsDetalle");
    cont.innerHTML = `
      <div>
        <h4>Zonas en el alta</h4>
        <ul>${d.zonas.map((z) => `<li>${z}</li>`).join("")}</ul>
      </div>
      <div>
        <h4>Cuestionario de síntomas</h4>
        <ul>${d.sintomas.slice(0, 8).map((s) => `<li>${s}</li>`).join("")}</ul>
        ${d.sintomas.length > 8 ? `<p style="margin-top:8px">+ ${d.sintomas.length - 8} más…</p>` : ""}
      </div>
      <div>
        <h4>Bloque de control</h4>
        <p>${d.control}</p>
        <h4 style="margin-top:16px">Cómo se comporta</h4>
        <p>${d.nota}</p>
      </div>`;
  };

  $$("#diagsSel button").forEach((b) => b.addEventListener("click", () => pintarDiagnostico(b.dataset.diag)));
  pintarDiagnostico("colitis_ulcerosa");

  /* ============================ Alertas ================================= */

  const REGLAS = [
    { nivel: "urgente", nombre: "Sangrado abundante", cuando: "Sangre abundante en heces registrada hoy.", detalle: "Se evalúa en cada deposición registrada. La app avisa en cuanto aparece «abundante» y explica que conviene contactar hoy con el centro de salud." },
    { nivel: "urgente", nombre: "Fiebre alta", cuando: "Fiebre marcada con intensidad 4 o más sobre 5.", detalle: "En colon irritable la explicación cambia: la fiebre no es habitual en este cuadro y por eso obliga a descartar otra causa." },
    { nivel: "urgente", nombre: "Dolor abdominal intenso", cuando: "Dolor marcado con intensidad 4 o más sobre 5.", detalle: "Se evalúa con el síntoma del día. Si además hay dolor fijo que no cede, conviene mencionarlo en la consulta." },
    { nivel: "urgente", nombre: "Posible absceso o fístula perianal", cuando: "Manifestación perianal nueva o absceso moderado en Crohn o colitis indeterminada.", detalle: "Solo aplica en Crohn e indeterminada, que son los diagnósticos donde las manifestaciones perianales se registran." },
    { nivel: "urgente", nombre: "Muchas deposiciones diarreicas", cuando: "8 o más deposiciones diarreicas en el día.", detalle: "Cuenta las deposiciones con Bristol 6 o 7 registradas desde el inicio del día." },
    { nivel: "pronto", nombre: "Síntoma que se mantiene", cuando: "Un síntoma lleva 14 días seguidos o más.", detalle: "Necesita histórico: la app mira los días consecutivos en los que el mismo síntoma aparece marcado." },
    { nivel: "pronto", nombre: "Pérdida de peso relevante", cuando: "Más del 5 % del peso en tres meses.", detalle: "Compara el peso registrado hoy con el de hace tres meses, si existe." },
    { nivel: "pronto", nombre: "Varios síntomas a la vez", cuando: "3 o más síntomas con uno moderado, 5 o más, o sensación general mala.", detalle: "Mira el día completo: intensidad, cantidad de síntomas marcados y la respuesta de «¿Cómo te sientes en general?»." },
    { nivel: "pronto", nombre: "Toca revisión", cuando: "Ha pasado el 75 % del intervalo habitual entre consultas.", detalle: "El intervalo se define en el perfil (por defecto, 6 meses). El aviso aparece antes de que se pase la fecha." },
  ];

  const contReglas = $("#reglas");
  if (contReglas) {
    contReglas.innerHTML = REGLAS.map((r) => `
      <button class="regla" data-nivel="${r.nivel}" type="button">
        <span class="regla__nivel">${r.nivel === "urgente" ? "Urgente" : "Pronto"}</span>
        <strong>${r.nombre}</strong>
        <p>${r.cuando}</p>
        <span class="regla__mas">Por qué salta →</span>
        <span class="regla__detalle">${r.detalle}</span>
      </button>`).join("");

    contReglas.addEventListener("click", (ev) => {
      const regla = ev.target.closest(".regla");
      if (regla) regla.classList.toggle("abierta");
    });
  }

  $("#btnSenalesDemo")?.addEventListener("click", () => {
    const caja = $("#tarjetaSenalesDemo");
    const demo = window.DigestDemo;
    if (!demo) { caja.innerHTML = "<p>La demo aún no está lista.</p>"; return; }

    const senales = demo.senales();
    if (!senales.length) {
      caja.innerHTML = `<span class="punto punto--verde"></span>
        <p><strong>Sin señales de alerta.</strong> Con los datos actuales no se detecta nada que deba adelantar la consulta.</p>`;
      return;
    }
    caja.innerHTML = senales.map((s) => `
      <div class="senal senal--${s.nivel}">
        <span class="senal__ico">${s.nivel === "urgente" ? "!!" : "i"}</span>
        <span><strong>${s.titulo}</strong>${s.motivo}</span>
      </div>`).join("");
  });

  /* ============================= Informe ================================ */

  let formatoInforme = "texto";
  let diasInforme = 7;

  $$("#informeFormato button").forEach((b) => b.addEventListener("click", () => {
    $$("#informeFormato button").forEach((x) => x.classList.toggle("activo", x === b));
    formatoInforme = b.dataset.formato;
  }));
  $$("#informePeriodo button").forEach((b) => b.addEventListener("click", () => {
    $$("#informePeriodo button").forEach((x) => x.classList.toggle("activo", x === b));
    diasInforme = Number(b.dataset.dias);
  }));

  const textoInforme = () => $("#informeSalida")?.textContent || "";

  $("#btnGenerarInforme")?.addEventListener("click", async () => {
    const salida = $("#informeSalida");
    salida.textContent = "Generando informe…";
    const demo = window.DigestDemo;
    await new Promise((r) => setTimeout(r, 420));
    const texto = demo
      ? demo.informe(formatoInforme, diasInforme)
      : "La demo no está disponible en este navegador.";
    salida.textContent = texto;
    salida.scrollTop = 0;
  });

  const copiar = async (texto, ok) => {
    try {
      await navigator.clipboard.writeText(texto);
      ok("Copiado ✓");
    } catch (_) {
      ok("No se pudo copiar");
    }
  };

  $("#btnCopiarInforme")?.addEventListener("click", (ev) => {
    const btn = ev.currentTarget;
    const original = btn.textContent;
    copiar(textoInforme(), (m) => {
      btn.textContent = m;
      setTimeout(() => { btn.textContent = original; }, 1600);
    });
  });

  $("#btnDescargarInforme")?.addEventListener("click", () => {
    const blob = new Blob([textoInforme()], { type: "text/plain;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = formatoInforme === "csv" ? "digest-sintomas.csv" : "digest-informe.txt";
    a.click();
    URL.revokeObjectURL(url);
  });

  $("#btnCopiarHash")?.addEventListener("click", (ev) => {
    const btn = ev.currentTarget;
    const original = btn.textContent;
    copiar($("#hashSha").textContent.trim(), (m) => {
      btn.textContent = m;
      setTimeout(() => { btn.textContent = original; }, 1600);
    });
  });

  /* ============================= Glosario =============================== */

  const GLOSARIO = [
    ["EII", "Enfermedad inflamatoria intestinal. Agrupa la colitis ulcerosa, la enfermedad de Crohn y la colitis indeterminada."],
    ["Colitis ulcerosa", "Inflamación continua del colon y del recto."],
    ["Enfermedad de Crohn", "Inflamación que puede afectar a cualquier parte del tubo digestivo, con frecuencia de forma discontinua."],
    ["Colitis indeterminada", "Inflamación del colon que no encaja con claridad en colitis ulcerosa ni en Crohn."],
    ["Colon irritable", "Trastorno funcional del intestino, sin inflamación demostrable, con dolor o molestia y cambios en el tránsito."],
    ["Brote", "Periodo de actividad de una enfermedad inflamatoria intestinal."],
    ["Remisión", "Periodo sin actividad inflamatoria relevante."],
    ["Escala de Bristol", "Clasificación de la consistencia de las heces en siete tipos, del estreñimiento a la diarrea."],
    ["Tenesmo", "Sensación de necesitar ir al baño sin conseguir evacuar."],
    ["Nutrición enteral", "Alimentación administrada por sonda o por vía digestiva."],
    ["Ostomía", "Salida artificial del intestino a través de la pared abdominal."],
    ["Resección ileocecal", "Extirpación quirúrgica de la parte final del íleon y del inicio del colon."],
    ["FODMAP", "Conjunto de hidratos de carbono fermentables que algunas personas con colon irritable toleran peor."],
    ["Aminosalicilatos (5-ASA)", "Grupo de fármacos de base para la colitis ulcerosa, por ejemplo la mesalazina."],
    ["Adherencia", "Porcentaje de tomas tomadas sobre las programadas, que el informe calcula por fármaco."],
    ["Alarma exacta", "Permiso de Android 12+ que permite avisar a la hora exacta; sin él, la app usa alarmas inexactas."],
  ];

  const contGlosario = $("#glosarioLista");
  const pintarGlosario = (filtro = "") => {
    const q = filtro.trim().toLowerCase();
    const items = GLOSARIO.filter(([t, d]) => !q || t.toLowerCase().includes(q) || d.toLowerCase().includes(q));
    contGlosario.innerHTML = items.map(([t, d]) => `
      <dl class="glosario__term ${q ? "resalta" : ""}">
        <dt>${t}</dt><dd>${d}</dd>
      </dl>`).join("");
    $("#glosarioVacio").hidden = items.length > 0;
  };
  pintarGlosario();
  $("#buscadorGlosario")?.addEventListener("input", (ev) => pintarGlosario(ev.target.value));

})();
