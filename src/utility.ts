
import * as vscode from "vscode";
import { setUserError } from "vscode-extension-telemetry-wrapper";

const JAVA_EXTENSION_ID = "redhat.java";

export function getJavaExtension(): vscode.Extension<any> | undefined {
    return vscode.extensions.getExtension(JAVA_EXTENSION_ID);
}

export class JavaExtensionNotEnabledError extends Error {
    constructor(message: string) {
        super(message);
        setUserError(this);
    }
}