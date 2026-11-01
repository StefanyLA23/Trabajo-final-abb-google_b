"use strict";

// La interfaz solo presenta datos y dibuja relaciones recibidas. El ABB vive en Java.
(() => {
  const $ = id => document.getElementById(id);
  const numero = new Intl.NumberFormat("es-CO");
  const estado = { raiz: null, terminos: [], top: [], nodos: new Map(), posiciones: new Map(), zoom: 1, seleccionado: null };
  let versionCarga = 0;
  let versionSugerencias = 0;
  let temporizadorSugerencias;
  let versionAnimacion = 0;
  let ultimaSecuencia = [];
  let dibujo = null;
  let primerAjuste = true;

  function elemento(etiqueta, clase, texto) {
    const nodo = document.createElement(etiqueta);
    if (clase) nodo.className = clase;
    if (texto !== undefined) nodo.textContent = texto;
    return nodo;
  }

  function svg(etiqueta, atributos, texto) {
    const nodo = document.createElementNS("http://www.w3.org/2000/svg", etiqueta);
    for (const [nombre, valor] of Object.entries(atributos)) nodo.setAttribute(nombre, String(valor));
    if (texto !== undefined) nodo.textContent = texto;
    return nodo;
  }

  async function api(ruta, opciones = {}) {
    const headers = { Accept: "application/json" };
    if (opciones.json !== undefined) headers["Content-Type"] = "application/json";
    let respuesta;
    try {
      respuesta = await fetch(ruta, { method: opciones.method || "GET", headers,
        body: opciones.json === undefined ? undefined : JSON.stringify(opciones.json), cache: "no-store" });
    } catch (error) {
      $("connection").classList.add("offline");
      $("connection").replaceChildren(elemento("i"), document.createTextNode("Sin conexión"));
      throw new Error("No hay conexión con el servidor local. Comprueba que ServidorWeb siga en ejecución.");
    }
    const datos = await respuesta.json();
    if (!respuesta.ok) throw new Error(datos.error || "No se pudo completar la operación.");
    $("connection").classList.remove("offline");
    $("connection").replaceChildren(elemento("i"), document.createTextNode("Servidor local activo"));
    return datos;
  }

  function consulta(ruta, parametros) { return `${ruta}?${new URLSearchParams(parametros)}`; }

  function mensaje(texto, error = false) {
    $("feedback-text").textContent = texto;
    $("feedback").classList.toggle("error", error);
    $("feedback").hidden = false;
  }

  const manejar = accion => async evento => {
    try { await accion(evento); } catch (error) { mensaje(error.message, true); }
  };

  function formulario(id, accion) {
    const form = $(id);
    form.addEventListener("submit", manejar(async evento => {
      evento.preventDefault();
      if (!form.reportValidity() || form.dataset.ocupado) return;
      const boton = form.querySelector("button[type=submit]");
      form.dataset.ocupado = "true";
      if (boton) boton.disabled = true;
      try { await accion(Object.fromEntries(new FormData(form)), form); }
      finally { delete form.dataset.ocupado; if (boton) boton.disabled = false; }
    }));
  }

  function hoy() {
    const fecha = new Date();
    return `${fecha.getFullYear()}-${String(fecha.getMonth() + 1).padStart(2, "0")}-${String(fecha.getDate()).padStart(2, "0")}`;
  }

  function mostrarRegistro(dato, titulo) {
    const tarjeta = elemento("article", "record-card");
    if (titulo) tarjeta.append(elemento("div", "record-label", titulo));
    if (!dato) { tarjeta.append(elemento("p", "empty-text", "Sin resultado.")); return tarjeta; }
    tarjeta.dataset.termino = dato.termino;
    tarjeta.append(elemento("h3", "", dato.termino));
    const lista = elemento("dl");
    for (const [etiqueta, valor] of [["Búsquedas", numero.format(dato.busquedas)], ["Categoría", dato.categoria], ["Última búsqueda", dato.fecha]]) {
      const bloque = elemento("div");
      bloque.append(elemento("dt", "", etiqueta), elemento("dd", "", valor));
      lista.append(bloque);
    }
    tarjeta.append(lista);
    return tarjeta;
  }

  function renderTop(id, datos) {
    const contenedor = $(id);
    contenedor.replaceChildren();
    if (!datos.length) { contenedor.append(elemento("p", "empty-text", "El índice aún no tiene términos.")); return; }
    const maximo = Math.max(1, datos[0].busquedas);
    datos.forEach((dato, indice) => {
      const fila = elemento("div", "top-item");
      const centro = elemento("div");
      const pista = elemento("div", "popularity-track");
      const barra = elemento("div", "popularity-fill");
      barra.style.width = `${dato.busquedas / maximo * 100}%`;
      pista.append(barra);
      centro.append(elemento("span", "top-term", dato.termino), elemento("span", "top-category", dato.categoria), pista);
      fila.append(elemento("span", "rank", String(indice + 1).padStart(2, "0")), centro,
        elemento("span", "top-count", numero.format(dato.busquedas)));
      contenedor.append(fila);
    });
  }

  function actualizarSelect(id) {
    const select = $(id);
    const anterior = select.value;
    select.replaceChildren(new Option("Selecciona un término", ""));
    for (const dato of estado.terminos) select.add(new Option(dato.termino, dato.termino));
    if (estado.terminos.some(dato => dato.termino === anterior)) select.value = anterior;
  }

  async function actualizarTodo() {
    const version = ++versionCarga;
    const [arbol, terminos, top] = await Promise.all([api("/api/arbol"), api("/api/terminos"), api("/api/top")]);
    if (version !== versionCarga) return;
    estado.raiz = arbol.raiz;
    estado.terminos = terminos.resultados;
    estado.top = top.resultados;
    const datos = arbol.estado;
    document.querySelectorAll("[data-stat]").forEach(nodo => { nodo.textContent = numero.format(datos[nodo.dataset.stat]); });
    $("csv-count").textContent = numero.format(datos.registrosCSV);
    $("load-badge").textContent = ({ original: "Datos originales", modificado: "Con cambios", vacío: "Árbol vacío" })[datos.carga];
    $("load-badge").className = `badge ${datos.carga === "original" ? "green" : "neutral"}`;
    $("tree-summary").textContent = `${datos.total} términos · altura ${datos.altura} · estructura real`;
    renderTop("home-top", estado.top);
    renderTop("query-top-list", estado.top);
    actualizarSelect("update-key");
    actualizarSelect("delete-key");
    for (const id of ["search-result", "admin-search-result"]) {
      const tarjeta = $(id).querySelector(".record-card[data-termino]");
      if (!tarjeta) continue;
      const dato = estado.terminos.find(termino => termino.termino === tarjeta.dataset.termino);
      const titulo = tarjeta.querySelector(".record-label")?.textContent;
      $(id).replaceChildren(dato ? mostrarRegistro(dato, titulo) : elemento("p", "soft-note", "Este término ya no está en el índice."));
    }
    for (const id of ["level-result", "neighbors-result", "range-result"]) $(id).replaceChildren();
    $("extremes-result").replaceChildren(elemento("p", "empty-text", "Consulta los extremos del árbol actual."));
    renderArbol();
    actualizarTipoEliminacion();
    // Una secuencia anterior podría contener nodos ya eliminados.
    ultimaSecuencia = [];
    $("animate-traversal").disabled = true;
    $("traversal-result").replaceChildren(elemento("li", "empty-text", "Ejecuta un recorrido para obtener la secuencia actual."));
  }

  function navegar(seccion) {
    const panel = $(`view-${seccion}`);
    if (!panel) return;
    document.querySelectorAll("[data-panel]").forEach(nodo => { nodo.hidden = nodo !== panel; });
    document.querySelectorAll("[data-section]").forEach(boton => {
      const activo = boton.dataset.section === seccion;
      boton.classList.toggle("active", activo);
      if (activo) { boton.setAttribute("aria-current", "page"); $("breadcrumb").textContent = boton.childNodes[1].textContent.trim(); }
      else boton.removeAttribute("aria-current");
    });
    if (location.hash !== `#${seccion}`) history.replaceState(null, "", `#${seccion}`);
    if (seccion === "arbol" && primerAjuste && dibujo) centrarVistaInicial();
  }

  document.querySelectorAll("[data-section]").forEach(boton => boton.addEventListener("click", () => navegar(boton.dataset.section)));
  document.querySelectorAll("[data-go]").forEach(boton => boton.addEventListener("click", () => navegar(boton.dataset.go)));
  window.addEventListener("hashchange", () => navegar(location.hash.slice(1) || "inicio"));
  $("refresh").addEventListener("click", manejar(async () => { await actualizarTodo(); mensaje("Índice actualizado."); }));
  $("close-feedback").addEventListener("click", () => { $("feedback").hidden = true; });

  function cerrarSugerencias() { $("suggestions").hidden = true; $("search-input").setAttribute("aria-expanded", "false"); }

  function pedirSugerencias() {
    const version = ++versionSugerencias;
    clearTimeout(temporizadorSugerencias);
    const prefijo = $("search-input").value;
    if (!prefijo.trim()) { cerrarSugerencias(); return; }
    temporizadorSugerencias = setTimeout(manejar(async () => {
      const datos = await api(consulta("/api/autocompletar", { prefijo }));
      if (version !== versionSugerencias) return;
      const lista = $("suggestion-list");
      lista.replaceChildren();
      if (!datos.resultados.length) lista.append(elemento("p", "empty-text", "No hay términos con este prefijo."));
      datos.resultados.slice(0, 8).forEach(dato => {
        const boton = elemento("button", "suggestion");
        boton.type = "button";
        boton.append(elemento("span", "", "⌕"), elemento("span", "", dato.termino), elemento("small", "", dato.categoria));
        boton.addEventListener("click", () => { $("search-input").value = dato.termino; cerrarSugerencias(); $("search-form").requestSubmit(); });
        boton.addEventListener("keydown", evento => {
          const botones = [...lista.querySelectorAll("button")];
          const indice = botones.indexOf(boton);
          if (evento.key === "ArrowDown") { evento.preventDefault(); botones[(indice + 1) % botones.length].focus(); }
          if (evento.key === "ArrowUp") { evento.preventDefault(); (botones[indice - 1] || $("search-input")).focus(); }
          if (evento.key === "Escape") { cerrarSugerencias(); $("search-input").focus(); }
        });
        lista.append(boton);
      });
      $("suggestion-note").textContent = `${datos.resultados.length} sugerencias · ${datos.visitas} nodos visitados con poda${datos.resultados.length > 8 ? " · se muestran 8" : ""}`;
      $("suggestions").hidden = false;
      $("search-input").setAttribute("aria-expanded", "true");
    }), 200);
  }

  $("search-input").addEventListener("input", pedirSugerencias);
  $("search-input").addEventListener("keydown", evento => {
    if (evento.key === "Escape") cerrarSugerencias();
    if (evento.key === "ArrowDown") { const primero = $("suggestion-list").querySelector("button"); if (primero && !$("suggestions").hidden) { evento.preventDefault(); primero.focus(); } }
  });
  document.addEventListener("click", evento => { if (!$("search-form").contains(evento.target)) cerrarSugerencias(); });
  document.querySelectorAll("[data-prefix]").forEach(boton => boton.addEventListener("click", () => {
    $("search-input").value = boton.dataset.prefix;
    $("search-input").focus();
    pedirSugerencias();
  }));

  formulario("search-form", async () => {
    ++versionSugerencias;
    clearTimeout(temporizadorSugerencias);
    cerrarSugerencias();
    $("search-result").hidden = false;
    $("search-result").replaceChildren(elemento("p", "empty-text", "Buscando en el índice…"));
    try {
      const datos = await api("/api/buscar", { method: "POST", json: { termino: $("search-input").value } });
      $("search-result").replaceChildren(mostrarRegistro(datos.termino, `Búsqueda registrada · ${datos.comparaciones} comparaciones · popularidad +1`));
      await actualizarTodo();
    } catch (error) {
      $("search-result").replaceChildren(elemento("p", "soft-note", error.message));
      throw error;
    }
  });

  formulario("insert-form", async (datos, form) => {
    const respuesta = await api("/api/terminos", { method: "POST", json: { ...datos, busquedas: Number(datos.busquedas) } });
    await actualizarTodo();
    mensaje(respuesta.mensaje);
    form.reset();
    form.elements.fecha.value = hoy();
  });

  formulario("admin-search-form", async datos => {
    $("admin-search-result").replaceChildren();
    try {
      const respuesta = await api(consulta("/api/termino", { clave: datos.termino }));
      $("admin-search-result").append(mostrarRegistro(respuesta.termino, "Consulta sin registrar popularidad"));
    } catch (error) { $("admin-search-result").append(elemento("p", "empty-text", error.message)); throw error; }
  });

  $("update-key").addEventListener("change", () => {
    const dato = estado.terminos.find(termino => termino.termino === $("update-key").value);
    for (const campo of ["busquedas", "categoria", "fecha"]) $("update-form").elements[campo].value = dato ? dato[campo] : "";
  });

  formulario("update-form", async (datos, form) => {
    const respuesta = await api(consulta("/api/termino", { clave: datos.clave }), { method: "PUT",
      json: { busquedas: Number(datos.busquedas), categoria: datos.categoria, fecha: datos.fecha } });
    form.reset();
    await actualizarTodo();
    mensaje(respuesta.mensaje);
  });

  function confirmar(titulo, descripcion, textoBoton = "Confirmar") {
    return new Promise(resolve => {
      $("confirm-title").textContent = titulo;
      $("confirm-description").textContent = descripcion;
      $("confirm-accept").textContent = textoBoton;
      const dialogo = $("confirm-dialog");
      dialogo.returnValue = "cancelar";
      dialogo.addEventListener("close", () => resolve(dialogo.returnValue === "confirmar"), { once: true });
      dialogo.showModal();
      dialogo.querySelector('button[value="cancelar"]').focus();
    });
  }

  function tipoNodo(nodo) {
    if (!nodo) return "";
    const hijos = Number(Boolean(nodo.izquierdo)) + Number(Boolean(nodo.derecho));
    return hijos === 0 ? "Nodo hoja" : hijos === 1 ? "Nodo con un hijo" : "Nodo con dos hijos";
  }

  function actualizarTipoEliminacion() {
    const nodo = estado.posiciones.get($("delete-key").value);
    $("delete-info").textContent = nodo ? `${tipoNodo(nodo.dato)}${nodo.depth === 0 ? " · Es la raíz" : ""}. Se aplicará la eliminación recursiva del ABB.` : "Selecciona un término para conocer su tipo de nodo.";
  }
  $("delete-key").addEventListener("change", actualizarTipoEliminacion);
  formulario("delete-form", async datos => {
    if (!await confirmar("¿Eliminar este término?", `Se eliminará «${datos.clave}» del árbol en memoria. Sus hijos se reorganizarán según el algoritmo del ABB.`, "Eliminar término")) return;
    const respuesta = await api(consulta("/api/termino", { clave: datos.clave }), { method: "DELETE" });
    await actualizarTodo();
    mensaje(respuesta.mensaje);
  });

  // Distribución por contornos: separa los nodos de un mismo nivel sin inventar enlaces.
  function distribuir(dato, depth = 0) {
    if (!dato) return null;
    const izquierdo = distribuir(dato.izquierdo, depth + 1);
    const derecho = distribuir(dato.derecho, depth + 1);
    let distancia = 120;
    if (izquierdo && derecho) {
      for (let i = 0; i < Math.min(izquierdo.max.length, derecho.min.length); i++) {
        distancia = Math.max(distancia, (izquierdo.max[i] - derecho.min[i] + 22) / 2);
      }
    }
    const resultado = { nodos: [{ dato, depth, x: 0 }], min: [-91], max: [91] };
    for (const [hijo, desplazamiento] of [[izquierdo, -distancia], [derecho, distancia]]) {
      if (!hijo) continue;
      for (const nodo of hijo.nodos) resultado.nodos.push({ ...nodo, x: nodo.x + desplazamiento });
      for (let i = 0; i < hijo.min.length; i++) {
        resultado.min[i + 1] = Math.min(resultado.min[i + 1] ?? Infinity, hijo.min[i] + desplazamiento);
        resultado.max[i + 1] = Math.max(resultado.max[i + 1] ?? -Infinity, hijo.max[i] + desplazamiento);
      }
    }
    return resultado;
  }

  function lineasEtiqueta(texto) {
    const palabras = texto.match(/.{1,21}(?:\s|$)|\S{1,21}/g) || [texto];
    if (palabras.length <= 2) return palabras.map(palabra => palabra.trim());
    return [palabras[0].trim(), `${palabras[1].trim().slice(0, 18)}…`];
  }

  function renderArbol() {
    limpiarAnimacion();
    const lienzo = $("tree-svg");
    lienzo.replaceChildren();
    estado.nodos.clear();
    estado.posiciones.clear();
    $("tree-empty").hidden = Boolean(estado.raiz);
    if (!estado.raiz) {
      dibujo = null;
      lienzo.setAttribute("width", "1"); lienzo.setAttribute("height", "1");
      estado.seleccionado = null;
      detalleNodo(null);
      return;
    }
    dibujo = distribuir(estado.raiz);
    const minimo = Math.min(...dibujo.min);
    dibujo.width = Math.max(400, Math.max(...dibujo.max) - minimo + 90);
    dibujo.height = dibujo.min.length * 106 + 65;
    dibujo.nodos.forEach(nodo => { nodo.x += 45 - minimo; nodo.y = 54 + nodo.depth * 106; estado.posiciones.set(nodo.dato.termino, nodo); });
    lienzo.setAttribute("viewBox", `0 0 ${dibujo.width} ${dibujo.height}`);
    for (const nodo of dibujo.nodos) {
      for (const hijo of [nodo.dato.izquierdo, nodo.dato.derecho]) {
        if (!hijo) continue;
        const destino = estado.posiciones.get(hijo.termino);
        const medio = (nodo.y + 58 + destino.y) / 2;
        lienzo.append(svg("path", { d: `M ${nodo.x} ${nodo.y + 58} C ${nodo.x} ${medio}, ${destino.x} ${medio}, ${destino.x} ${destino.y}`, class: "tree-edge" }));
      }
    }
    for (const nodo of dibujo.nodos) {
      const grupo = svg("g", { class: `tree-node${nodo.depth === 0 ? " is-root" : ""}`, transform: `translate(${nodo.x - 91},${nodo.y})`, tabindex: "0", role: "button", "aria-label": `${nodo.dato.termino}, ${tipoNodo(nodo.dato)}. Ver detalles.` });
      grupo.append(svg("title", {}, `${nodo.dato.termino} · ${nodo.dato.busquedas} búsquedas`));
      grupo.append(svg("rect", { x: 0, y: 0, width: 182, height: 58, rx: 10 }));
      if (nodo.depth === 0) grupo.append(svg("text", { x: 91, y: -12, "text-anchor": "middle", class: "root-caption" }, "RAÍZ"));
      const lineas = lineasEtiqueta(nodo.dato.termino);
      lineas.forEach((linea, indice) => grupo.append(svg("text", { x: 91, y: lineas.length === 1 ? 25 : 20 + indice * 13, "text-anchor": "middle" }, linea)));
      grupo.append(svg("text", { x: 91, y: 48, "text-anchor": "middle", class: "node-count" }, `${numero.format(nodo.dato.busquedas)} búsquedas`));
      grupo.addEventListener("click", () => seleccionarNodo(nodo.dato.termino));
      grupo.addEventListener("keydown", evento => { if (evento.key === "Enter" || evento.key === " ") { evento.preventDefault(); seleccionarNodo(nodo.dato.termino); } });
      estado.nodos.set(nodo.dato.termino, grupo);
      lienzo.append(grupo);
    }
    aplicarZoom();
    if (estado.seleccionado && estado.posiciones.has(estado.seleccionado)) seleccionarNodo(estado.seleccionado);
    else { estado.seleccionado = null; detalleNodo(null); }
    if (!$("view-arbol").hidden && primerAjuste) centrarVistaInicial();
  }

  function seleccionarNodo(clave) {
    estado.seleccionado = clave;
    estado.nodos.forEach((grupo, termino) => grupo.classList.toggle("is-selected", termino === clave));
    detalleNodo(estado.posiciones.get(clave));
  }

  function detalleNodo(posicion) {
    const contenedor = $("node-detail");
    contenedor.replaceChildren();
    if (!posicion) {
      const vacio = elemento("div", "detail-placeholder", "⑂");
      vacio.append(elemento("h2", "", "Un nodo, una historia."), elemento("p", "", "Selecciona un término para ver sus datos y relaciones."));
      contenedor.append(vacio);
      return;
    }
    const dato = posicion.dato;
    const ficha = elemento("div", "node-record");
    ficha.append(elemento("h2", "", dato.termino), elemento("span", "badge neutral", `${tipoNodo(dato)}${posicion.depth === 0 ? " · raíz" : ""}`));
    const lista = elemento("dl");
    for (const [etiqueta, valor] of [["Popularidad", `${numero.format(dato.busquedas)} búsquedas`], ["Categoría", dato.categoria], ["Última búsqueda", dato.fecha], ["Nivel", posicion.depth]]) {
      const bloque = elemento("div"); bloque.append(elemento("dt", "", etiqueta), elemento("dd", "", String(valor))); lista.append(bloque);
    }
    const relaciones = elemento("div", "node-relations");
    relaciones.append(elemento("div", "", `Izquierdo: ${dato.izquierdo?.termino || "Sin hijo"}`), elemento("div", "", `Derecho: ${dato.derecho?.termino || "Sin hijo"}`));
    ficha.append(lista, relaciones); contenedor.append(ficha);
  }

  function aplicarZoom() {
    if (!dibujo) return;
    $("tree-svg").setAttribute("width", String(dibujo.width * estado.zoom));
    $("tree-svg").setAttribute("height", String(dibujo.height * estado.zoom));
    $("zoom-label").value = `${Math.round(estado.zoom * 100)} %`;
    $("zoom-label").textContent = $("zoom-label").value;
  }

  function centrarVistaInicial() {
    // El texto empieza al 100 %. Ajustar es opcional; el contenedor permite desplazarse.
    estado.zoom = 1;
    aplicarZoom();
    const raiz = estado.posiciones.get(estado.raiz.termino);
    $("tree-viewport").scrollLeft = Math.max(0, raiz.x - $("tree-viewport").clientWidth / 2);
    $("tree-viewport").scrollTop = 0;
    primerAjuste = false;
  }

  function ajustarZoom() {
    if (!dibujo) return;
    estado.zoom = Math.min(1, Math.max(.15, ($("tree-viewport").clientWidth - 12) / dibujo.width));
    aplicarZoom();
    $("tree-viewport").scrollLeft = 0;
    $("tree-viewport").scrollTop = 0;
  }

  function cambiarZoom(cambio) {
    if (!dibujo) return;
    const area = $("tree-viewport");
    const anterior = estado.zoom;
    const centroX = (area.scrollLeft + area.clientWidth / 2) / anterior;
    const centroY = (area.scrollTop + area.clientHeight / 2) / anterior;
    estado.zoom = Math.max(.15, Math.min(2, estado.zoom + cambio));
    aplicarZoom();
    area.scrollLeft = centroX * estado.zoom - area.clientWidth / 2;
    area.scrollTop = centroY * estado.zoom - area.clientHeight / 2;
  }

  $("zoom-fit").addEventListener("click", ajustarZoom);
  $("zoom-in").addEventListener("click", () => cambiarZoom(.15));
  $("zoom-out").addEventListener("click", () => cambiarZoom(-.15));
  $("speed").addEventListener("input", () => { $("speed-label").textContent = `${$("speed").value} ms`; });

  function limpiarAnimacion() {
    versionAnimacion++;
    estado.nodos.forEach(grupo => grupo.classList.remove("is-visited", "is-current"));
    $("animation-status").textContent = "Elige una búsqueda o un recorrido.";
  }

  async function animar(claves, titulo) {
    limpiarAnimacion();
    const version = versionAnimacion;
    navegar("arbol");
    if (!claves.length) { $("animation-status").textContent = "No hay nodos para recorrer."; return; }
    for (let indice = 0; indice < claves.length; indice++) {
      if (version !== versionAnimacion) return;
      estado.nodos.forEach(grupo => grupo.classList.remove("is-current"));
      const grupo = estado.nodos.get(claves[indice]);
      if (grupo) {
        grupo.classList.add("is-visited", "is-current");
        const posicion = estado.posiciones.get(claves[indice]);
        const area = $("tree-viewport");
        area.scrollTo({ left: posicion.x * estado.zoom - area.clientWidth / 2,
          top: posicion.y * estado.zoom - area.clientHeight / 2, behavior: "auto" });
      }
      $("animation-status").textContent = `${titulo}: ${indice + 1}/${claves.length} · ${claves[indice]}`;
      await new Promise(resolve => setTimeout(resolve, matchMedia("(prefers-reduced-motion: reduce)").matches ? 0 : Number($("speed").value)));
    }
    if (version !== versionAnimacion) return;
    estado.nodos.forEach(grupo => grupo.classList.remove("is-current"));
    $("animation-status").textContent = `${titulo} completado · ${claves.length} nodos visitados.`;
  }
  $("stop-animation").addEventListener("click", limpiarAnimacion);
  formulario("path-form", async datos => {
    const respuesta = await api(consulta("/api/camino", datos));
    if (!respuesta.encontrado) mensaje(`No se encontró «${datos.termino}». Se muestra el camino explorado.`);
    await animar(respuesta.camino, "Camino de búsqueda");
  });

  const explicaciones = {
    inorden: "Inorden: izquierda, raíz, derecha. Produce el orden alfabético.",
    preorden: "Preorden: raíz, izquierda, derecha. La raíz se visita primero.",
    postorden: "Postorden: izquierda, derecha, raíz. La raíz se visita al final.",
    niveles: "Por niveles: desde la raíz, utilizando una cola FIFO."
  };
  $("traversal-order").addEventListener("change", () => { $("traversal-explanation").textContent = explicaciones[$("traversal-order").value]; });
  formulario("traversal-form", async datos => {
    const respuesta = await api(consulta("/api/recorrido", datos));
    ultimaSecuencia = respuesta.resultados.map(dato => dato.termino);
    const lista = $("traversal-result"); lista.replaceChildren();
    if (!respuesta.resultados.length) lista.append(elemento("li", "empty-text", "El árbol está vacío."));
    respuesta.resultados.forEach((dato, indice) => {
      const item = elemento("li"); const boton = elemento("button"); boton.type = "button";
      boton.append(elemento("span", "sequence-index", indice + 1), document.createTextNode(dato.termino));
      boton.addEventListener("click", () => { navegar("arbol"); seleccionarNodo(dato.termino); });
      item.append(boton); lista.append(item);
    });
    $("traversal-explanation").textContent = explicaciones[datos.orden];
    $("animate-traversal").disabled = !ultimaSecuencia.length;
  });
  $("animate-traversal").addEventListener("click", manejar(() => animar([...ultimaSecuencia], "Recorrido")));
  $("query-stats").addEventListener("click", manejar(async () => { await actualizarTodo(); mensaje("Estadísticas actualizadas desde Java."); }));
  $("query-extremes").addEventListener("click", manejar(async () => {
    const datos = await api("/api/extremos");
    $("extremes-result").replaceChildren(mostrarRegistro(datos.minimo, "Mínimo"), mostrarRegistro(datos.maximo, "Máximo"));
  }));
  formulario("level-form", async datos => {
    $("level-result").replaceChildren();
    const respuesta = await api(consulta("/api/nivel", datos));
    $("level-result").append(elemento("p", "soft-note", `«${datos.termino}» se encuentra en el nivel ${respuesta.nivel}.`));
  });
  formulario("neighbors-form", async datos => {
    $("neighbors-result").replaceChildren();
    const respuesta = await api(consulta("/api/vecinos", datos));
    $("neighbors-result").append(mostrarRegistro(respuesta.predecesor, "Predecesor"), mostrarRegistro(respuesta.sucesor, "Sucesor"));
  });
  formulario("range-form", async datos => {
    $("range-result").replaceChildren();
    const respuesta = await api(consulta("/api/rango", datos));
    const resultados = elemento("div", "range-results");
    respuesta.resultados.forEach(dato => resultados.append(elemento("span", "", dato.termino)));
    $("range-result").append(elemento("p", "caption", `${respuesta.resultados.length} resultados · ${respuesta.visitas} nodos visitados con poda.`), resultados);
  });
  $("query-top").addEventListener("click", manejar(async () => { const datos = await api("/api/top"); renderTop("query-top-list", datos.resultados); renderTop("home-top", datos.resultados); }));

  $("restore-data").addEventListener("click", manejar(async () => {
    if (!await confirmar("¿Restaurar los datos originales?", "Se reemplazarán todos los términos de esta sesión por los registros originales de terminos.csv. Los cambios en memoria se descartarán.", "Restaurar CSV")) return;
    $("restore-data").disabled = true;
    try {
      const respuesta = await api("/api/restaurar", { method: "POST", json: { confirmacion: "RESTAURAR" } });
      primerAjuste = true;
      await actualizarTodo();
      mensaje(respuesta.mensaje);
    } finally { $("restore-data").disabled = false; }
  }));
  $("empty-data").addEventListener("click", manejar(async () => {
    if (!await confirmar("¿Vaciar todo el árbol?", "Se eliminarán todos los términos en memoria. El archivo CSV permanecerá intacto y podrás restaurarlo después.", "Vaciar árbol")) return;
    $("empty-data").disabled = true;
    try {
      const respuesta = await api("/api/arbol", { method: "DELETE", json: { confirmacion: "VACIAR" } });
      primerAjuste = true;
      await actualizarTodo();
      mensaje(respuesta.mensaje);
    } finally { $("empty-data").disabled = false; }
  }));
  $("load-status").addEventListener("click", manejar(async () => {
    const datos = await api("/api/estado");
    await actualizarTodo();
    mensaje(`CSV disponible: ${datos.registrosCSV} registros. Árbol actual: ${datos.total} términos. Estado: ${datos.carga}.`);
  }));
  $("run-experiment").addEventListener("click", manejar(async () => {
    $("run-experiment").disabled = true;
    try {
      const datos = await api("/api/experimento");
      $("experiment-body").replaceChildren();
      for (const fila of datos.resultados) {
        const tr = elemento("tr");
        for (const valor of [fila.orden, fila.nodos, fila.altura, fila.insercion, fila.totalBusqueda, fila.mediaBusqueda.toFixed(2), fila.maximoBusqueda, fila.ausente]) tr.append(elemento("td", "", String(valor)));
        $("experiment-body").append(tr);
      }
      $("experiment-extra").textContent = datos.resultados.map(fila => `${fila.orden}: prefijo «ja», ${fila.visitasPrefijo} visitas; rango [java, python], ${fila.visitasRango} visitas. Orden ABB ${fila.valido ? "válido" : "inválido"}.`).join(" · ");
      mensaje("Experimento calculado por Java. El árbol de la sesión no se modificó.");
    } finally { $("run-experiment").disabled = false; }
  }));

  $("insert-form").elements.fecha.value = hoy();
  navegar(location.hash.slice(1) || "inicio");
  actualizarTodo().catch(error => mensaje(error.message, true));
})();
