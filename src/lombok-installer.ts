import * as vscode from "vscode";
import { ConfigurationTarget, WorkspaceConfiguration } from "vscode";
import { getJarPath } from './util';

const VM_ARGS_KEY = "java.jdt.ls.vmargs";

async function updateVmArgs(value: string) {
    await getWorkspaceConfig().update(VM_ARGS_KEY, value, ConfigurationTarget.Global);
}

function getWorkspaceConfig(): WorkspaceConfiguration {
    return vscode.workspace.getConfiguration();
}

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