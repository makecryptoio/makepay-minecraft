import { existsSync, readFileSync, readdirSync, statSync } from "node:fs";
import path from "node:path";

const root = process.cwd();
const requiredFiles = [
  "settings.gradle.kts",
  "build.gradle.kts",
  "src/main/resources/plugin.yml",
  "src/main/resources/config.yml",
  "src/main/java/io/makepay/minecraft/MakePayMinecraftPlugin.java",
  "src/main/java/io/makepay/minecraft/BackendClient.java",
  "src/main/java/io/makepay/minecraft/EntitlementPoller.java",
  "src/main/java/io/makepay/minecraft/EntitlementExecutor.java",
  "src/test/java/io/makepay/minecraft/TemplateRendererTest.java",
  "README.md",
  "CHANGELOG.md",
  "LICENSE",
  "SECURITY.md",
  "CONTRIBUTING.md",
  "docs/BACKEND_CONTRACT.md",
  "docs/ROADMAP.md",
  "docs/REPOSITORY_PROTECTION.md"
];

const forbiddenPatterns = [
  new RegExp("Jo" + "zef\\s+Voj" + "tas", "i"),
  new RegExp("orange" + "btc", "i"),
  new RegExp("vc" + "p_[A-Za-z0-9]+"),
  new RegExp("sb" + "p_[A-Za-z0-9]+"),
  new RegExp("Payments" + "2025", "i"),
  new RegExp("MAKEPAY_" + "SECRET", "i"),
  new RegExp("MAKEPAY_" + "API_KEY", "i"),
  new RegExp("part" + "ner[_-]?" + "key", "i")
];

const directMakePayApi = new RegExp("api\\.make" + "pay\\.io", "i");

function fail(message) {
  console.error(`validate: ${message}`);
  process.exitCode = 1;
}

for (const file of requiredFiles) {
  if (!existsSync(path.join(root, file))) {
    fail(`missing ${file}`);
  }
}

const pluginYml = readFileSync(path.join(root, "src/main/resources/plugin.yml"), "utf8");
for (const expected of [
  "name: MakePay",
  "main: io.makepay.minecraft.MakePayMinecraftPlugin",
  "api-version: '1.21'",
  "makepay.admin"
]) {
  if (!pluginYml.includes(expected)) {
    fail(`plugin.yml missing ${expected}`);
  }
}

const build = readFileSync(path.join(root, "build.gradle.kts"), "utf8");
for (const expected of [
  "com.gradleup.shadow",
  "paper-api:1.21.11-R0.1-SNAPSHOT",
  "JavaLanguageVersion.of(21)",
  "relocate(\"com.google.gson\""
]) {
  if (!build.includes(expected)) {
    fail(`build.gradle.kts missing ${expected}`);
  }
}

function listFiles(directory) {
  const entries = readdirSync(directory);
  const files = [];
  for (const entry of entries) {
    if ([".git", "build", ".gradle", "node_modules"].includes(entry)) {
      continue;
    }
    const absolute = path.join(directory, entry);
    const relative = path.relative(root, absolute);
    const stat = statSync(absolute);
    if (stat.isDirectory()) {
      files.push(...listFiles(absolute));
    } else {
      files.push(relative);
    }
  }
  return files;
}

for (const file of listFiles(root)) {
  const body = readFileSync(path.join(root, file), "utf8");
  for (const pattern of forbiddenPatterns) {
    if (pattern.test(body)) {
      fail(`${file} contains forbidden pattern ${pattern}`);
    }
  }
  if (file.startsWith("src/main/java") && directMakePayApi.test(body)) {
    fail(`${file} appears to call MakePay APIs directly instead of a merchant backend relay`);
  }
}

if (process.exitCode) {
  process.exit();
}

console.log("validate: Minecraft plugin metadata and safety checks passed");
