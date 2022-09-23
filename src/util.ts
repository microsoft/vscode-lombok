'use strict';

import * as vscode from "vscode";
import { JAVA_EXTENSION_ID } from "./constants";

export const VM_ARGS_KEY = "java.jdt.ls.vmargs";

export function getUserSettingsPath(platform: string): string {
    const map: any = {
        win32: process.env.APPDATA + '\\Code\\User\\settings.json',
        darwin: process.env.HOME + '/Library/Application Support/Code/User/settings.json',
        linux: process.env.HOME + '/.config/Code/User/settings.json'
    };
    return map[platform];
}

export function isLombokSupportEnabled(): boolean | undefined {
	return vscode.workspace.getConfiguration().get("java.jdt.ls.lombokSupport.enabled");
}

export function getJavaExtension(): vscode.Extension<any> | undefined {
    return vscode.extensions.getExtension(JAVA_EXTENSION_ID);
}

export async function getExtensionApi(): Promise<any> {
    const extension: vscode.Extension<any> | undefined = getJavaExtension();
    if (extension === undefined) {
        return undefined;
    }
    const extensionApi: any = await extension.activate();
    if (extensionApi.getClasspaths === undefined) {
        throw undefined;
    }
    return extensionApi;
}
