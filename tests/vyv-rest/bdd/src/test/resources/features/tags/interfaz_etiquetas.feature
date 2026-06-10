# language: es
Característica: Gestión de etiquetas desde la Interfaz Gráfica (UI)
  Como usuario editor de XWiki
  Quiero poder añadir y visualizar etiquetas directamente desde la interfaz web
  Para categorizar las páginas de forma visual e intuitiva sin depender de la API

  Escenario: Añadir una etiqueta exitosamente usando el formulario dinámico de XWiki
    Dado que he iniciado sesión exitosamente con credenciales válidas en la interfaz web
    Y navego a la página "WebHome" del espacio "Main"
    Cuando hago clic en el botón de añadir etiqueta (icono de enlace o suma en la sección de etiquetas)
    Y el sistema carga el formulario de entrada de etiquetas dinámicamente vía AJAX
    Y escribo una etiqueta única en el campo de texto y presiono Enter
    Entonces el sistema envía la etiqueta al servidor
    Y al recargar la página web por completo
    Y la nueva etiqueta se muestra persistida en el DOM dentro de la sección de etiquetas
