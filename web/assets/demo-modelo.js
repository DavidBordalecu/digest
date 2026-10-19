/* =========================================================================
   Digest · demo · modelo
   Datos de dominio, estado de la demo, generación de señales clínicas e
   informe. Todo lo que hay aquí replica la documentación funcional v1.7.
   ========================================================================= */

const DG = window.DG = window.DG || {};

/* ============================== Modelo ================================= */

DG.CONDICIONES = {
  colitis_ulcerosa: {
    nombre: "Colitis ulcerosa",
    desc: "Afecta al colon y al recto.",
    inflamatoria: true,
    nota: "La colitis ulcerosa afecta al colon y al recto: preguntas por sangre, tenesmo y estado del brote.",
    control: "Estado del brote",
    opciones: ["En remisión", "Brote leve", "Brote moderado", "Brote severo", "No me aplica"],
    zonas: ["Proctitis", "Colitis izquierda", "Colitis extensa", "Pancolitis"],
  },
  crohn: {
    nombre: "Enfermedad de Crohn",
    desc: "Puede afectar a cualquier parte del tubo digestivo.",
    inflamatoria: true,
    nota: "En Crohn aparecen las zonas del íleon y las manifestaciones perianales, que en colitis ulcerosa no se preguntan.",
    control: "Estado del brote",
    opciones: ["En remisión", "Brote leve", "Brote moderado", "Brote severo", "No me aplica"],
    zonas: ["Íleítis (íleon)", "Íleo-colítica", "Colitis de Crohn", "Gastroduodenal", "Perianal"],
  },
  indeterminada: {
    nombre: "Colitis indeterminada",
    desc: "No está claro si es colitis o Crohn.",
    inflamatoria: true,
    nota: "La colitis indeterminada recibe el cuestionario más amplio: el de Crohn, con perianales incluidas.",
    control: "Estado del brote",
    opciones: ["En remisión", "Brote leve", "Brote moderado", "Brote severo", "No me aplica"],
    zonas: ["Proctitis", "Colitis izquierda", "Colitis extensa", "Pancolitis", "Íleítis (íleon)", "Íleo-colítica", "Colitis de Crohn", "Gastroduodenal", "Perianal"],
  },
  colon_irritable: {
    nombre: "Colon irritable",
    desc: "Dolor o molestia abdominal con cambios en el tránsito, sin inflamación.",
    inflamatoria: false,
    nota: "El colon irritable no pregunta zonas ni cirugía, y excluye sangre, fiebre y pérdida de peso: no forman parte del cuadro.",
    control: "Control de los síntomas",
    opciones: ["Sin molestias", "Molestias leves", "Molestias moderadas", "Molestias intensas", "No me aplica"],
    zonas: [],
  },
};

const CATEGORIAS = ["Intestinales", "Perianales", "Generales", "Extraintestinales"];

const TODAS = "todas", EII = "eii", ILEON = "ileon";

DG.SINTOMAS = [
  { id: "diarrea", nombre: "Diarrea", cat: "Intestinales", con: TODAS },
  { id: "dolor_abdominal", nombre: "Dolor abdominal", cat: "Intestinales", con: TODAS },
  { id: "tenesmo", nombre: "Tenesmo (ganas de ir sin poder)", cat: "Intestinales", con: EII },
  { id: "sangre", nombre: "Sangre en heces", cat: "Intestinales", con: EII },
  { id: "mucosidad", nombre: "Mucosidad", cat: "Intestinales", con: TODAS },
  { id: "distension", nombre: "Distensión / hinchazón", cat: "Intestinales", con: TODAS },
  { id: "fisura", nombre: "Fisura anal", cat: "Perianales", con: ILEON },
  { id: "fistula", nombre: "Fístula anal", cat: "Perianales", con: ILEON },
  { id: "absceso", nombre: "Absceso perianal", cat: "Perianales", con: ILEON },
  { id: "supuracion", nombre: "Dolor o supuración perianal", cat: "Perianales", con: ILEON },
  { id: "inflamacion_perianal", nombre: "Inflamación externa perianal", cat: "Perianales", con: ILEON },
  { id: "fatiga", nombre: "Fatiga / astenia", cat: "Generales", con: TODAS },
  { id: "fiebre", nombre: "Fiebre o febrícula", cat: "Generales", con: EII },
  { id: "nauseas", nombre: "Náuseas", cat: "Generales", con: TODAS },
  { id: "perdida_apetito", nombre: "Pérdida de apetito", cat: "Generales", con: EII },
  { id: "perdida_peso", nombre: "Pérdida de peso", cat: "Generales", con: EII },
  { id: "desnutricion", nombre: "Desnutrición", cat: "Generales", con: EII },
  { id: "aftas", nombre: "Aftas en la boca", cat: "Extraintestinales", con: EII },
  { id: "dolor_articular", nombre: "Dolor articular", cat: "Extraintestinales", con: EII },
  { id: "manchas_piel", nombre: "Manchas en la piel", cat: "Extraintestinales", con: EII },
  { id: "ojo_rojo", nombre: "Ojo rojo / uveítis", cat: "Extraintestinales", con: EII },
];

