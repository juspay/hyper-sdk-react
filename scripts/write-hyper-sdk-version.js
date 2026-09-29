#!/usr/bin/env node
// Runs as hyper-sdk-react's own `postinstall`. Rewrites the
// `hyperSdkVersion` literal in Package.swift with hyperSdkIOSVersion from
// the consuming app's package.json (found via INIT_CWD, the directory
// `npm install` was originally run from).
const fs = require('fs');
const path = require('path');

// Single source of truth for the base version — also read directly by
// hyper-sdk-react.podspec via package["hyperSdkIOSVersion"].
const ownPackageJson = JSON.parse(fs.readFileSync(path.join(__dirname, '..', 'package.json'), 'utf8'));
const BASE_HYPER_SDK_VERSION = ownPackageJson.hyperSdkIOSVersion;

const packageSwiftPath = path.join(__dirname, '..', 'Package.swift');
const versionLinePattern = /var hyperSdkVersion: Version = "[^"]+"/;

function compareVersions(a, b) {
  const partsA = a.split(/[.-]/).map((p) => (Number.isNaN(Number(p)) ? p : Number(p)));
  const partsB = b.split(/[.-]/).map((p) => (Number.isNaN(Number(p)) ? p : Number(p)));
  const length = Math.max(partsA.length, partsB.length);
  for (let i = 0; i < length; i++) {
    const partA = partsA[i] ?? 0;
    const partB = partsB[i] ?? 0;
    if (partA === partB) continue;
    if (typeof partA === 'number' && typeof partB === 'number') return partA - partB;
    return String(partA).localeCompare(String(partB));
  }
  return 0;
}

function resolveEffectiveVersion() {
  try {
    const appDir = process.env.INIT_CWD || process.cwd();
    const appPackageJson = JSON.parse(fs.readFileSync(path.join(appDir, 'package.json'), 'utf8'));
    const overrideVersion = appPackageJson.hyperSdkIOSVersion;
    if (typeof overrideVersion === 'string' && overrideVersion.length > 0 &&
        compareVersions(overrideVersion, BASE_HYPER_SDK_VERSION) > 0) {
      return overrideVersion;
    }
  } catch (e) {
    // No app package.json found / unreadable / no override — fall through to base.
  }
  return BASE_HYPER_SDK_VERSION;
}

const effectiveVersion = resolveEffectiveVersion();
const packageSwiftContents = fs.readFileSync(packageSwiftPath, 'utf8');
const updatedContents = packageSwiftContents.replace(
  versionLinePattern,
  `var hyperSdkVersion: Version = "${effectiveVersion}"`
);

if (updatedContents !== packageSwiftContents) {
  fs.writeFileSync(packageSwiftPath, updatedContents);
}

// Clean up the old (pre-direct-rewrite) local file, if present from an
// earlier install of this package.
const legacyLocalVersionFile = path.join(__dirname, '..', '.hyper-sdk-version');
if (fs.existsSync(legacyLocalVersionFile)) {
  fs.unlinkSync(legacyLocalVersionFile);
}
