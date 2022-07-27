'use strict';
import * as vscode from 'vscode';
import { registerCodaActionCommand, LombokCodeActionProvider } from './codeActionProvider';


export function activate(context: vscode.ExtensionContext) {
    registerCodaActionCommand(context);
    context.subscriptions.push(vscode.languages.registerCodeActionsProvider({ scheme: 'file', language: 'java' }, new LombokCodeActionProvider()));
}


export function deactivate() {
}