const ES_EII = (c) => ["colitis_ulcerosa", "crohn", "indeterminada"].includes(c);
const ES_ILEON = (c) => ["crohn", "indeterminada"].includes(c);

DG.sintomasDe = (condicion) => {
  const aplica = (s) =>
    s.con === TODAS ||
    (s.con === EII && ES_EII(condicion)) ||
    (s.con === ILEON && ES_ILEON(condicion));
  return DG.SINTOMAS.filter(aplica);
};

DG.categoriasDe = (condicion) => {
  const grupos = {};
  DG.sintomasDe(condicion).forEach((s) => { (grupos[s.cat] = grupos[s.cat] || []).push(s); });
  return Object.entries(grupos).filter(([, lista]) => lista.length);
};

DG.BRISTOL = [
  { n: 1, nombre: "Heces duras sueltas", desc: "Trozos separados y duros, como nueces. Estreñimiento.", color: "#8D6E63" },
  { n: 2, nombre: "Salchicha grumosa", desc: "Agrupadas pero irregulares, como una salchicha grumosa.", color: "#8D6E63" },
  { n: 3, nombre: "Salchicha con grietas", desc: "Como una salchicha pero con grietas en la superficie. Normal.", color: "#A1887F" },
  { n: 4, nombre: "Suave y lisa", desc: "Como una serpiente, suave y lisa. Ideal.", color: "#66BB6A" },
  { n: 5, nombre: "Trozos blandos con bordes", desc: "Trozos blandos con bordes definidos. Ligera diarrea.", color: "#FFB74D" },
  { n: 6, nombre: "Trozos blandos deshilachados", desc: "Blandos, con bordes deshilachados, aspecto de papilla.", color: "#E57373" },
  { n: 7, nombre: "Líquidas, sin trozos", desc: "Totalmente líquidas, sin restos sólidos. Diarrea severa.", color: "#C62828" },
];

DG.GRAVEDAD = ["#4CAF50", "#9CCC65", "#FFB74D", "#E57373", "#C62828"];

DG.SANGRE = ["Ninguna", "Ligera", "Moderada", "Abundante"];
DG.TIPOS_COMIDA = ["Desayuno", "Media mañana", "Almuerzo", "Merienda", "Cena", "Tarde / nocturno"];
DG.MEJORA = ["Sin mejoras", "Con poca mejora", "Mejora clara", "Con gran mejora", "Me está empeorando", "No me aplica"];
DG.SENSACION = ["Muy bien", "Bien", "Regular", "Mal", "Muy mal"];
DG.HIDRATACION = ["Normal", "Bebí menos de lo debido", "Bebí mucho más", "No me aplica"];
DG.TIPOS_DOLORES = [
  ["Cólico", "Va y viene en forma de retortijón, suele calmarse al defecar."],
  ["Fijo", "Continuo, no cede. Puede indicar inflamación localizada o estenosis."],
  ["Punzante", "Como un pinchazo puntual."],
  ["Presivo", "Tensión o pesadez en el abdomen."],
];
DG.ZONAS_DOLOR = ["Epigastrio (estómago)", "Hipocondrio izquierdo", "Periumbilical (ombligo)", "Hipocondrio derecho", "Fosa ilíaca izquierda", "Fosa ilíaca derecha (típica de Crohn)", "Pélvica", "Lumbar (espalda baja)", "Difusa (todo el abdomen)"];
DG.TIPOS_ENTERAL = ["Nutrición enteral exclusiva", "Nutrición enteral parcial", "Fórmula como suplemento"];
DG.TIPOS_DOCS = ["Análisis de sangre", "Colonoscopia", "Endoscopia", "Biopsia", "Prueba de imagen", "Calibre del íleon", "Otro documento"];
DG.PRESENTACIONES = ["Medicamento", "Suplemento"];
DG.GRUPOS = ["Aminosalicilatos (5-ASA)", "Corticoides", "Inmunosupresores", "Biológicos", "Antidiarreicos", "Analgésicos", "Suplementos", "Otros"];

DG.PROFESIONALES = [
  { nombre: "Dra. Elena Vidal", esp: "Gastroenterología", centro: "Hospital General de Norte", ciudad: "Madrid", eii: true, online: true, tel: "910 000 001" },
  { nombre: "Dr. Marcos Aguirre", esp: "Gastroenterología", centro: "Centro de Salud La Vega", ciudad: "Madrid", eii: true, online: false, tel: "910 000 002" },
  { nombre: "Dra. Nuria Castells", esp: "Gastroenterología", centro: "Clínica Digestiva Sur", ciudad: "Sevilla", eii: true, online: true, tel: "954 000 003" },
  { nombre: "Dr. Iván Serrano", esp: "Gastroenterología", centro: "Hospital Comarcal del Ebro", ciudad: "Zaragoza", eii: false, online: false, tel: "976 000 004" },
  { nombre: "Dra. Paula Requena", esp: "Nutrición", centro: "Unidad de Nutrición Clínica", ciudad: "Valencia", eii: true, online: true, tel: "963 000 005" },
  { nombre: "Dr. Andrés Molina", esp: "Nutrición", centro: "Centro Dietética Norte", ciudad: "Bilbao", eii: false, online: true, tel: "944 000 006" },
];

