import { execFileSync } from 'node:child_process'
import { copyFileSync, existsSync, mkdirSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const frontend = new URL('../', import.meta.url)
const android = new URL('android/', frontend)
const cli = fileURLToPath(new URL('node_modules/@capacitor/cli/bin/capacitor', frontend))

// Na primeira execução, `cap add` também sincroniza o build web.
execFileSync(process.execPath, [cli, existsSync(android) ? 'sync' : 'add', 'android'], {
  cwd: frontend,
  stdio: 'inherit',
})

const debug = new URL('app/src/debug/', android)
mkdirSync(debug, { recursive: true })
copyFileSync(
  new URL('capacitor/AndroidManifest.debug.xml.template', frontend),
  new URL('AndroidManifest.xml', debug),
)
