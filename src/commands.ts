'use strict';

import * as vscode from "vscode";

// tslint:disable-next-line: no-namespace
export namespace Commands {

    export const JAVA_EXECUTE_WORKSPACE_COMMAND = "java.execute.workspaceCommand";

    export const CODEACTION_LOMBOK = "codeAction.lombok";

    export const JAVA_CODEACTION_LOMBOK_ANNOTATIONS = "java.codeAction.lombok.getAnnotations";

    export const JAVA_CODEACTION_LOMBOK = "java.codeAction.lombok";

    export const GET_ALL_JAVA_PROJECTS = 'java.project.getAll';

    export const ORGANIZE_IMPORTS_SILENTLY = "java.edit.organizeImports";
}

export async function executeJavaLanguageServerCommand<T>(...rest: any[]): Promise<T | undefined> {
    return vscode.commands.executeCommand<T>(Commands.JAVA_EXECUTE_WORKSPACE_COMMAND, ...rest);
}
