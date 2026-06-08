/*
 * PLANTILLA (vacia) - Pruebas unitarias de ObjectResourceImpl.
 * Funcionalidad: "objects" (ver, actualizar y borrar un objeto de una pagina).
 *
 * El responsable de esta funcionalidad implementa aqui sus casos.
 * El package DEBE ser el mismo que el de la clase real.
 * Patron de referencia: la clase de pruebas de comentarios (paquete comments).
 */
package org.xwiki.rest.internal.resources.objects;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

@ComponentTest
class ObjectResourceImplTest
{
    /** Clase REAL bajo prueba, inyectada por el framework de XWiki. */
    @InjectMockComponents
    private ObjectResourceImpl resource;

    /**
     * Pendiente de implementar. Metodos a probar: getObject, updateObject y deleteObject.
     * Sugerencia: declarar ModelFactory y ContextualAuthorizationManager con MockComponent;
     * para update y delete cubrir sin permiso (401), objeto inexistente (404) y exito (verifica
     * el guardado o el borrado del documento). Es la funcionalidad mas rica para los 5 dobles.
     * Quita la anotacion Disabled cuando lo implementes.
     */
    @Test
    @Disabled("Pendiente de implementar")
    void pendiente()
    {
        // El responsable de objects escribe sus casos en este metodo.
    }
}
