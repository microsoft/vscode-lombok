import * as vscode from "vscode";
import { ConfigurationTarget } from "vscode";
import lombokConfig from './lombok-config';

function getSetting(key: string): string | undefined {
    return vscode.workspace.getConfiguration().get(key);
}

export async function setLombokToVSCode(): Promise<boolean> {

    const previousVmArguments = getSetting(lombokConfig.vmArgsKey);

    if (!previousVmArguments) {
        return updateVMSettings(lombokConfig.vmArgsKey, lombokConfig.vmArgsValue);
    } else if (!previousVmArguments.includes(lombokConfig.path)) {
        return updateVMSettings(lombokConfig.vmArgsKey, previousVmArguments.trim() + ' ' + lombokConfig.vmArgsValue);
    } else if (!previousVmArguments.includes(lombokConfig.vmArgsValue)) {
        return updateVMSettings(lombokConfig.vmArgsKey, previousVmArguments.split('-javaagent:')[0].trim() + ' ' + lombokConfig.vmArgsValue);
    }

    return true;
}

export async function cleanLombok(): Promise<boolean> {
    const actualVmArguments = getSetting(lombokConfig.vmArgsKey);

    return actualVmArguments !== undefined && updateVMSettings(lombokConfig.vmArgsKey, actualVmArguments.replace(lombokConfig.vmArgsValue, ''));
}

async function updateVMSettings(key: string, value: string): Promise<boolean> {
    await vscode.workspace.getConfiguration().update(key, value, ConfigurationTarget.Global);

    const newVmArguments = getSetting(key);

    return newVmArguments !== undefined && newVmArguments === value;
}