/* ============================== Utilidades ============================= */

DG.uid = () => Math.random().toString(36).slice(2, 9);

DG.hoyISO = () => {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
};

DG.diasAtrasISO = (n) => {
  const d = new Date();
  d.setDate(d.getDate() - n);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
};

DG.sumarDiasISO = (iso, n) => {
  const [a, m, d] = iso.split("-").map(Number);
  const f = new Date(a, m - 1, d);
  f.setDate(f.getDate() + n);
  return `${f.getFullYear()}-${String(f.getMonth() + 1).padStart(2, "0")}-${String(f.getDate()).padStart(2, "0")}`;
};

DG.fmtFecha = (iso) => {
  const [a, m, d] = iso.split("-");
  return `${d}/${m}/${a}`;
};

const MESES = ["enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"];
const DIAS_SEMANA = ["domingo", "lunes", "martes", "miércoles", "jueves", "viernes", "sábado"];

DG.fechaLarga = (iso) => {
  const [a, m, d] = iso.split("-").map(Number);
  const f = new Date(a, m - 1, d);
  return `${DIAS_SEMANA[f.getDay()]}, ${d} de ${MESES[m - 1]} de ${a}`;
};

DG.diasEntre = (iso1, iso2) => {
  const [a1, m1, d1] = iso1.split("-").map(Number);
  const [a2, m2, d2] = iso2.split("-").map(Number);
  const f1 = new Date(a1, m1 - 1, d1), f2 = new Date(a2, m2 - 1, d2);
  return Math.round((f1 - f2) / 86400000);
};

