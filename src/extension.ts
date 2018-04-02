import * as vscode from "vscode";
import { Extension, ConfigurationTarget } from "vscode";

const { publisher, name, displayName } = require('../package.json');

function getSetting(key: string): string | undefined {
    return vscode.workspace.getConfiguration().get(key);
}

export function getExtension(): Extension<any> {
    const extensionId = publisher + '.' + name;

    const extension: Extension<any> | undefined = vscode.extensions.getExtension(extensionId);

    if (extension === undefined) {
        throw new Error('Visual Studio Code could not find ' + displayName +
            ' with id: ' + extensionId + ' in .vscode/extensions folder');
    }

    return extension;
}

export async function setLombokToVSCode(lombokConfig: any): Promise<boolean> {

    const previousVmArguments = getSetting(lombokConfig.vmArgsKey);

    if (!previousVmArguments) {
        return updateVMSettings(lombokConfig.vmArgsKey, lombokConfig.vmArgsValue);
    } else if (previousVmArguments.indexOf(lombokConfig.path) === -1) {
        return updateVMSettings(lombokConfig.vmArgsKey, previousVmArguments.trim() + ' ' + lombokConfig.vmArgsValue);
    }

    return true;
}

export async function cleanLombok(lombokConfig: any): Promise<boolean> {
    const actualVmArguments = getSetting(lombokConfig.vmArgsKey);

    return actualVmArguments !== undefined && updateVMSettings(lombokConfig.vmArgsKey, actualVmArguments.replace(lombokConfig.vmArgsValue, ''));
}

async function updateVMSettings(key: string, value: string): Promise<boolean> {
    await vscode.workspace.getConfiguration().update(key, value, ConfigurationTarget.Global);

    const newVmArguments = getSetting(key);

    return newVmArguments !== undefined && newVmArguments === value;
}