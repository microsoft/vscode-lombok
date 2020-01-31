import * as vscode from "vscode";
import { ConfigurationTarget, WorkspaceConfiguration, Extension } from "vscode";
import * as path from 'path';
import { VM_ARGS_KEY } from "./util";

const { publisher, name } = require('../package.json');

function getExtensionInstance(): Extension<any> {
    const extensionId = publisher + '.' + name;
    const instance = vscode.extensions.getExtension(extensionId);
    if (!instance) {
        throw new Error("Could not get extension instance with id " + extensionId);
    }
    return instance;
}

async function updateVmArgs(value: string) {
    await getWorkspaceConfig().update(VM_ARGS_KEY, value, ConfigurationTarget.Global);
}

function getWorkspaceConfig(): WorkspaceConfiguration {
    return vscode.workspace.getConfiguration();
}

export const getJarPath = () => path.join(getExtensionInstance().extensionPath, "server", "lombok.jar");

export async function install(): Promise<void> {

    const javaAgentArg = `-javaagent:"${getJarPath()}"`;

    const vmArgs: string | undefined = getWorkspaceConfig().get(VM_ARGS_KEY);
    if (!vmArgs) {
        await updateVmArgs(javaAgentArg);
    } else if (!vmArgs.match(/-javaagent:".*"/)) {
        await updateVmArgs(vmArgs.trim() + ' ' + javaAgentArg);
    } else if (!vmArgs.includes(javaAgentArg)) {
        await updateVmArgs(vmArgs.replace(/-javaagent:".*"/, javaAgentArg));
    }
}