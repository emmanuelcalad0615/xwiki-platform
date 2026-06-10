# language: es
# Gherkin completo de la funcionalidad "objects" (ObjectResourceImpl):
# consultar, actualizar y eliminar un objeto de una pagina via API REST.
# Cada escenario se ejecuta con Cucumber JVM contra la clase REAL de XWiki 18.4.0,
# con el wiki simulado mediante dobles de prueba (patron Screenplay).

Característica: Gestión de objetos de una página por la API REST de XWiki
  Como editor del wiki
  Quiero consultar, actualizar y eliminar los objetos de una página
  Para administrar sus datos estructurados sin tocar el resto del contenido

  Antecedentes:
    Dado que la página "Main.PruebaObjetos" del wiki "xwiki" existe

  Escenario: Consultar un objeto existente
    Dado que la página tiene un objeto "XWiki.TagClass" con número 0
    Cuando el editor consulta el objeto "XWiki.TagClass" número 0
    Entonces recibe el objeto con clase "XWiki.TagClass" y número 0

  Escenario: Consultar un objeto que no existe
    Dado que la página tiene un objeto "XWiki.TagClass" con número 0
    Cuando el editor consulta el objeto "XWiki.TagClass" número 99
    Entonces la operación falla con el código HTTP 404

  Escenario: Actualizar un objeto sin permiso de edición
    Dado que la página tiene un objeto "XWiki.TagClass" con número 0
    Y que el editor no tiene permiso de edición sobre la página
    Cuando el editor actualiza el objeto "XWiki.TagClass" número 0
    Entonces la operación falla con el código HTTP 401
    Y la página no se guarda

  Escenario: Actualizar un objeto existente con permiso de edición
    Dado que la página tiene un objeto "XWiki.TagClass" con número 0
    Y que el editor tiene permiso de edición sobre la página
    Cuando el editor actualiza el objeto "XWiki.TagClass" número 0
    Entonces la operación responde con el código HTTP 202
    Y la página se guarda con los cambios

  Escenario: Actualizar un objeto que no existe
    Dado que el editor tiene permiso de edición sobre la página
    Cuando el editor actualiza el objeto "XWiki.TagClass" número 99
    Entonces la operación falla con el código HTTP 404
    Y la página no se guarda

  Escenario: Eliminar un objeto existente
    Dado que la página tiene un objeto "XWiki.TagClass" con número 0
    Y que el editor tiene permiso de edición sobre la página
    Cuando el editor elimina el objeto "XWiki.TagClass" número 0
    Entonces el objeto "XWiki.TagClass" número 0 ya no está en la página
    Y la página se guarda

  Escenario: Eliminar un objeto sin permiso de edición
    Dado que la página tiene un objeto "XWiki.TagClass" con número 0
    Y que el editor no tiene permiso de edición sobre la página
    Cuando el editor elimina el objeto "XWiki.TagClass" número 0
    Entonces la operación falla con el código HTTP 401
    Y el objeto "XWiki.TagClass" número 0 sigue en la página

  Escenario: Eliminar un objeto que no existe
    Dado que el editor tiene permiso de edición sobre la página
    Cuando el editor elimina el objeto "XWiki.TagClass" número 99
    Entonces la operación falla con el código HTTP 404
    Y la página no se guarda

  Escenario: El almacenamiento del wiki falla durante la consulta
    Dado que el almacenamiento del wiki está fallando
    Cuando el editor consulta el objeto "XWiki.TagClass" número 0
    Entonces la operación falla con un error interno del wiki
