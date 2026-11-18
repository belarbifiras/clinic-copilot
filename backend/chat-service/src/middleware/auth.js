const crypto = require('crypto');

const JWKS_URI = process.env.KEYCLOAK_JWKS_URI
    || process.env.KEYCLOAK_JWK_SET_URI
    || 'http://localhost:8180/realms/clinic/protocol/openid-connect/certs';

let cachedCert = null;
let lastFetch = 0;

async function getSigningCert() {
    const now = Date.now();
    // Cache de 1 heure pour éviter d'appeler Keycloak à chaque requête
    if (!cachedCert || now - lastFetch > 3600000) {
        const res = await fetch(JWKS_URI);
        const data = await res.json();
        const key = (data.keys || []).find(k => k.use === 'sig' && k.alg === 'RS256') || data.keys?.[0];
        if (!key || !key.x5c || !key.x5c[0]) {
            throw new Error('Certificat Keycloak introuvable');
        }
        cachedCert = `-----BEGIN CERTIFICATE-----\n${key.x5c[0]}\n-----END CERTIFICATE-----`;
        lastFetch = now;
    }
    return cachedCert;
}

async function verifyToken(token) {
    if (!token) throw new Error('Token absent');
    const parts = token.split('.');
    if (parts.length !== 3) throw new Error('Format de token invalide');

    const [headerB64, payloadB64, sigB64] = parts;

    const cert = await getSigningCert();
    const verifier = crypto.createVerify('RSA-SHA256');
    verifier.update(`${headerB64}.${payloadB64}`);
    const isValid = verifier.verify(cert, Buffer.from(sigB64, 'base64url'));

    if (!isValid) {
        throw new Error('Signature du token invalide');
    }

    const payload = JSON.parse(Buffer.from(payloadB64, 'base64url').toString('utf8'));

    if (payload.exp && payload.exp * 1000 < Date.now()) {
        throw new Error('Token expiré');
    }

    const roles = payload.realm_access?.roles || [];
    const matchedRole = roles.find(r => ['ADMIN', 'DOCTOR', 'PATIENT'].includes(r))
        || payload.role
        || '';

    return {
        id: String(payload.sub || payload.userId || payload.id),
        role: String(matchedRole).replace('ROLE_', ''),
    };
}

async function auth(req, res, next) {
    const header = req.headers.authorization || '';
    if (!header.startsWith('Bearer ')) {
        return res.status(401).json({ message: 'Token manquant' });
    }
    try {
        req.user = await verifyToken(header.slice(7));
        next();
    } catch (err) {
        res.status(401).json({ message: 'Token invalide' });
    }
}

module.exports = { auth, verifyToken };