DG.horaAhora = () => {
  const d = new Date();
  return `${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
};

DG.diaVacio = (fecha) => ({
  fecha,
  sintomas: {},
  comidas: [],
  deposiciones: [],
  dolores: [],
  enteral: [],
  tomas: [],
  diario: { mejora: null, sensacion: null, control: null, hidratacion: null, notas: "" },
  peso: null,
});

/* ================================ Estado =============================== */

DG.estadoInicial = () => ({
  perfil: null,
  condicionPrevia: "colitis_ulcerosa",
  seccion: "hoy",
  avisoPermisos: true,
  dias: {},
  medicamentos: [],
  documentos: [],
  cirugias: [],
  ultimaConsulta: null,
  intervaloMeses: 6,
  pasos: {},
});

DG.cargar = () => {
  try {
    const crudo = localStorage.getItem("digest-demo-v1");
    if (!crudo) return DG.estadoInicial();
    const datos = JSON.parse(crudo);
    return Object.assign(DG.estadoInicial(), datos);
  } catch (_) {
    return DG.estadoInicial();
  }
};

DG.guardar = () => {
  try { localStorage.setItem("digest-demo-v1", JSON.stringify(DG.estado)); } catch (_) {}
};

DG.estado = DG.cargar();

/** Devuelve el día creándolo si no existe. */
DG.dia = (fecha = DG.hoyISO()) => {
  if (!DG.estado.dias[fecha]) DG.estado.dias[fecha] = DG.diaVacio(fecha);
  return DG.estado.dias[fecha];
};

DG.hoy = () => DG.dia(DG.hoyISO());

DG.diasOrdenados = () => Object.values(DG.estado.dias).sort((a, b) => a.fecha.localeCompare(b.fecha));

DG.reiniciarEstado = () => {
  DG.estado = DG.estadoInicial();
  try { localStorage.removeItem("digest-demo-v1"); } catch (_) {}
};

/** Crea las tomas de hoy a partir de la agenda de medicación. */
DG.prepararTomas = () => {
  const hoy = DG.hoy();
  const claves = new Set(hoy.tomas.map((t) => t.medId + t.hora));
  DG.estado.medicamentos.forEach((med) => {
    med.horarios.forEach((hora) => {
      if (claves.has(med.id + hora)) return;
      hoy.tomas.push({
        id: DG.uid(),
        medId: med.id,
        nombre: med.nombre,
        dosis: med.dosis,
        hora,
        estado: "pendiente",
      });
    });
  });
  hoy.tomas.sort((a, b) => a.hora.localeCompare(b.hora));
};

/* ========================= Señales clínicas ============================ */

const sigueMarcado = (sintomaId, hastaISO) => {
  let racha = 0;
  let fecha = hastaISO;
  for (let i = 0; i < 120; i++) {
    const d = DG.estado.dias[fecha];
    if (!d || !d.sintomas[sintomaId]) break;
    racha++;
    fecha = DG.sumarDiasISO(fecha, -1);
  }
  return racha;
};

/**
 * Evalúa las nueve reglas de la documentación sobre el estado actual.
 * Devuelve [{nivel, titulo, motivo}] ordenado: urgentes primero.
 */
DG.senales = () => {
  const est = DG.estado;
  if (!est.perfil) return [];
  const cond = est.perfil.condicion;
  const irritable = cond === "colon_irritable";
  const hoy = DG.hoy();
  const salida = [];

  const aplicaIrritable = (motivo) =>
    irritable ? `${motivo} En colon irritable esta señal no es habitual y conviene descartar otra causa.` : motivo;

  const urgentes = [], pronto = [];

  // 1 · Sangrado abundante
  if (hoy.deposiciones.some((d) => d.sangre === "Abundante")) {
    urgentes.push({
      id: "sangrado",
      nivel: "urgente",
      titulo: "Sangrado abundante",
      motivo: aplicaIrritable("Has registrado sangre abundante en heces hoy. Conviene contactar hoy con el centro de salud."),
    });
  }

  // 2 · Fiebre alta
  if ((hoy.sintomas.fiebre || 0) >= 4) {
    urgentes.push({
      id: "fiebre",
      nivel: "urgente",
      titulo: "Fiebre alta",
      motivo: aplicaIrritable(`Fiebre marcada con intensidad ${hoy.sintomas.fiebre} sobre 5. No debería esperar.`),
    });
  }

  // 3 · Dolor abdominal intenso
  if ((hoy.sintomas.dolor_abdominal || 0) >= 4) {
    urgentes.push({
      id: "dolor",
      nivel: "urgente",
      titulo: "Dolor abdominal intenso",
      motivo: `Dolor abdominal marcado con intensidad ${hoy.sintomas.dolor_abdominal} sobre 5. Conviene valorarlo hoy mismo.`,
    });
  }

  // 4 · Absceso o fístula perianal (Crohn e indeterminada)
  if (ES_ILEON(cond)) {
    const perianal = ["fisura", "fistula", "absceso", "supuracion", "inflamacion_perianal"]
      .filter((id) => (hoy.sintomas[id] || 0) > 0);
    if (perianal.length) {
      urgentes.push({
        id: "perianal",
        nivel: "urgente",
        titulo: "Posible absceso o fístula perianal",
        motivo: `Has marcado una manifestación perianal nueva (${perianal.map((p) => DG.SINTOMAS.find((s) => s.id === p).nombre).join(", ")}). En Crohn y colitis indeterminada no debe esperar.`,
      });
    }
  }

  // 5 · Ocho o más deposiciones diarreicas
  const diarreicas = hoy.deposiciones.filter((d) => d.bristol >= 6).length;
  if (diarreicas >= 8) {
    urgentes.push({
      id: "deposiciones",
      nivel: "urgente",
      titulo: "Muchas deposiciones diarreicas",
      motivo: `${diarreicas} deposiciones diarreicas registradas hoy (Bristol 6 o 7).`,
    });
  }

  // 6 · Síntoma que se mantiene 14 días
  Object.keys(hoy.sintomas).forEach((id) => {
    const racha = sigueMarcado(id, DG.hoyISO());
    if (racha >= 14) {
      const s = DG.SINTOMAS.find((x) => x.id === id);
      pronto.push({
        id: "racha_" + id,
        nivel: "pronto",
        titulo: "Síntoma que se mantiene",
        motivo: `${s.nombre} lleva ${racha} días seguidos marcado. Conviene comentarlo en la próxima cita.`,
      });
    }
  });

  // 7 · Pérdida de peso relevante
  if (est.pesoHistorico && est.pesoHistorico.length >= 2) {
    const orden = [...est.pesoHistorico].sort((a, b) => a.fecha.localeCompare(b.fecha));
    const viejo = orden[0], nuevo = orden[orden.length - 1];
    const dias = DG.diasEntre(nuevo.fecha, viejo.fecha);
    if (dias >= 80 && viejo.peso > 0) {
      const variacion = ((viejo.peso - nuevo.peso) / viejo.peso) * 100;
      if (variacion > 5) {
        pronto.push({
          id: "peso",
          nivel: "pronto",
          titulo: "Pérdida de peso relevante",
          motivo: `Has perdido un ${String(variacion.toFixed(1)).replace(".", ",")} % del peso en ${Math.round(dias / 30)} meses (${viejo.peso} kg → ${nuevo.peso} kg).`,
        });
      }
    }
  }

  // 8 · Varios síntomas a la vez
  const marcados = Object.entries(hoy.sintomas);
  const conModerado = marcados.some(([, g]) => g >= 3);
  const sensacionMala = ["Mal", "Muy mal"].includes(hoy.diario.sensacion);
  if (marcados.length >= 5 || (marcados.length >= 3 && conModerado) || sensacionMala) {
    pronto.push({
      id: "varios",
      nivel: "pronto",
      titulo: "Varios síntomas a la vez",
      motivo: sensacionMala && marcados.length < 3
        ? `Tu sensación general de hoy es «${hoy.diario.sensacion}» con ${marcados.length} síntoma(s) marcado(s).`
        : `${marcados.length} síntomas marcados hoy${conModerado ? ", con uno de intensidad moderada o más" : ""}.`,
    });
  }

  // 9 · Toca revisión
  if (est.ultimaConsulta && est.intervaloMeses) {
    const dias = DG.diasEntre(DG.hoyISO(), est.ultimaConsulta);
    const intervaloDias = est.intervaloMeses * 30;
    if (dias >= intervaloDias * 0.75) {
      pronto.push({
        id: "revision",
        nivel: "pronto",
        titulo: "Toca revisión",
        motivo: `Han pasado ${dias} días desde la última consulta de un intervalo de ${est.intervaloMeses} meses (${Math.round((dias / intervaloDias) * 100)} % del periodo).`,
      });
    }
  }

  return [...urgentes, ...pronto];
};

/** Mensaje snackbar tras guardar un registro, igual que la app. */
DG.mensajeRegistro = () => {
  const senales = DG.senales();
  if (!senales.length) return "Guardado. Ningún aviso de consulta por ahora.";
  const urgentes = senales.filter((s) => s.nivel === "urgente");
  if (urgentes.length) return `Guardado. <strong>AVISO URGENTE:</strong> ${urgentes[0].titulo}`;
  return senales.length === 1
    ? `Guardado. 1 aviso: ${senales[0].titulo}`
    : `Guardado. ${senales.length} avisos: ${senales.map((s) => s.titulo).join(", ")}`;
};

/* ===================== Datos de ejemplo (semilla) ====================== */

const ALEATORIO = (semilla) => {
  let s = semilla;
  return () => {
    s = (s * 1103515245 + 12345) % 2147483648;
    return s / 2147483648;
  };
};

/**
 * Construye un estado completo con 7 días de historial más hoy.
 * Se usa tanto para el botón «cargar datos de ejemplo» como para el
 * informe de muestra de la web cuando la demo está vacía.
 */
DG.crearSemilla = (condicion = "colitis_ulcerosa") => {
  const est = DG.estadoInicial();
  const rnd = ALEATORIO(20261019);
  const irritable = condicion === "colon_irritable";

  est.condicionPrevia = condicion;
  est.perfil = {
    condicion,
    anio: "2019",
    peso: 66.8,
    altura: 168,
    afectaciones: irritable ? [] : (condicion === "crohn" ? ["Íleo-colítica"] : ["Pancolitis"]),
    ostomia: false,
  };
  est.pesoHistorico = [
    { fecha: DG.diasAtrasISO(92), peso: 70.0 },
    { fecha: DG.diasAtrasISO(30), peso: 67.9 },
    { fecha: DG.hoyISO(), peso: 66.8 },
  ];
  est.ultimaConsulta = DG.diasAtrasISO(141);
  est.intervaloMeses = 6;
  est.avisoPermisos = false;

  est.medicamentos = [
    { id: "med1", nombre: "Mesalazina 1 g", dosis: "1 g · 1 comprimido", presentacion: "Medicamento", grupo: "Aminosalicilatos (5-ASA)", instrucciones: "Con comida", horarios: ["08:00", "14:00", "20:00"] },
    { id: "med2", nombre: "Vitamina D 1000 UI", dosis: "1000 UI · 1 cápsula", presentacion: "Suplemento", grupo: "Suplementos", instrucciones: "Con el desayuno", horarios: ["09:00"] },
  ];

  if (!irritable && ["crohn", "indeterminada"].includes(condicion)) {
    est.cirugias = [{ id: DG.uid(), tipo: "Resección ileocecal", fecha: "2022" }];
  }

  est.documentos = [
    { id: DG.uid(), nombre: "Colonoscopia 2024", tipo: "Colonoscopia", fecha: "12/03/2024", notas: "Biopsia del colon transverso sin actividad inflamatoria.", tamano: "1,2 MB", formato: "PDF" },
    { id: DG.uid(), nombre: "Analítica de sangre", tipo: "Análisis de sangre", fecha: "05/02/2026", notas: "PCR normal, ferritina baja.", tamano: "480 KB", formato: "PDF" },
  ];

  const recetas = [
    ["Tostada con aceite y huevo cocido", "Tostada con aceite y huevo"],
    ["Avena con plátano y café con leche", "Avena con plátano"],
    ["Pollo con arroz y zanahoria", "Pollo con arroz"],
    ["Yogur natural y galletas", "Yogur con galletas"],
    ["Sopa de verduras y pavo", "Sopa de verduras con pavo"],
    ["Puré de calabacín y merluza", "Merluza al horno con puré"],
  ];

  for (let atras = 6; atras >= 0; atras--) {
    const fecha = DG.diasAtrasISO(atras);
    const d = DG.diaVacio(fecha);
    const base = Math.floor(rnd() * 3);

    d.sintomas = {
      diarrea: 2 + (atras % 3),
      distension: 1 + (rnd() > 0.5 ? 1 : 0),
      fatiga: 2 + (rnd() > 0.6 ? 1 : 0),
      mucosidad: 1 + (rnd() > 0.7 ? 1 : 0),
    };
    if (atras % 2 === 0) d.sintomas.dolor_abdominal = 2 + (rnd() > 0.6 ? 1 : 0);
    if (!irritable && atras % 3 === 0) d.sintomas.nauseas = 1;
    if (atras === 0) {
      d.sintomas = { diarrea: 3, dolor_abdominal: 2, fatiga: 3, distension: 2 };
    }

    const nDep = 3 + Math.floor(rnd() * 3);
    for (let i = 0; i < nDep; i++) {
      d.deposiciones.push({
        id: DG.uid(),
        hora: `${String(7 + i * 3).padStart(2, "0")}:${rnd() > 0.5 ? "40" : "15"}`,
        bristol: 5 + Math.floor(rnd() * 3),
        sangre: rnd() > 0.72 ? "Ligera" : "Ninguna",
      });
    }

    if (atras % 3 === 1) {
      d.dolores.push({ id: DG.uid(), intensidad: 3, tipo: "Cólico", zona: "Fosa ilíaca izquierda" });
    }

    d.comidas = [
      { id: DG.uid(), tipo: "Desayuno", hora: "08:15", alimentos: recetas[0][atras % 2], control: atras > 0 ? "sin" : null },
      { id: DG.uid(), tipo: "Almuerzo", hora: "13:45", alimentos: recetas[2][atras % 2], control: atras > 0 ? (atras % 2 ? "si" : "sin") : null },
      { id: DG.uid(), tipo: "Cena", hora: "21:00", alimentos: recetas[4][atras % 2], control: atras > 0 ? "sin" : null },
    ];

    d.diario = {
      mejora: atras % 3 === 0 ? "Mejora clara" : (atras % 3 === 1 ? "Con poca mejora" : "Mejora clara"),
      sensacion: atras % 4 === 0 ? "Regular" : "Bien",
      control: atras % 5 === 0 ? (irritable ? "Molestias leves" : "Brote leve") : (irritable ? "Sin molestias" : "En remisión"),
      hidratacion: "Normal",
      notas: atras === 2 ? "Descansé mejor; las ganas de ir al baño aparecieron solo por la mañana." : "",
    };

    d.peso = atras === 0 ? 66.8 : null;

    est.medicamentos.forEach((med) => {
      med.horarios.forEach((hora) => {
        d.tomas.push({
          id: DG.uid(),
          medId: med.id,
          nombre: med.nombre,
          dosis: med.dosis,
          hora,
          estado: atras === 0 ? (hora >= DG.horaAhora() ? "pendiente" : (hora === "08:00" && DG.horaAhora() > "09:30" ? "omitida" : "tomada")) : (rnd() > 0.94 ? "omitida" : "tomada"),
        });
      });
    });

    est.dias[fecha] = d;
  }

  est.dias[DG.hoyISO()].tomas.sort((a, b) => a.hora.localeCompare(b.hora));
  return est;
};

/* ============================== Informe ================================= */

const cuenta = (arr, f) => arr.filter(f).length;
const media = (arr) => (arr.length ? arr.reduce((a, b) => a + b, 0) / arr.length : 0);
const pct = (a, b) => (b ? Math.round((a / b) * 100) : 0);

const periodoSemanas = (dias) => {
  const hoy = DG.hoyISO();
  const desde = DG.sumarDiasISO(hoy, -(dias - 1));
  return DG.diasOrdenados().filter((d) => d.fecha >= desde && d.fecha <= hoy);
};

/**
 * Informe en texto o CSV del periodo indicado, calculado sobre el estado
 * que se le pase (el de la demo o el de muestra).
 */
DG.generarInforme = (estado, formato = "texto", dias = 7) => {
  const decimal = (n) => Number(n).toFixed(1).replace(".", ",");
  const cond = estado.perfil ? estado.perfil.condicion : "colitis_ulcerosa";
  const meta = DG.CONDICIONES[cond];
  const hoy = DG.hoyISO();
  const desde = DG.sumarDiasISO(hoy, -(dias - 1));
  const seleccion = Object.values(estado.dias)
    .filter((d) => d.fecha >= desde && d.fecha <= hoy)
    .sort((a, b) => a.fecha.localeCompare(b.fecha));

  if (!seleccion.length) {
    return "No hay anotaciones en el periodo seleccionado.\n\nCarga los datos de ejemplo de la demo o registra algunos días para ver el informe.";
  }

  const sintomasUsados = {};
  seleccion.forEach((d) => Object.keys(d.sintomas).forEach((id) => { sintomasUsados[id] = true; }));

  if (formato === "csv") {
    const filas = ["fecha; síntoma; intensidad; deposiciones; sangre; bristol"];
    seleccion.forEach((d) => {
      const sangreDia = d.deposiciones.some((x) => x.sangre !== "Ninguna");
      const bristolMedio = d.deposiciones.length
        ? decimal(d.deposiciones.reduce((a, b) => a + b.bristol, 0) / d.deposiciones.length)
        : "";
      Object.entries(d.sintomas).forEach(([id, g]) => {
        const s = DG.SINTOMAS.find((x) => x.id === id);
        filas.push([DG.fmtFecha(d.fecha), s ? s.nombre : id, g, d.deposiciones.length, sangreDia ? "sí" : "no", bristolMedio].join("; "));
      });
      if (!Object.keys(d.sintomas).length) {
        filas.push([DG.fmtFecha(d.fecha), "Sin síntomas", "", d.deposiciones.length, sangreDia ? "sí" : "no", bristolMedio].join("; "));
      }
    });
    return filas.join("\n");
  }

  const L = [];
  const titulo = (t) => { L.push(""); L.push(t.toUpperCase()); L.push("-".repeat(t.length)); };

  L.push("INFORME PARA TU MÉDICO — Digest");
  L.push(`Periodo: ${DG.fmtFecha(desde)} a ${DG.fmtFecha(hoy)} (${dias} días)`);
  L.push(`Generado el ${DG.fmtFecha(hoy)} por el paciente`);

  // Paciente
  titulo("Paciente");
  L.push(`- Diagnóstico: ${meta.nombre}`);
  if (estado.perfil && estado.perfil.afectaciones.length) L.push(`- Zonas afectadas: ${estado.perfil.afectaciones.join(", ")}`);
  L.push(`- Ostomía o resección: ${estado.perfil && estado.perfil.ostomia ? "Sí" : "No"}`);
  if (estado.perfil && estado.perfil.peso) L.push(`- Peso actual: ${estado.perfil.peso} kg · Altura ${estado.perfil.altura} cm`);
  if (estado.ultimaConsulta) {
    const diasU = DG.diasEntre(hoy, estado.ultimaConsulta);
    L.push(`- Última consulta: ${DG.fmtFecha(estado.ultimaConsulta)} (hace ${diasU} días, cada ${estado.intervaloMeses} meses)`);
  }

  // Medicación
  const tomasPeriodo = seleccion.flatMap((d) => d.tomas || []);
  if (estado.medicamentos.length) {
    titulo("Medicación");
    estado.medicamentos.forEach((med) => {
      const suyas = tomasPeriodo.filter((t) => t.medId === med.id);
      if (!suyas.length) {
        L.push(`- ${med.nombre}: sin tomas registradas en el periodo`);
      } else {
        const tomadas = cuenta(suyas, (t) => t.estado === "tomada");
        const omitidas = cuenta(suyas, (t) => t.estado === "omitida");
        const pendientes = cuenta(suyas, (t) => t.estado === "pendiente");
        const pendienteTxt = pendientes === 1 ? "1 pendiente" : `${pendientes} pendientes`;
        L.push(`- ${med.nombre}: ${suyas.length} programadas · ${tomadas} tomadas · ${omitidas} omitidas · ${pendienteTxt} · adherencia ${pct(tomadas, tomadas + omitidas)} %`);
      }
      if (med.instrucciones || med.dosis) {
        L.push(`  ${[med.dosis, med.instrucciones].filter(Boolean).join(" · ")} · horarios ${(med.horarios || []).join(", ")}`);
      }
    });
  }

  // Síntomas
  const ids = Object.keys(sintomasUsados);
  if (ids.length) {
    titulo("Síntomas");
    ids.forEach((id) => {
      const s = DG.SINTOMAS.find((x) => x.id === id);
      const registros = seleccion.filter((d) => d.sintomas[id]);
      const grados = registros.map((d) => d.sintomas[id]);
      L.push(`- ${s ? s.nombre : id}: ${registros.length} de ${seleccion.length} días · media ${decimal(media(grados))} / 5`);
      L.push(`  ${registros.map((d) => `${DG.fmtFecha(d.fecha)}: ${d.sintomas[id]}`).join(" · ")}`);
    });
  }

  // Deposiciones
  const deps = seleccion.flatMap((d) => d.deposiciones);
  if (deps.length) {
    titulo("Deposiciones");
    const conSangre = cuenta(seleccion, (d) => d.deposiciones.some((x) => x.sangre !== "Ninguna"));
    const diasDiarrea = cuenta(seleccion, (d) => d.deposiciones.some((x) => x.bristol >= 6));
    L.push(`- Total: ${deps.length} · media de Bristol ${decimal(deps.reduce((a, b) => a + b.bristol, 0) / deps.length)}`);
    L.push(`- Días con sangre: ${conSangre} · días con diarrea (Bristol 6-7): ${diasDiarrea}`);
    const niveles = [...new Set(deps.map((d) => d.sangre))].filter((n) => n !== "Ninguna");
    if (niveles.length) L.push(`- Cantidad de sangre registrada: ${niveles.join(", ")}`);
  }

  // Dolor
  const dolores = seleccion.flatMap((d) => d.dolores);
  if (dolores.length) {
    titulo("Dolor abdominal");
    L.push(`- ${dolores.length} episodios · media ${decimal(media(dolores.map((d) => d.intensidad)))} / 5`);
    const tipos = [...new Set(dolores.map((d) => d.tipo))];
    const zonas = [...new Set(dolores.map((d) => d.zona))];
    L.push(`- Carácter: ${tipos.join(", ")}`);
    L.push(`- Localización: ${zonas.join(", ")}`);
  }

  // Nutrición enteral
  const enterales = seleccion.flatMap((d) => d.enteral);
  if (enterales.length) {
    titulo("Nutrición enteral");
    const total = enterales.reduce((a, b) => a + (Number(b.volumen) || 0), 0);
    L.push(`- ${enterales.length} registros · ${total} ml en el periodo`);
    [...new Set(enterales.map((e) => `${e.producto || "Fórmula"} (${e.tipo})`))].forEach((p) => L.push(`- ${p}`));
  }

  // Comidas
  const comidas = seleccion.flatMap((d) => d.comidas);
  if (comidas.length) {
    titulo("Comidas y reacción");
    comidas.slice(0, 14).forEach((c) => {
      const reaccion = c.control === "si" ? "cambios notados después" : (c.control === "sin" ? "sin cambios" : "sin controlar");
      L.push(`- ${c.tipo} (${c.hora}): ${c.alimentos || "sin detalle"} → ${reaccion}`);
    });
    if (comidas.length > 14) L.push(`- … y ${comidas.length - 14} comidas más`);
    const conCambios = cuenta(comidas, (c) => c.control === "si");
    L.push(`- Total: ${comidas.length} comidas · ${conCambios} con cambios después de comer`);
  }

  // Peso
  if (estado.pesoHistorico && estado.pesoHistorico.length >= 2) {
    const orden = [...estado.pesoHistorico].sort((a, b) => a.fecha.localeCompare(b.fecha));
    const primero = orden[0], ultimo = orden[orden.length - 1];
    const difNum = ultimo.peso - primero.peso;
    titulo("Peso");
    L.push(`- ${primero.peso} kg (${DG.fmtFecha(primero.fecha)}) → ${ultimo.peso} kg (${DG.fmtFecha(ultimo.fecha)})`);
    L.push(`- Variación: ${difNum > 0 ? "+" : ""}${decimal(difNum)} kg en ${DG.diasEntre(ultimo.fecha, primero.fecha)} días`);
  }

  // Diario
  titulo("Diario");
  const rellenos = (campo) => seleccion.map((d) => d.diario[campo]).filter(Boolean);
  const resumen = (campo) => {
    const vals = rellenos(campo);
    if (!vals.length) return null;
    const cuentaValores = {};
    vals.forEach((v) => { cuentaValores[v] = (cuentaValores[v] || 0) + 1; });
    return Object.entries(cuentaValores).map(([v, n]) => `${v} (${n} ${n === 1 ? "día" : "días"})`).join(" · ");
  };
  const mejora = resumen("mejora");
  if (mejora) L.push(`- Mejora con la medicación: ${mejora}`);
  const sensacion = resumen("sensacion");
  if (sensacion) L.push(`- Sensación general: ${sensacion}`);
  const control = resumen("control");
  if (control) L.push(`- ${meta.control}: ${control}`);
  const hidr = resumen("hidratacion");
  if (hidr) L.push(`- Hidratación: ${hidr}`);
  const notas = seleccion.filter((d) => d.diario.notas);
  if (notas.length) {
    L.push("- Notas:");
    notas.forEach((d) => L.push(`  ${DG.fmtFecha(d.fecha)}: ${d.diario.notas}`));
  }
  if (!mejora && !sensacion && !control && !hidr && !notas.length) L.push("- Sin anotaciones de diario en el periodo.");

  // Documentos
  if (estado.documentos.length) {
    titulo("Informes médicos adjuntos");
    estado.documentos.forEach((doc) => L.push(`- ${doc.nombre} · ${doc.tipo} · ${doc.fecha} · ${doc.formato} (${doc.tamano})`));
  }

  // Señales
  const anteriores = DG.estado;
  DG.estado = estado;
  const senales = DG.senales();
  DG.estado = anteriores;
  titulo("Señales de alerta registradas");
  if (senales.length) senales.forEach((s) => L.push(`- [${s.nivel === "urgente" ? "Urgente" : "Pronto"}] ${s.titulo} — ${s.motivo}`));
  else L.push("- Ninguna señal de alerta en el periodo.");

  L.push("");
  L.push("—");
  L.push("Este resumen lo he generado yo a partir de mis anotaciones en Digest.");
  L.push("No es una valoración médica ni una historia clínica oficial.");

  return L.join("\n");
};
