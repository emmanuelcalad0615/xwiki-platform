// Stagehand v3 + gpt-oss-20b a veces devuelve el campo interno "completed" como
// string ("true") en vez de boolean, lo que rompe la validacion del extract de
// forma INTERMITENTE (NO es falta de tokens; el dato si se extrajo). Reintentar
// resuelve porque el modelo suele devolver el boolean correcto al segundo intento.
export async function extraerConReintento(
  sh: any,
  instruccion: string,
  schema: any,
  intentos = 3,
): Promise<any> {
  let ultimo: any;
  for (let i = 1; i <= intentos; i++) {
    try {
      return await sh.extract(instruccion, schema);
    } catch (e) {
      ultimo = e;
      console.log(`extract intento ${i}/${intentos} fallo (${(e as Error)?.name}); reintentando...`);
      await new Promise((r) => setTimeout(r, 2000));
    }
  }
  throw ultimo;
}
