'use strict';
import * as vscode from 'vscode';
import { setLombokToVSCode } from './extension';
import lombokConfig from './lombok-config';

export async function activate(context: vscode.ExtensionContext) {
    if (await setLombokToVSCode(lombokConfig)) {
        const { displayName } = require('../package.json');
        vscode.window.showInformationMessage(displayName + ' is active');
    }
}

export function deactivate(context: vscode.ExtensionContext) {
  // await cleanLombok(lombokConfig);
}