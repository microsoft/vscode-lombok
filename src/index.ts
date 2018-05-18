'use strict';
import * as vscode from 'vscode';
import { setLombokToVSCode } from './extension';


export async function activate(context: vscode.ExtensionContext) {
    await setLombokToVSCode();
}

export function deactivate(context: vscode.ExtensionContext) {
    // await cleanLombok();
}