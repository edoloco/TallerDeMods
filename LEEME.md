# Taller de Mods

App de Android para crear addons de Minecraft (Bedrock): criaturas, bloques y objetos.

## Para el papá: conseguir el APK (sin instalar nada)

1. Entra a github.com e inicia sesión.
2. Crea un repositorio nuevo (botón "New"), por ejemplo `taller-de-mods`.
3. Sube TODO el contenido de esta carpeta, incluida la carpeta oculta `.github`.
   La forma más fácil es abrir el repositorio vacío, elegir "uploading an existing file" y arrastrar los archivos.
   Si la carpeta `.github` no se sube desde el navegador, usa GitHub Desktop o `git push`.
4. Ve a la pestaña **Actions**. Se ejecuta "Crear APK" solo. Espera unos minutos hasta que salga la palomita verde.
5. Lo más fácil: ve a **Releases** (a la derecha en la página del repositorio) y descarga `TallerDeMods.apk`. También está en **Artifacts** de cada ejecución.
6. Descomprime el zip y copia el archivo `.apk` al teléfono.
7. En el teléfono, abre el APK y permite "instalar apps desconocidas" si lo pide.

## Actualizaciones (se instalan desde la misma app)

- La app revisa la pestaña **Releases** del repositorio cada vez que se abre. Si hay una versión más nueva, aparece el aviso "¡Hay una versión nueva!" con el botón **Actualizar ahora**.
- La primera vez Android pide el permiso "Permitir de esta fuente": se activa una sola vez.
- Las creaciones del niño no se borran al actualizar.

**Para publicar una versión nueva:** cambia el número en `version.txt` (por ejemplo de `1.0.0` a `1.0.1`) y súbelo a `main`. GitHub compila y crea el Release solo; el mensaje del commit aparece como novedades en la app.

Otras formas que también funcionan:

Desde la web (sirve en el teléfono): pestaña **Releases** → **Draft a new release** → en "Choose a tag" escribe la versión nueva (por ejemplo `v1.0.1`) y elige "Create new tag" → escribe qué cambió → **Publish release**. En unos 3 minutos GitHub le agrega `TallerDeMods.apk` solo.

O desde la terminal:

```
git tag -a v1.0.1 -m "Qué cambió en esta versión"
git push origin v1.0.1
```

GitHub Actions compila, crea el Release con `TallerDeMods.apk` y el mensaje del tag aparece como novedades en la app. El número siempre tiene que subir (v1.0.1, v1.0.2, v1.1.0…).

**Firma:** todas las APK se firman con `keystore/tallerdemods.jks`. No borres ese archivo: si la firma cambia, Android no deja instalar encima y habría que desinstalar (y se perderían las creaciones). Si alguna vez instalaste una APK anterior a v1.0.0, desinstálala una vez e instala la v1.0.0.

## Para el niño

1. Abre Taller de Mods.
2. Crea criaturas, bloques y objetos.
3. En "Mi addon" toca **Enviar a Minecraft**. Se abre Minecraft e importa tu addon.
4. En Minecraft, crea o edita un mundo, entra a Complementos y activa los dos paquetes (el de comportamiento y el de recursos).
5. Busca tus huevos de aparición en el inventario creativo, o usa el comando que aparece en "Mi addon" (por ejemplo `/summon custom:mi_criatura`).

## Dibujar (editor de pixeles)

Al crear un **bloque** u **objeto** aparece "🎨 Dibújalo tú": un lienzo de 16×16 con lápiz, rellenar, borrar, copiar color, espejo, deshacer y paleta. El dibujo se usa como textura en Minecraft. En "Mi addon" el botón ✏️ permite cambiar cualquier cosa ya creada.

## Pintar criaturas

En la criatura, "🎨 Píntala tú": se elige parte (cabeza, cuerpo, brazos, piernas) y cara (frente, espalda, lados, arriba) y se pinta pixel por pixel, con vista previa de frente y espalda. Al pintarla, la criatura usa forma de persona (modelo de zombi), con el tamaño, fuerza y poderes elegidos.

## Formas 3D

En la portada, "🧊 Formas 3D": se construye una figura con cubitos, capa por capa. El tamaño se elige: ancho 16, 24 o 32 y alto 16, 24, 32, 48 o 64 capas (16 = un bloque). Si es más ancha que 16, la cuadrícula muestra una ventana de 16×16 que se mueve con flechas, con un mapa chico de la capa entera (como construir en Minecraft). Se ve la capa de abajo en transparente y una vista 3D que se gira con el dedo. Herramientas: poner, quitar, rellenar, copiar color, espejo, deshacer, copiar capa de abajo y vaciar capa. Hay plantillas para empezar (esfera, pirámide, hongo, mesa).

Las formas se usan en:
- **Bloques**: el bloque toma esa figura. Minecraft solo deja bloques de hasta 30 píxeles, así que los bloques aceptan formas de hasta 24×24×24.
- **Criaturas**: cualquier tamaño (64 capas = 4 bloques de alto); además se puede agrandar con la opción de tamaño.

La app convierte los cubitos en un modelo 3D de Minecraft (geometría con cajas unidas) y una textura con los colores.

## Poderes de los objetos

- Espadas y objetos: **al usarlo** (bola de fuego, bola de nieve, flecha, rayo, explosión, teletransporte, supersalto, correr rápido, curarse, llamar un gólem) y **al golpear** (quemar, envenenar, congelar, marchitar, rayo, mandar al aire, curarse).
- Comida: **poder al comerla** (velocidad, salto, ver de noche, invisible, regenerar, fuerza, no quemarse, respirar bajo el agua, flotar).

Funcionan con scripts, igual que las reglas de magia.

## Si algo sale invisible o con "?"

Casi siempre es porque el paquete **Apariencia** no está activo en el mundo (o quedó activo uno viejo). En el mundo: Editar → Paquetes de recursos → Activos debe tener "<nombre> - Apariencia (vN)". Los paquetes viejos que se llamaban "BP"/"RP" conviene borrarlos en Configuración → Almacenamiento. La app tiene esta misma ayuda en "Mi addon".

## Reglas de magia (estilo MCreator, más sencillo)

En la pantalla principal, **Crear una regla**: eliges *cuándo pasa algo* (alguien usa mi objeto, toca/rompe/pone mi bloque, mi criatura muere, la golpean o golpea, un jugador entra al mundo, cada cierto tiempo) y *qué pasa* (mensaje, dar un poder, dar un objeto, invocar una criatura, explosión, rayo, fuego, curar, hacer daño, subir al cielo, sonido, partículas).
Las reglas usan scripts de Minecraft (`@minecraft/server`), por eso conviene tener Minecraft actualizado.
Si Minecraft dice que falta una dependencia o no carga el addon con reglas, avísale a Claude el mensaje exacto: se puede cambiar la versión del script en la app (constante `SCRIPT_VER`).

## Si algo falla

- Si el APK no compila, copia el error rojo de la pestaña Actions y se lo mandas a Claude para que lo arregle.
- Si Minecraft no importa el addon, dile qué mensaje salió.
- Esta app no se probó en un teléfono real antes de entregarse.
