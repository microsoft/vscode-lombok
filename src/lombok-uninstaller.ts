import { readFileSync, writeFileSync, existsSync } from "fs";

const VM_ARGS_KEY = "java.jdt.ls.vmargs";

export function getUserSettingsPath(platform: string): string {
    const map: any = {
        win32: process.env.APPDATA + '\\Code\\User\\settings.json',
        darwin: process.env.HOME + '/Library/Application Support/Code/User/settings.json',
        linux: process.env.HOME + '/.config/Code/User/settings.json'
    };
    return map[platform];
}

export function uninstall(): void {
    const userSettingsPath: string = getUserSettingsPath(process.platform);
    
    if (userSettingsPath && existsSync(userSettingsPath)) {
        const settings = JSON.parse(readFileSync(userSettingsPath, 'utf8'));
        const vmArgs: string = settings[VM_ARGS_KEY];

        if (vmArgs && vmArgs.match(/-javaagent:".*"/)) {
            const newVmArgs = vmArgs.replace(/-javaagent:".*"/, "").trim();
            settings[VM_ARGS_KEY] = newVmArgs === "" ? undefined : newVmArgs;

            writeFileSync(userSettingsPath, JSON.stringify(settings), 'utf8');
        }
    }
}