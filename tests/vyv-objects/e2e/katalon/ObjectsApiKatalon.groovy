/*
 * Katalon Studio - Test Case (modo Script) para la funcionalidad "objects".
 *
 * Mismo ciclo de vida que la suite Cypress (objects-api.cy.js), implementado
 * con los keywords WS de Katalon: crear pagina -> crear objeto -> GET 200 ->
 * PUT 202 -> DELETE -> GET 404.
 *
 * Uso: en Katalon Studio crear un Test Case nuevo, cambiar a la pestana
 * "Script" y pegar este contenido. Requiere el XWiki de docker-compose en
 * http://localhost:8080 con el flavor instalado (Admin/admin).
 */
import static org.assertj.core.api.Assertions.assertThat

import com.kms.katalon.core.testobject.RequestObject
import com.kms.katalon.core.testobject.ResponseObject
import com.kms.katalon.core.testobject.RestRequestObjectBuilder
import com.kms.katalon.core.webservice.keyword.WSBuiltInKeywords as WS

String base    = 'http://localhost:8080'
String espacio = 'VyVObjectsKatalon'
String pagina  = 'PruebaObjetos'
String clase   = 'XWiki.TagClass'
String urlPagina  = "${base}/rest/wikis/xwiki/spaces/${espacio}/pages/${pagina}"
String credencial = 'Basic ' + Base64.encoder.encodeToString('Admin:admin'.bytes)

def peticion = { String metodo, String url, String tipo, String cuerpo ->
    RestRequestObjectBuilder b = new RestRequestObjectBuilder()
        .withRestRequestMethod(metodo)
        .withRestUrl(url)
        .withHttpHeaders([
            new com.kms.katalon.core.testobject.TestObjectProperty('Authorization', com.kms.katalon.core.testobject.ConditionType.EQUALS, credencial),
            new com.kms.katalon.core.testobject.TestObjectProperty('Accept', com.kms.katalon.core.testobject.ConditionType.EQUALS, 'application/json')
        ])
    if (cuerpo != null) {
        b.withHttpHeaders([
            new com.kms.katalon.core.testobject.TestObjectProperty('Authorization', com.kms.katalon.core.testobject.ConditionType.EQUALS, credencial),
            new com.kms.katalon.core.testobject.TestObjectProperty('Accept', com.kms.katalon.core.testobject.ConditionType.EQUALS, 'application/json'),
            new com.kms.katalon.core.testobject.TestObjectProperty('Content-Type', com.kms.katalon.core.testobject.ConditionType.EQUALS, tipo)
        ])
        b.withTextBodyContent(cuerpo)
    }
    RequestObject req = b.build()
    return WS.sendRequest(req)
}

// 1. Arrange: crear la pagina de trabajo
ResponseObject rPagina = peticion('PUT', urlPagina, 'application/xml',
    '<?xml version="1.0" encoding="UTF-8"?><page xmlns="http://www.xwiki.org"><title>Katalon objects</title><content>.</content></page>')
WS.verifyStatusCode(rPagina, rPagina.statusCode in [201, 202] ? rPagina.statusCode : 201)

// 2. Crear el objeto TagClass
ResponseObject rCrear = peticion('POST', "${urlPagina}/objects",
    'application/x-www-form-urlencoded', 'className=XWiki.TagClass&property%23tags=katalon-vyv')
WS.verifyStatusCode(rCrear, 201)
int numero = new groovy.json.JsonSlurper().parseText(rCrear.responseText).number

// 3. GET del objeto (camino exito de getObject)
ResponseObject rGet = peticion('GET', "${urlPagina}/objects/${clase}/${numero}", null, null)
WS.verifyStatusCode(rGet, 200)
def objeto = new groovy.json.JsonSlurper().parseText(rGet.responseText)
assert objeto.className == clase
assert objeto.number == numero

// 4. PUT de actualizacion (camino 202 de updateObject)
ResponseObject rPut = peticion('PUT', "${urlPagina}/objects/${clase}/${numero}",
    'application/x-www-form-urlencoded', 'property%23tags=katalon-actualizado')
WS.verifyStatusCode(rPut, 202)

// 5. DELETE (camino exito de deleteObject) y verificacion 404 posterior
ResponseObject rDel = peticion('DELETE', "${urlPagina}/objects/${clase}/${numero}", null, null)
assert rDel.statusCode in [200, 204]
ResponseObject r404 = peticion('GET', "${urlPagina}/objects/${clase}/${numero}", null, null)
WS.verifyStatusCode(r404, 404)

// 6. Limpieza
peticion('DELETE', urlPagina, null, null)
