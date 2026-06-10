# language: es
Característica: Rendimiento en la gestión de etiquetas
  Como administrador del sistema XWiki
  Quiero que la lectura y escritura de etiquetas sea muy rápida
  Para que la categorización automatizada no afecte el rendimiento del servidor

  Escenario: Tiempo de respuesta al consultar etiquetas
    Dado que existe una página "WebHome" en el espacio "Main" de la wiki "xwiki"
    Y la página tiene asignadas las etiquetas "documentacion" y "guia"
    Cuando realizo una petición GET al endpoint de etiquetas de la página
    Entonces recibo un código de estado 200 OK
    Y el tiempo de respuesta de la API es menor a 200 milisegundos

  Escenario: Tiempo de respuesta al añadir nuevas etiquetas
    Dado que soy un administrador autenticado
    Y existe una página "WebHome" en el espacio "Main"
    Cuando realizo una petición PUT al endpoint de etiquetas para añadir "rendimiento"
    Entonces recibo un código de estado 202 Accepted
    Y el tiempo de respuesta de la API es menor a 200 milisegundos
