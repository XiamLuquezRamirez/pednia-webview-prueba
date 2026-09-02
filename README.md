# PedNia Kiosco — WebView de prueba (cámara embebida)

APK mínima para probar que la **cámara y el micrófono se muestren embebidos**
dentro de la plataforma PedNia (no a pantalla completa), sirviendo el servidor
Laravel de la red local bajo un **origen HTTPS virtual** (contexto seguro para
`getUserMedia`).

---

## ⚙️ ANTES DE COMPILAR: cambia la IP de tu servidor

Abre `app/src/main/java/com/pednia/kiosco/MainActivity.kt` y ajusta esta línea
con la IP:puerto de tu Laravel en la red local:

```kotlin
private const val SERVIDOR_LAN = "192.168.1.15:8000"
```

(Debe ser la IP de la PC que corre `php artisan serve --host=0.0.0.0`, accesible
desde la tablet en la misma WiFi.)

---

## 📦 Cómo obtener la APK (sin instalar nada — GitHub Actions)

1. Crea un repositorio nuevo en GitHub (puede ser privado).
2. Sube **todo el contenido de esta carpeta** al repo (rama `main`):
   ```bash
   git init
   git add .
   git commit -m "WebView PedNia de prueba"
   git branch -M main
   git remote add origin https://github.com/TU_USUARIO/TU_REPO.git
   git push -u origin main
   ```
3. En GitHub, ve a la pestaña **Actions**. El workflow "Compilar APK PedNia" se
   ejecuta solo al hacer push (o dale **Run workflow** manualmente).
4. Cuando termine (2–4 min), entra al run y descarga el artefacto
   **`pednia-kiosco-debug-apk`** — dentro está `app-debug.apk`.

## 📲 Instalar en la tablet

1. Pasa el `app-debug.apk` a la tablet (cable, USB, o descarga directa).
2. En la tablet, permite "instalar apps de orígenes desconocidos" y ábrela.
3. Al abrir, acepta los permisos de **cámara** y **micrófono**.
4. La app carga PedNia. Ve a un bloque de **evidencia** (foto/audio/video) y
   comprueba que la cámara aparece **en el recuadro dentro de la plataforma**.

## ✔️ Qué valida esta prueba

- Si la cámara sale **embebida** → el enfoque de origen seguro funciona; se puede
  llevar al WebView de producción del equipo.
- Si sigue el aviso de "conexión segura" → revisar que la IP sea correcta y que
  la tablet llegue al servidor por esa IP.

## 🗂️ Qué hace el código (resumen)

- `MainActivity.kt` — WebView + `WebViewAssetLoader` que sirve la LAN bajo
  `https://appassets.androidplatform.net` (origen seguro) mediante un proxy que
  propaga método/cabeceras/cookies (para el login del niño por PIN y los POST).
- `onPermissionRequest.grant(...)` — concede cámara/micrófono a la página.
- Permisos `CAMERA` y `RECORD_AUDIO` en el Manifest + petición en runtime.

> Es un proyecto de **prueba**. Para producción, el equipo Android integra esta
> misma técnica (origen seguro + proxy) en su WebView definitivo.
