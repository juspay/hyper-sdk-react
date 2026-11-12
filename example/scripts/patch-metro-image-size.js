// Runs as the example's `postinstall`.
//
// Metro 0.82 (React Native 0.79) depends on image-size 1.x, which has unfixed
// denial-of-service advisories (GHSA-w3rx-r6r6-pgpr, GHSA-5p2g-fcmc-qvqq), so
// package.json resolves image-size to 2.x instead. 2.x only has a named export
// and only accepts a buffer, so this rewrites Metro's one require of image-size
// to match. 1.x has the same named export, so the rewritten line works with
// either version.
//
// Metro 0.83.8 and later no longer use image-size: delete this script, the
// postinstall entry and the image-size resolution once the example moves past
// React Native 0.80.
const fs = require('fs');
const path = require('path');

const ORIGINAL = 'const getImageSize = require("image-size");';
const PATCHED = [
  'const { imageSize } = require("image-size");',
  'const getImageSize = (input) =>',
  '  imageSize(typeof input === "string" ? fs.readFileSync(input) : input);',
].join('\n');

const assetsPath = path.join(
  path.dirname(require.resolve('metro/package.json')),
  'src',
  'Assets.js'
);
const source = fs.readFileSync(assetsPath, 'utf8');

if (source.includes(ORIGINAL)) {
  fs.writeFileSync(assetsPath, source.replace(ORIGINAL, PATCHED));
  console.log('Patched Metro to use the image-size 2 API');
} else if (source.includes('image-size') && !source.includes(PATCHED)) {
  throw new Error(
    `Unexpected image-size usage in ${assetsPath}, update scripts/patch-metro-image-size.js`
  );
}
