import * as vscode from "vscode";
import { Extension } from 'vscode';

const { publisher, name, displayName } = require('../package.json');

function getExtension(): Extension<any> {
    const extensionId = publisher + '.' + name;
    const extension: Extension<any> | undefined = vscode.extensions.getExtension(extensionId);
    if (extension === undefined) {
        throw new Error('Visual Studio Code could not find ' + displayName +
            ' with id: ' + extensionId + ' in .vscode/extensions folder');
    }

    return extension;
}

const { lombokConfig } = require('../package.json');

const lombokJar: string = getExtension().extensionPath + lombokConfig.path;

lombokConfig.vmArgsValue = lombokConfig.vmArgsValue.replace(new RegExp("{lombok-jar}", 'g'), lombokJar);

export default lombokConfig;
