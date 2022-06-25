export const VM_ARGS_KEY = "java.jdt.ls.vmargs";
export const LOMBOK_PATH_KEY = "vscode-lombok.lombokPath";

export const LOMBOK_DOWNLOAD_URL = "https://projectlombok.org/downloads/lombok.jar";

export function getUserSettingsPath(platform: string): string {
    const map: any = {
        win32: process.env.APPDATA + '\\Code\\User\\settings.json',
        darwin: process.env.HOME + '/Library/Application Support/Code/User/settings.json',
        linux: process.env.HOME + '/.config/Code/User/settings.json'
    };
    return map[platform];
}