// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

import * as vscode from "vscode";
import * as utility from "./utility";

// tslint:disable-next-line: no-namespace
export namespace Commands {

    export const JAVA_EXECUTE_WORKSPACE_COMMAND = "java.execute.workspaceCommand";

    export const CODEACTION_LOMBOK = "codeAction.lombok";

    export const CODEACTION_DELOMBOK = "codeAction.delombok";

    export const JAVA_CODEACTION_LOMBOK_ANNOTATIONS = "java.codeAction.lombok.getAnnotations";

    export const JAVA_CODEACTION_DELOMBOK = "java.codeAction.delombok";

    export const JAVA_CODEACTION_LOMBOK = "java.codeAction.lombok";

    export const JAVA_CODEACTION_SELECTANNOTATION = "java.codeAction.selectAnnotation";
}

export function executeJavaLanguageServerCommand(...rest: any[]) {
    return executeJavaExtensionCommand(Commands.JAVA_EXECUTE_WORKSPACE_COMMAND, ...rest);
}

export async function executeJavaExtensionCommand(commandName: string, ...rest: any[]) {
    // TODO: need to handle error and trace telemetry
    const javaExtension = utility.getJavaExtension();
    if (!javaExtension) {
        throw new utility.JavaExtensionNotEnabledError(`Cannot execute command ${commandName}, VS Code Java Extension is not enabled.`);
    }
    if (!javaExtension.isActive) {
        await javaExtension.activate();
    }
    return vscode.commands.executeCommand(commandName, ...rest);
}
