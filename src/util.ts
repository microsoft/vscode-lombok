import * as vscode from "vscode";
import { Extension } from "vscode";

const { publisher, name } = require('../package.json');

function getExtensionInstance(): Extension<any> {
    const extensionId = publisher + '.' + name;
    const instance = vscode.extensions.getExtension(extensionId);
    if (!instance) {
        throw new Error("Could not get extension instance with id " + extensionId);
    }
    return instance;
}

export const getJarPath = () => getExtensionInstance().extensionPath + "/server/lombok.jar";