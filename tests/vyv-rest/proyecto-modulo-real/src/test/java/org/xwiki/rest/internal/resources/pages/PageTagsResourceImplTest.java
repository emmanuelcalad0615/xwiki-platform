/*
 * PLANTILLA (vacia) - Pruebas unitarias de PageTagsResourceImpl.
 * Funcionalidad: "page tags" (leer y asignar las etiquetas de una pagina).
 *
 * El responsable de esta funcionalidad implementa aqui sus casos.
 * El package DEBE ser el mismo que el de la clase real.
 * Patron de referencia: la clase de pruebas de comentarios (paquete comments).
 */
package org.xwiki.rest.internal.resources.pages;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

@ComponentTest
class PageTagsResourceImplTest
{
    /** Clase REAL bajo prueba, inyectada por el framework de XWiki. */
    @InjectMockComponents
    private PageTagsResourceImpl resource;

    /**
     * Pendiente de implementar. Metodos a probar: getPageTags y setTags.
     * Sugerencia: simular el acceso a XWiki y el documento de la pagina; para getPageTags cubrir
     * pagina sin etiquetas y pagina con etiquetas; para setTags cubrir el guardado y los permisos.
     * Quita la anotacion Disabled cuando lo implementes.
     */
    @Test
    @Disabled("Pendiente de implementar")
    void pendiente()
    {
        // El responsable de page tags escribe sus casos en este metodo.
    }
}
