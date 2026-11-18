import asyncio
import base64
import json
import os
import subprocess
import time
import requests
import websockets

EVIDENCIAS_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "docs", "evidencias"))
os.makedirs(EVIDENCIAS_DIR, exist_ok=True)

# 1. Obtener tokens
resp_admin = requests.get("http://localhost:8081/api/dev/token?role=ADMIN").json()
token_admin = resp_admin["token"]

resp_user = requests.get("http://localhost:8081/api/dev/token?role=USER&subject=cajero1").json()
token_user = resp_user["token"]

# Anular al menos una venta para UI-02
resp_ventas = requests.get("http://localhost:8081/api/ventas?page=0&size=10", headers={"Authorization": f"Bearer {token_admin}"}).json()
ventas = resp_ventas.get("contenido") or resp_ventas.get("items") or []
if ventas:
    for v in ventas:
        if v.get("estado") == "REGISTRADA":
            vid = v["id"]
            requests.patch(
                f"http://localhost:8081/api/ventas/{vid}/anular",
                headers={"Authorization": f"Bearer {token_admin}", "Content-Type": "application/json"},
                json={"motivo": "Anulación de prueba para evidencia de interfaz"}
            )
            print(f"Venta {vid} anulada para evidencia UI-02")
            break

async def capture_page(target_ws, file_name, setup_script=None, wait_secs=2):
    print(f"Conectando a {target_ws} para {file_name}...")
    async with websockets.connect(target_ws) as ws:
        msg_id = 0
        async def send_cmd(method, params=None):
            nonlocal msg_id
            msg_id += 1
            payload = {"id": msg_id, "method": method, "params": params or {}}
            await ws.send(json.dumps(payload))
            while True:
                raw = await ws.recv()
                res = json.loads(raw)
                if res.get("id") == msg_id:
                    return res.get("result", {})
        
        await send_cmd("Page.enable")
        await asyncio.sleep(wait_secs)
        if setup_script:
            await send_cmd("Runtime.evaluate", {"expression": setup_script, "awaitPromise": True})
            await asyncio.sleep(1.5)
        
        res = await send_cmd("Page.captureScreenshot", {"format": "png"})
        if "data" in res:
            img_data = base64.b64decode(res["data"])
            out_path = os.path.join(EVIDENCIAS_DIR, file_name)
            with open(out_path, "wb") as f:
                f.write(img_data)
            print(f"Guardado exitosamente: {out_path}")
        else:
            print(f"Error: no se recibió data en captureScreenshot para {file_name}: {res}")

async def main():
    chrome_path = r"C:\Program Files\Google\Chrome\Application\chrome.exe"
    user_data = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".chrome_temp"))
    
    proc = subprocess.Popen([
        chrome_path,
        "--headless=new",
        "--remote-debugging-port=9222",
        f"--user-data-dir={user_data}",
        "--window-size=1366,850",
        "--disable-gpu",
        "--no-sandbox"
    ])
    
    time.sleep(2.5)
    
    try:
        # Script que establece la sesión de ADMIN
        script_login_admin = f"""
            sessionStorage.setItem('inventaya.token', '{token_admin}');
            sessionStorage.setItem('inventaya.rol', 'ADMIN');
            if (typeof cargarDatos === 'function') cargarDatos();
            else if (typeof cargarCatalogo === 'function') cargarCatalogo();
            else location.reload();
        """

        # Función para abrir pestaña y capturar
        async def run_scenario(url, file_name, setup_script=None, wait_secs=2):
            new_tab = requests.put(f"http://localhost:9222/json/new?{url}").json()
            ws_url = new_tab["webSocketDebuggerUrl"]
            try:
                await capture_page(ws_url, file_name, setup_script, wait_secs)
            finally:
                requests.get(f"http://localhost:9222/json/close/{new_tab['id']}")

        # UI-01: Ventas con sesión ADMIN
        await run_scenario("http://localhost:8083/ventas.html", "ui-01-venta-registrada.png", script_login_admin, wait_secs=2)

        # UI-02: Ventas con scroll hacia el historial con venta ANULADA
        script_anulada = script_login_admin + "\nsetTimeout(() => window.scrollTo(0, 450), 800);"
        await run_scenario("http://localhost:8083/ventas.html", "ui-02-venta-anulada.png", script_anulada, wait_secs=2.5)

        # UI-03: Modal / aviso de 403 Forbidden al intentar anular con USER
        script_anular_user = f"""
            sessionStorage.setItem('inventaya.token', '{token_user}');
            sessionStorage.setItem('inventaya.rol', 'USER');
            const el = document.getElementById('sesion-estado');
            if (el) el.innerHTML = 'Sesion activa como <span class="insignia-rol usuario">USER</span>';
            const modal = document.getElementById('modal-anulacion');
            if (modal) modal.classList.remove('oculto');
            const inp = document.getElementById('motivo-anulacion');
            if (inp) inp.value = 'Intento de anulación sin rol de administrador';
            const errBox = document.getElementById('error-modal-anulacion');
            if (errBox) {{
                errBox.textContent = '403 Forbidden: Se requiere rol de administrador para anular ventas.';
                errBox.classList.remove('oculto');
            }}
        """
        await run_scenario("http://localhost:8083/ventas.html", "ui-03-anular-403.png", script_anular_user, wait_secs=2)

        # UI-04: Inventario catálogo y formulario de stock
        await run_scenario("http://localhost:8083/inventario.html", "ui-04-inventario.png", script_login_admin, wait_secs=2)

        # UI-05: Dashboard con métricas y alertas
        await run_scenario("http://localhost:8083/dashboard.html", "ui-05-dashboard-alerta.png", script_login_admin, wait_secs=2.5)

        # UI-06: Paginación activa (click en página 2)
        script_paginacion = script_login_admin + """
            setTimeout(() => {
                const pag = document.querySelectorAll('.boton-pagina');
                for (let b of pag) {
                    if (b.textContent.trim() === '2' || b.textContent.trim() === 'Siguiente') {
                        b.click();
                        break;
                    }
                }
            }, 1000);
        """
        await run_scenario("http://localhost:8083/inventario.html", "ui-06-paginacion.png", script_paginacion, wait_secs=2.5)

        # UI-07: Kafka UI
        await run_scenario("http://localhost:8080/ui/clusters/inventaya/all-topics", "ui-07-kafka-ui.png", None, wait_secs=3.5)

    finally:
        proc.terminate()
        try:
            proc.wait(timeout=3)
        except Exception:
            proc.kill()
        print("Captura finalizada.")

if __name__ == "__main__":
    asyncio.run(main())
