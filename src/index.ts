'use strict';
import * as vscode from 'vscode';
import { install } from './lombok-installer';


export function activate(context: vscode.ExtensionContext) {
    install();
}

export function deactivate(context: vscode.ExtensionContext) {
    // VSCode doesn't support settings removing during deactivation (issue #45474)
}