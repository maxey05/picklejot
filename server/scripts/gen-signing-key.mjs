// Prints the ECDSA P-256 keypair for response signing.
//   private -> `wrangler secret put SIGNING_PRIVATE_KEY`
//   public  -> `licenseServerPublicKey` in gradle.properties
import { generateKeyPairSync } from "node:crypto";

const { privateKey, publicKey } = generateKeyPairSync("ec", { namedCurve: "P-256" });
console.log("SIGNING_PRIVATE_KEY (secret):\n" + privateKey.export({ type: "pkcs8", format: "der" }).toString("base64"));
console.log("\nlicenseServerPublicKey (app):\n" + publicKey.export({ type: "spki", format: "der" }).toString("base64"));
