import * as vscode from "vscode";
import { ConfigurationTarget, WorkspaceConfiguration, Extension } from "vscode";
import * as path from 'path';
import { VM_ARGS_KEY, LOMBOK_PATH_KEY } from "./util";
import * as fs from "fs";

const { publisher, name } = require('../package.json');
const https = require('https');
const logger = vscode.window.createOutputChannel(name);

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
    vscode.window.showInformationMessage("If you have any trouble using Lombok, please, make sure your project is using the latest version");
}

function getWorkspaceConfig(): WorkspaceConfiguration {
    return vscode.workspace.getConfiguration();
}

function downloadLatestJar(to: string) {
    logger.appendLine("Downloading latest lombok...");

    var dir = path.dirname(to);
    if (!fs.existsSync(dir)) {
        fs.mkdirSync(dir);
    }

    var file = fs.createWriteStream(to);
    https.get("https://projectlombok.org/downloads/lombok.jar", function (response: any) {
        logger.appendLine("Saving the downloaded lombok...");
        
        response.pipe(file);
        file.on('finish', function() {
            file.close();
            logger.appendLine("Download completed");
        }).on('error', function(err: any) {
            logger.appendLine("Unable to download lombok. " + err.message);
        });
    });
}

function getJarPath(): string {
    const customJarPath = vscode.workspace.getConfiguration(name).get<string>(LOMBOK_PATH_KEY)?.trim();
    const builtInJarPath = path.join(getExtensionInstance().extensionPath, "server", "lombok.jar");

    if (customJarPath) {
        return customJarPath;
    }

    if (!fs.existsSync(builtInJarPath)) {
        logger.appendLine("Built-in jar does not exist. Will download the latest version.");
        downloadLatestJar(builtInJarPath);
    }
    else {
        logger.appendLine("Built-in jar already exists. Skip downloading");
    }
    
    return builtInJarPath;
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