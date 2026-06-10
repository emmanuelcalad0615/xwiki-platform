# language: es
Característica: Gestión de etiquetas de una página mediante API REST
  Como usuario del sistema XWiki
  Quiero poder consultar y modificar las etiquetas de una página a través de la API REST
  Para poder categorizar y organizar el contenido de forma automatizada

  Escenario: Consultar etiquetas de una página que no tiene etiquetas
    Dado que existe una página "WebHome" en el espacio "Main" de la wiki "xwiki"
    Y la página no tiene ninguna etiqueta asignada
    Cuando realizo una petición GET al endpoint de etiquetas de la página
    Entonces recibo un código de estado 200 OK
    Y la respuesta contiene una lista vacía de etiquetas

  Escenario: Consultar etiquetas de una página que ya tiene etiquetas
    Dado que existe una página "WebHome" en el espacio "Main" de la wiki "xwiki"
    Y la página tiene asignadas las etiquetas "documentacion" y "guia"
    Cuando realizo una petición GET al endpoint de etiquetas de la página
    Entonces recibo un código de estado 200 OK
    Y la respuesta contiene las etiquetas "documentacion" y "guia"

  Escenario: Añadir nuevas etiquetas a una página exitosamente
    Dado que soy un administrador autenticado
    Y existe una página "WebHome" en el espacio "Main"
    Cuando realizo una petición PUT al endpoint de etiquetas con el cuerpo:
      """
      {
        "tags": [
          {"name": "nueva-etiqueta"}
        ]
      }
      """
    Entonces recibo un código de estado 202 Accepted
    Y al consultar las etiquetas de la página, la lista incluye "nueva-etiqueta"

  Escenario: Manejo de etiquetas duplicadas en la petición (Regresión)
    Dado que soy un administrador autenticado
    Y existe una página "WebHome" en el espacio "Main"
    Cuando realizo una petición PUT al endpoint de etiquetas enviando "etiqueta-duplicada" dos veces
    Entonces recibo un código de estado 202 Accepted
    Y el sistema procesa la petición correctamente sin generar excepciones
