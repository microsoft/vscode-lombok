'use strict';

import * as vscode from 'vscode';
import * as path from 'path';
import { getExtensionApi } from './util';
import { Commands, executeJavaLanguageServerCommand } from './commands';

const lombokJarRegex = /lombok-\d+.*\.jar$/;

export async function isLombokExists(): Promise<boolean> {
    const projectUris: string[] = await getAllJavaProjects();
    const extensionApi = await getExtensionApi();
    if (!extensionApi) {
        return false;
    }
    for (const projectUri of projectUris) {
        const classpathResult = await extensionApi.getClasspaths(projectUri, { scope: 'test' });
        for (const classpath of classpathResult.classpaths) {
            if (lombokJarRegex.test(classpath)) {
                return true;
            }
        }
    }
    return false;
}

async function getAllJavaProjects(excludeDefaultProject: boolean = true): Promise<string[]> {
    let projectUris: string[] = await executeJavaLanguageServerCommand(Commands.GET_ALL_JAVA_PROJECTS) as string[];
    if (excludeDefaultProject) {
        projectUris = projectUris.filter((uriString) => {
            const projectPath = vscode.Uri.parse(uriString).fsPath;
            return path.basename(projectPath) !== "jdt.ls-java-project";
        });
    }
    return projectUris;
}
