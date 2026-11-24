// Outil de TEST développeur (pas le frontend final).
// Sert index.html et relaie /api/* vers le service IA (aucune modification du service, pas de CORS).
// Lancer :  node server.js      puis ouvrir http://localhost:5500
const http = require("http");
const fs = require("fs");
const path = require("path");

const PORT = Number(process.env.PORT || 5500);
const TARGET_HOST = process.env.AI_HOST || "localhost";
const TARGET_PORT = Number(process.env.AI_PORT || 8090);

http
  .createServer((req, res) => {
    if (req.url === "/" || req.url === "/index.html") {
      res.writeHead(200, { "Content-Type": "text/html; charset=utf-8" });
      return fs.createReadStream(path.join(__dirname, "index.html")).pipe(res);
    }
    if (!req.url.startsWith("/api/")) {
      res.writeHead(404);
      return res.end("Not found");
    }
    const headers = {};
    for (const h of ["content-type", "x-patient-id", "accept"]) {
      if (req.headers[h]) headers[h] = req.headers[h];
    }
    const upstream = http.request(
      { host: TARGET_HOST, port: TARGET_PORT, method: req.method, path: req.url.slice(4), headers, timeout: 180000 },
      (up) => {
        res.writeHead(up.statusCode || 502, { "Content-Type": up.headers["content-type"] || "application/json" });
        up.pipe(res);
      }
    );
    upstream.on("timeout", () => upstream.destroy(new Error("timeout")));
    upstream.on("error", (e) => {
      res.writeHead(502, { "Content-Type": "application/json" });
      res.end(JSON.stringify({ detail: "Service IA injoignable sur " + TARGET_HOST + ":" + TARGET_PORT + " (" + e.message + ")" }));
    });
    req.pipe(upstream);
  })
  .listen(PORT, () => console.log("Page de test : http://localhost:" + PORT + "  ->  service IA " + TARGET_HOST + ":" + TARGET_PORT));
