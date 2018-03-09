'use strict';

import * as vscode from 'vscode';
const pjson = require('../package.json');

const extensionId = pjson.publisher + '.' + pjson.name;

const extension: vscode.Extension<any> | undefined = vscode.extensions.getExtension(extensionId);

if (extension === undefined) {
    throw new Error('Visual Studio Code could not find an extension with id: ' + extensionId);
}

const vmargsKey = 'java.jdt.ls.vmargs';

const lombokJar = extension.extensionPath + '\\server\\lombok.jar';

const lombokValue = '-javaagent:"' + lombokJar + '" -Xbootclasspath/a:"' + lombokJar + '"';

export function activate(context: vscode.ExtensionContext) {

    const config = vscode.workspace.getConfiguration();

    const vmArgsValue: string | undefined = config.get(vmargsKey);

    if (!vmArgsValue) {
        config.update(vmargsKey, lombokValue);
    } else {
        if (vmArgsValue.indexOf(lombokValue) === -1) {
            config.update(vmargsKey, vmArgsValue + ' ' + lombokValue);
        }

    }
}


export function deactivate(context: vscode.ExtensionContext) {
}