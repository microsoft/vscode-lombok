'use strict';

import { Disposable, Event, Extension, ExtensionContext, languages, Uri } from 'vscode';
import { initializeFromJsonFile, dispose as disposeTelemetryWrapper, instrumentOperation, instrumentOperationAsVsCodeCommand } from 'vscode-extension-telemetry-wrapper';
import { LombokCodeActionProvider, lombokAction } from './codeActionProvider';
import { LanguageServerMode } from './constants';
import { getJavaExtension, isLombokSupportEnabled } from './util';
import { isLombokExists } from './lombokChecker';
import { CodeActionParams } from 'vscode-languageclient';
import { Commands } from './commands';

let isRegistered: boolean = false;
let disposables: Disposable[] = [];

export async function activate(context: ExtensionContext): Promise<void> {
    await initializeFromJsonFile(context.asAbsolutePath('./package.json'));
    await instrumentOperation('activation', doActivate)(context);
}

async function doActivate(_operationId: string, context: ExtensionContext): Promise<void> {
    const javaLanguageSupport: Extension<any> | undefined = getJavaExtension();
    if (!javaLanguageSupport) {
        return;
    }
    if (!javaLanguageSupport.isActive) {
        await javaLanguageSupport.activate();
    }
    const extensionApi: any = javaLanguageSupport.exports;
    if (!extensionApi) {
        return;
    }

    if (extensionApi.serverMode === LanguageServerMode.LightWeight) {
        if (extensionApi.onDidServerModeChange) {
            const onDidServerModeChange: Event<string> = extensionApi.onDidServerModeChange;
            context.subscriptions.push(onDidServerModeChange(async (mode: string) => {
                if (mode === LanguageServerMode.Standard) {
                    syncComponents();
                }
            }));
        }
    } else {
        await extensionApi.serverReady();
        syncComponents();
    }

    if (extensionApi.onDidClasspathUpdate) {
        const onDidClasspathUpdate: Event<Uri> = extensionApi.onDidClasspathUpdate;
        context.subscriptions.push(onDidClasspathUpdate(async () => {
            // workaround: wait more time to make sure Language Server has updated all caches
            setTimeout(() => {
                syncComponents();
            }, 1000 /*ms*/);
        }));
    }

    if (extensionApi.onDidProjectsImport) {
        const onDidProjectsImport: Event<Uri[]> = extensionApi.onDidProjectsImport;
        context.subscriptions.push(onDidProjectsImport(() => {
            syncComponents();
        }));
    }
}

async function syncComponents(): Promise<void> {
    if (isLombokSupportEnabled() && await isLombokExists()) {
        registerComponents();
    } else {
        unRegisterComponents();
    }
}

async function registerComponents(): Promise<void> {
    if (isRegistered) {
        return;
    }
    disposables.push(instrumentOperationAsVsCodeCommand(Commands.CODEACTION_LOMBOK, async (params: CodeActionParams, selectedAnnotations: string[]) => {
        lombokAction(params, selectedAnnotations);
    }));
    disposables.push(languages.registerCodeActionsProvider({ scheme: 'file', language: 'java' }, new LombokCodeActionProvider()));
    isRegistered = true;
}

async function unRegisterComponents(): Promise<void> {
    if (!isRegistered) {
        return;
    }
    for (const disposable of disposables) {
        disposable.dispose();
    }
    disposables = [];
    isRegistered = false;
}

export async function deactivate(): Promise<void> {
    for (const disposable of disposables) {
        disposable.dispose();
    }
    await disposeTelemetryWrapper();
}
