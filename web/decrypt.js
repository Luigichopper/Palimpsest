/**
 * web/decrypt.js
 * Client-side WebCrypto decryption for encrypted stage content.
 */

function hexToBytes(hex) {
    const bytes = new Uint8Array(hex.length / 2);
    for (let i = 0; i < bytes.length; i++) {
        bytes[i] = parseInt(hex.substr(i * 2, 2), 16);
    }
    return bytes;
}

async function decryptStage(passphrase, stageData) {
    const enc = new TextEncoder();
    const keyMaterial = await crypto.subtle.importKey(
        "raw",
        enc.encode(passphrase.toLowerCase().trim()),
        "PBKDF2",
        false,
        ["deriveKey"]
    );

    const salt = hexToBytes(stageData.salt_hex);
    const iv = hexToBytes(stageData.iv_hex);
    const ciphertextWithTag = hexToBytes(stageData.ciphertext_hex + stageData.tag_hex);

    const key = await crypto.subtle.deriveKey(
        {
            name: "PBKDF2",
            salt: salt,
            iterations: stageData.iterations || 100000,
            hash: "SHA-256"
        },
        keyMaterial,
        { name: "AES-GCM", length: 256 },
        false,
        ["decrypt"]
    );

    try {
        const decrypted = await crypto.subtle.decrypt(
            { name: "AES-GCM", iv: iv },
            key,
            ciphertextWithTag
        );
        return new TextDecoder().decode(decrypted);
    } catch (e) {
        throw new Error("Decryption failed. Invalid password or key hash mismatch.");
    }
}

// Toggle high contrast theme
function toggleTheme() {
    const current = document.documentElement.getAttribute("data-theme");
    if (current === "high-contrast") {
        document.documentElement.removeAttribute("data-theme");
        localStorage.setItem("palimpsest_theme", "default");
    } else {
        document.documentElement.setAttribute("data-theme", "high-contrast");
        localStorage.setItem("palimpsest_theme", "high-contrast");
    }
}

document.addEventListener("DOMContentLoaded", () => {
    if (localStorage.getItem("palimpsest_theme") === "high-contrast") {
        document.documentElement.setAttribute("data-theme", "high-contrast");
    }
});
