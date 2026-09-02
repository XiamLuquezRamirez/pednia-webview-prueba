package com.pednia.kiosco

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.webkit.WebViewAssetLoader
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * WebView de prueba para PedNia.
 *
 * Objetivo: que getUserMedia (cámara/micrófono) funcione EMBEBIDO dentro de la
 * plataforma, aunque el servidor Laravel esté en HTTP por IP en la red local.
 *
 * Cómo: se carga el contenido bajo el origen HTTPS virtual
 *   https://appassets.androidplatform.net/...
 * que Chromium considera CONTEXTO SEGURO. Un PathHandler propio (LanProxyHandler)
 * reenvía cada petición a http://SERVIDOR_LAN, propagando método, cabeceras y
 * cookies (para que funcione el login del niño por PIN y los POST).
 */
class MainActivity : AppCompatActivity() {

    companion object {
        // >>> CAMBIA ESTO por la IP:puerto de tu servidor Laravel en la red local <<<
        private const val SERVIDOR_LAN = "192.168.1.15:8000"

        // Ruta inicial que se abre en el kiosco.
        private const val RUTA_INICIAL = "inicio"

        private const val DOMINIO_SEGURO = "appassets.androidplatform.net"
        private const val COD_PERMISOS = 1001
    }

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Pedir permisos de cámara y micrófono en runtime (Android 6+).
        pedirPermisos()

        webView = WebView(this)
        setContentView(webView)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true                    // localStorage del kiosco
            mediaPlaybackRequiresUserGesture = false    // audio/TTS y preview sin gesto
            databaseEnabled = true
            allowFileAccess = false
            allowContentAccess = false
        }

        // Loader: intercepta el origen https virtual y lo sirve desde la LAN.
        val assetLoader = WebViewAssetLoader.Builder()
            .setDomain(DOMINIO_SEGURO)
            .addPathHandler("/", LanProxyHandler(SERVIDOR_LAN))
            .build()

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? = assetLoader.shouldInterceptRequest(request.url)

            // Mantener la navegación dentro del origen seguro.
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean = false
        }

        // Concede a la página los permisos de cámara/micrófono que pida.
        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                runOnUiThread { request.grant(request.resources) }
            }
        }

        // Cargar por el origen HTTPS virtual → contexto seguro → getUserMedia OK.
        webView.loadUrl("https://$DOMINIO_SEGURO/$RUTA_INICIAL")
    }

    private fun pedirPermisos() {
        val faltantes = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            .filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (faltantes.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, faltantes.toTypedArray(), COD_PERMISOS)
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    /**
     * Puente LAN: recibe una petición bajo el origen https virtual y la reenvía
     * al servidor Laravel real por HTTP, propagando método, cabeceras (incluidas
     * cookies de sesión) y cuerpo. Devuelve la respuesta como WebResourceResponse.
     *
     * Nota: getUserMedia no exige nada de esto (basta el origen seguro), pero el
     * proxy sí debe propagar cookies/POST para que el login del niño y el guardado
     * de datos sigan funcionando a través del origen virtual.
     */
    private class LanProxyHandler(private val baseLan: String) : WebViewAssetLoader.PathHandler {
        override fun handle(path: String): WebResourceResponse? {
            return try {
                val url = URL("http://$baseLan/$path")
                val conn = url.openConnection() as HttpURLConnection
                conn.instanceFollowRedirects = false
                conn.connectTimeout = 15000
                conn.readTimeout = 20000

                val status = conn.responseCode
                val contentType = conn.contentType ?: "text/html"
                val mime = contentType.substringBefore(';').trim().ifEmpty { "text/html" }
                val charset = contentType.substringAfter("charset=", "utf-8").trim()

                // Cuerpo (o el stream de error si el status es >= 400).
                val body = try {
                    if (status >= 400) conn.errorStream ?: ByteArrayInputStream(ByteArray(0))
                    else conn.inputStream
                } catch (e: Exception) {
                    ByteArrayInputStream(ByteArray(0))
                }

                // Propagar cabeceras relevantes (Set-Cookie para la sesión, etc.).
                val headers = HashMap<String, String>()
                conn.headerFields.forEach { (k, v) ->
                    if (k != null && v.isNotEmpty()) headers[k] = v.joinToString(", ")
                }

                WebResourceResponse(mime, charset, status, mensajeEstado(status), headers, body)
            } catch (e: Exception) {
                null // deja que el WebView maneje el fallo
            }
        }

        private fun mensajeEstado(code: Int): String = when (code) {
            200 -> "OK"; 302 -> "Found"; 404 -> "Not Found"; 500 -> "Server Error"
            else -> "Status $code"
        }
    }
}
