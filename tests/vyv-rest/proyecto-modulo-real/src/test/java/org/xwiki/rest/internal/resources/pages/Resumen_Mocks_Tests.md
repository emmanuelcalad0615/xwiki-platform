# Resumen de Tests y Tipos de Mocks - Gestión de Etiquetas (Pages)

En este documento se describe cada archivo de prueba dentro de la carpeta `pages`, indicando la funcionalidad que prueba, el tipo de mock que utiliza (siguiendo los patrones de prueba de software) y cómo aplica el patrón AAA (Arrange, Act, Assert).

## 1. `AbstractPageTagsResourceTest.java`
* **Descripción:** Es la clase base abstracta de la cual heredan todas las pruebas de la gestión de etiquetas. Define constantes compartidas (como `WIKI`, `SPACE`, `PAGE_ID`) y configura el entorno inicial común.
* **Tipos de Mocks usados:** 
  * **Stub:** Se usa `mock(UriInfo.class)` para simular la petición web y se le programa un comportamiento fijo (`when(...).thenReturn(...)`) para que devuelva una URL base constante.
  * **Inyección de Componentes:** Utiliza `@InjectMockComponents` para inyectar automáticamente los mocks en la clase a probar (`PageTagsResourceImpl`).

## 2. `PageTagsResourceImplGetEmptyTest.java`
* **Descripción:** Prueba los escenarios donde se solicita obtener las etiquetas de una página, pero esta página no tiene etiquetas (ya sea porque el objeto no existe, la propiedad es nula, o su valor es nulo).
* **Tipos de Mocks usados:**
  * **Stubs:** Usa extensivamente `mock(Clase.class)` y `mockStatic(Utils.class)` programándolos con `when()` para que devuelvan respuestas controladas (`null`, o instancias vacías de `BaseObject`) sin verificar cómo fueron llamados. Su objetivo es preparar el estado del sistema para el test (Fase *Arrange*).

## 3. `PageTagsResourceImplGetSuccessTest.java`
* **Descripción:** Valida que el sistema pueda obtener correctamente una lista de etiquetas cuando la página sí las tiene, y comprueba el manejo de excepciones cuando el motor de XWiki falla internamente.
* **Tipos de Mocks usados:**
  * **Spy (Espía):** Utiliza `spy(new ObjectFactory())`. A diferencia del stub, el espía envuelve un objeto real, permitiendo observar si se llamaron a sus métodos (`createTags()`, `createTag()`). En la fase *Assert*, se usa `verify(spyFactory)` para confirmar su comportamiento.
  * **Stubs y Mocks estáticos:** Para forzar el escenario de éxito o lanzar la excepción esperada (`XWikiRestException`).

## 4. `PageTagsResourceImplSetErrorTest.java`
* **Descripción:** Prueba todos los flujos de error al intentar modificar (guardar) etiquetas en una página. Valida permisos insuficientes (error 401), fallos al crear objetos internos (error 500) y documentos no encontrados.
* **Tipos de Mocks usados:**
  * **Dummy:** Se utiliza `Tags dummyTags = new Tags()`. Este objeto se pasa como parámetro obligatorio al método `setTags`, pero su contenido real nunca es usado ni evaluado por el test porque la prueba falla antes (por ejemplo, por falta de permisos). Es un objeto "tonto" solo para cumplir la firma del método.
  * **Mock / Stub:** Se usan mocks estáticos para simular que el usuario actual es un "Guest" (Invitado) y forzar la caída por falta de permisos.

## 5. `PageTagsResourceImplSetSuccessTest.java`
* **Descripción:** Asegura que si se proveen etiquetas válidas y el usuario tiene permisos, los cambios se guardan exitosamente en la base de datos de XWiki, ya sea sobrescribiendo un objeto de etiquetas existente o creando uno nuevo.
* **Tipos de Mocks usados:**
  * **Fake:** En la segunda prueba se utiliza `FakeTags fakeTags = new FakeTags("java", "testing")`. A diferencia de un Dummy, el objeto Fake sí tiene estado o comportamiento simplificado que el método bajo prueba va a leer para extraer los datos ("java", "testing").
  * **Mocks de Verificación:** Al final del flujo (fase *Assert*), se utilizan las funciones `verify(...)` sobre los mocks de `XWikiDocument` y `XWiki` para confirmar que se llamó a la función de guardado `saveDocument` con los parámetros correctos.
