# Taller de Mods

App de Android para crear addons de Minecraft (Bedrock): criaturas, bloques y objetos.

## Para el papá: conseguir el APK (sin instalar nada)

1. Entra a github.com e inicia sesión.
2. Crea un repositorio nuevo (botón "New"), por ejemplo `taller-de-mods`.
3. Sube TODO el contenido de esta carpeta, incluida la carpeta oculta `.github`.
   La forma más fácil es abrir el repositorio vacío, elegir "uploading an existing file" y arrastrar los archivos.
   Si la carpeta `.github` no se sube desde el navegador, usa GitHub Desktop o `git push`.
4. Ve a la pestaña **Actions**. Se ejecuta "Crear APK" solo. Espera unos minutos hasta que salga la palomita verde.
5. Abre esa ejecución y baja a **Artifacts**. Descarga `TallerDeMods-APK`.
6. Descomprime el zip y copia el archivo `.apk` al teléfono.
7. En el teléfono, abre el APK y permite "instalar apps desconocidas" si lo pide.

## Para el niño

1. Abre Taller de Mods.
2. Crea criaturas, bloques y objetos.
3. En "Mi addon" toca **Enviar a Minecraft**. Se abre Minecraft e importa tu addon.
4. En Minecraft, crea o edita un mundo, entra a Complementos y activa los dos paquetes (el de comportamiento y el de recursos).
5. Busca tus huevos de aparición en el inventario creativo, o usa el comando que aparece en "Mi addon" (por ejemplo `/summon custom:mi_criatura`).

## Reglas de magia (estilo MCreator, más sencillo)

En la pantalla principal, **Crear una regla**: eliges *cuándo pasa algo* (alguien usa mi objeto, toca/rompe/pone mi bloque, mi criatura muere, la golpean o golpea, un jugador entra al mundo, cada cierto tiempo) y *qué pasa* (mensaje, dar un poder, dar un objeto, invocar una criatura, explosión, rayo, fuego, curar, hacer daño, subir al cielo, sonido, partículas).
Las reglas usan scripts de Minecraft (`@minecraft/server`), por eso conviene tener Minecraft actualizado.
Si Minecraft dice que falta una dependencia o no carga el addon con reglas, avísale a Claude el mensaje exacto: se puede cambiar la versión del script en la app (constante `SCRIPT_VER`).

## Si algo falla

- Si el APK no compila, copia el error rojo de la pestaña Actions y se lo mandas a Claude para que lo arregle.
- Si Minecraft no importa el addon, dile qué mensaje salió.
- Esta app no se probó en un teléfono real antes de entregarse.
