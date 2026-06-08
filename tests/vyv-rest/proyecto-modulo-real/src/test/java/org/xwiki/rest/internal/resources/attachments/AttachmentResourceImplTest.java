/*
 * PLANTILLA (vacia) - Pruebas unitarias de AttachmentResourceImpl.
 * Funcionalidad: "attachments" (ver, subir y borrar un adjunto de una pagina).
 *
 * El responsable de esta funcionalidad implementa aqui sus casos.
 * El package DEBE ser el mismo que el de la clase real.
 * Patron de referencia: la clase de pruebas de comentarios (paquete comments).
 */
package org.xwiki.rest.internal.resources.attachments;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

@ComponentTest
class AttachmentResourceImplTest
{
    /** Clase REAL bajo prueba, inyectada por el framework de XWiki. */
    @InjectMockComponents
    private AttachmentResourceImpl resource;

    /**
     * Pendiente de implementar. Metodos a probar: getAttachment, putAttachment y deleteAttachment.
     * Sugerencia: simular el acceso a XWiki, el documento y su adjunto; cubrir el caso en que el
     * adjunto no existe (respuesta 404) y la validacion de permisos de edicion.
     * Quita la anotacion Disabled cuando lo implementes.
     */
    @Test
    @Disabled("Pendiente de implementar")
    void pendiente()
    {
        // El responsable de attachments escribe sus casos en este metodo.
    }
}
