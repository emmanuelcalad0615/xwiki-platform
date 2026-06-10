# language: es
Característica: Seguridad y Autorización en la gestión de etiquetas
  Como administrador del sistema XWiki
  Quiero que la gestión de etiquetas esté protegida por permisos
  Para evitar que usuarios no autorizados modifiquen la categorización del contenido

  Escenario: Intento de modificación de etiquetas sin autenticación
    Dado que soy un usuario anónimo (sin credenciales)
    Y existe una página "WebHome" en el espacio "Main"
    Cuando realizo una petición PUT al endpoint de etiquetas para añadir "hacker-tag"
    Entonces recibo un código de estado 401 Unauthorized
    Y las etiquetas de la página no son modificadas en el sistema

  Escenario: Intento de modificación de etiquetas con credenciales inválidas o sin permisos
    Dado que soy un usuario autenticado pero sin permisos de edición (ej. Guest o usuario sin privilegios)
    Y existe una página "WebHome" en el espacio "Main"
    Cuando realizo una petición PUT al endpoint de etiquetas para añadir "hacker-tag"
    Entonces el servidor rechaza la petición por falta de autorización
    Y las etiquetas de la página no son modificadas
