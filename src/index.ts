'use strict';
import * as vscode from 'vscode';
import { setLombokToVSCode } from './extension';
import lombokConfig from './lombok-config';

export function activate(context: vscode.ExtensionContext) {
    if (setLombokToVSCode(lombokConfig)) {
        const { displayName } = require('../package.json');
        vscode.window.showInformationMessage(displayName + ' is active');
    }
}

export function deactivate(context: vscode.ExtensionContext) {
    //return cleanLombok(lombokConfig);
}