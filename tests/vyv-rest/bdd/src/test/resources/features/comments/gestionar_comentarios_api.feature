# language: es
Característica: Gestión de comentarios de una página mediante API REST
  Como usuario del sistema XWiki
  Quiero consultar, crear y obtener los comentarios de una página a través de la API REST
  Para gestionar la conversación sobre el contenido de forma automatizada

  Escenario: Consultar los comentarios de una página que no tiene comentarios
    Dado que existe una página "WebHome" en el espacio "Main" de la wiki "xwiki"
    Y la página no tiene ningún comentario
    Cuando realizo una petición GET al endpoint de comentarios de la página
    Entonces recibo un código de estado 200 OK
    Y la respuesta contiene una lista vacía de comentarios

  Escenario: Consultar los comentarios de una página que ya tiene comentarios
    Dado que existe una página "WebHome" en el espacio "Main"
    Y la página tiene los comentarios "Buen articulo" y "Muy util, gracias"
    Cuando realizo una petición GET al endpoint de comentarios de la página
    Entonces recibo un código de estado 200 OK
    Y la respuesta contiene los comentarios "Buen articulo" y "Muy util, gracias"

  Escenario: Crear un comentario exitosamente
    Dado que soy un administrador autenticado
    Y existe una página "WebHome" en el espacio "Main"
    Cuando realizo una petición POST para crear el comentario "Comentario de prueba BDD"
    Entonces recibo un código de estado 201 Created
    Y al consultar los comentarios de la página, la lista incluye "Comentario de prueba BDD"

  Escenario: Rechazar la creación de un comentario sin autenticación
    Dado que soy un usuario anónimo (sin credenciales)
    Y existe una página "WebHome" en el espacio "Main"
    Cuando realizo una petición POST para crear el comentario "Comentario intruso"
    Entonces el servidor rechaza la petición por falta de autorización

  Escenario: Crear un comentario con cuerpo vacío (Regresión)
    Dado que soy un administrador autenticado
    Y existe una página "WebHome" en el espacio "Main"
    Cuando realizo una petición POST para crear un comentario sin contenido
    Entonces el sistema responde con un código de estado 204 No Content

  Escenario: Obtener un comentario que no existe
    Dado que soy un administrador autenticado
    Y la página tiene los comentarios "Unico comentario"
    Cuando realizo una petición GET para obtener el comentario con id 999
    Entonces recibo un código de estado 404 Not Found
