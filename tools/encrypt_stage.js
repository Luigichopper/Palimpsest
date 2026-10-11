#!/usr/bin/env node
/**
 * tools/encrypt_stage.js
 * Encrypts website stage content using PBKDF2 (SHA-256) and AES-256-GCM.
 * Compatible with WebCrypto SubtleCrypto in web/hints.js and web/terminal.js.
 * 
 * Usage:
 *   node tools/encrypt_stage.js <stageId> <answer> <contentOrFile> [outputPath]
 */

const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

function encryptStage(stageId, answer, content, outputPath) {
    const salt = crypto.randomBytes(16);
    const iv = crypto.randomBytes(12);
    const key = crypto.pbkdf2Sync(answer.toLowerCase().trim(), salt, 100000, 32, 'sha256');

    const cipher = crypto.createCipheriv('aes-256-gcm', key, iv);
    let ciphertext = cipher.update(content, 'utf8', 'hex');
    ciphertext += cipher.final('hex');
    const tag = cipher.getAuthTag().toString('hex');

    const result = {
        stage: stageId,
        salt_hex: salt.toString('hex'),
        iv_hex: iv.toString('hex'),
        ciphertext_hex: ciphertext,
        tag_hex: tag,
        iterations: 100000,
        algorithm: 'AES-GCM-256'
    };

    const outDir = path.dirname(outputPath);
    if (!fs.existsSync(outDir)) {
        fs.mkdirSync(outDir, { recursive: true });
    }

    fs.writeFileSync(outputPath, JSON.stringify(result, null, 2), 'utf8');
    console.log(`Encrypted stage '${stageId}' -> ${outputPath}`);
    return result;
}

if (require.main === module) {
    const args = process.argv.slice(2);
    if (args.length < 3) {
        console.log("Usage: node tools/encrypt_stage.js <stageId> <answer> <contentOrFile> [outputPath]");
        process.exit(1);
    }

    const [stageId, answer, contentArg, optOut] = args;
    let content = contentArg;
    if (fs.existsSync(contentArg)) {
        content = fs.readFileSync(contentArg, 'utf8');
    }

    const defaultOut = path.join(__dirname, '..', 'web', 'data', `${stageId}.json`);
    const outputPath = optOut || defaultOut;

    encryptStage(stageId, answer, content, outputPath);
}

module.exports = { encryptStage };
