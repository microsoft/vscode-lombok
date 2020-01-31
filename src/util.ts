export const VM_ARGS_KEY = "java.jdt.ls.vmargs";

export function getUserSettingsPath(platform: string): string {
    const map: any = {
        win32: process.env.APPDATA + '\\Code\\User\\settings.json',
        darwin: process.env.HOME + '/Library/Application Support/Code/User/settings.json',
        linux: process.env.HOME + '/.config/Code/User/settings.json'
    };
    return map[platform];
}