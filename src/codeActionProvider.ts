'use strict';

import { commands, window, workspace, CodeActionProvider, CancellationToken, CodeAction, CodeActionContext, Command, ProviderResult, Range, Selection, TextDocument, TextEditorRevealType, CodeActionKind, ThemeIcon, Uri, env, Disposable, QuickPickItem, QuickPickItemKind } from "vscode";
import { Commands, executeJavaLanguageServerCommand } from './commands';
import * as ProtocolConverter from "vscode-languageclient/lib/common/protocolConverter";
import * as CodeConverter from "vscode-languageclient/lib/common/codeConverter";
import { CodeActionParams, WorkspaceEdit } from 'vscode-languageclient';
import { AnnotationResponse, LombokRequestParams } from './protocol';
import { sendInfo } from "vscode-extension-telemetry-wrapper";

const protoConverter: ProtocolConverter.Converter = ProtocolConverter.createConverter(undefined, undefined);
const codeConverter: CodeConverter.Converter = CodeConverter.createConverter();

const supportedLombokAnnotations = ["Data", "NoArgsConstructor", "AllArgsConstructor", "Getter", "Setter", "ToString", "EqualsAndHashCode"];

const annotationsDescriptions = [
    "Bundles the features of @ToString, @EqualsAndHashCode, @Getter, @Setter and @RequiredArgsConstructor together",
    "Generates a constructor with no parameters.",
    "Generates a constructor with 1 parameter for each field in your class.",
    "Generates the default getter automatically.",
    "Generates the default setter automatically.",
    "Generates a toString for you.",
    "Generates hashCode and equals implementations from the fields of your object."
];

const annotationLinks = [
    "https://projectlombok.org/features/Data",
    "https://projectlombok.org/features/constructor",
    "https://projectlombok.org/features/constructor",
    "https://projectlombok.org/features/GetterSetter",
    "https://projectlombok.org/features/GetterSetter",
    "https://projectlombok.org/features/ToString",
    "https://projectlombok.org/features/EqualsAndHashCode"
];

async function applyWorkspaceEdit(workspaceEdit: WorkspaceEdit): Promise<void> {
    const edit = protoConverter.asWorkspaceEdit(workspaceEdit);
    if (edit) {
        await workspace.applyEdit(edit);
    }
}

async function revealWorkspaceEdit(workspaceEdit: WorkspaceEdit): Promise<void> {
    const codeWorkspaceEdit = protoConverter.asWorkspaceEdit(workspaceEdit);
    if (!codeWorkspaceEdit) {
        return;
    }
    for (const entry of codeWorkspaceEdit.entries()) {
        await workspace.openTextDocument(entry[0]);
        if (entry[1].length > 0) {
            // reveal first available change of the workspace edit
            window.activeTextEditor?.revealRange(entry[1][0].range, TextEditorRevealType.InCenter);
            break;
        }
    }
}

export async function lombokAction(params: CodeActionParams, annotations: string[]): Promise<void> {
    const annotationResponse = await executeJavaLanguageServerCommand(Commands.JAVA_CODEACTION_LOMBOK_ANNOTATIONS, JSON.stringify(params)) as AnnotationResponse;
    if (!annotationResponse) {
        return;
    }
    let annotationsAfter = [];
    if (annotations.length) {
        annotationsAfter = annotationResponse.annotations.filter((item) => {
            return !annotations.includes(item);
        })
    } else {
        const annotationItems = supportedLombokAnnotations.map(name => {
            return {
                label: `@${name}`,
                description: annotationsDescriptions[supportedLombokAnnotations.indexOf(name)],
                buttons: [{
                    iconPath: new ThemeIcon("link-external"),
                    tooltip: "Reference"
                }]
            };
        });
        const itemsToDelombok = annotationItems.filter((item) => {
            return annotationResponse.annotations.indexOf(item.label.split('@')[1]) >= 0;
        });
        const itemsToLombok = annotationItems.filter((item) => {
            return annotationResponse.annotations.indexOf(item.label.split('@')[1]) < 0;
        });
        const showItems: QuickPickItem[] = [];
        showItems.push({
            label: "Unselect to Delombok",
            kind: QuickPickItemKind.Separator
        });
        showItems.push(...itemsToDelombok);
        showItems.push({
            label: "Select to Lombok",
            kind: QuickPickItemKind.Separator
        });
        showItems.push(...itemsToLombok);
        let selectedItems: readonly QuickPickItem[] = [];
        const disposables: Disposable[] = [];
        try {
            selectedItems = await new Promise<readonly QuickPickItem[]>(async (resolve, reject) => {
                const pickBox = window.createQuickPick();
                pickBox.items = showItems;
                pickBox.canSelectMany = true;
                pickBox.ignoreFocusOut = true;
                pickBox.selectedItems = itemsToDelombok;
                pickBox.placeholder = 'Add or remove Lombok annotations in class';
                disposables.push(
                    pickBox.onDidTriggerItemButton(e => {
                        env.openExternal(Uri.parse(annotationLinks[supportedLombokAnnotations.indexOf(e.item.label.split('@')[1])]));
                    }),
                    pickBox.onDidAccept(() => {
                        resolve(pickBox.selectedItems);
                    }),
                    pickBox.onDidHide(() => {
                        reject();
                    })
                );
                disposables.push(pickBox);
                pickBox.show();
            });
        } catch (err) {
            // return when the quickpick is cancelled.
            sendInfo("", {
                operationName: "cancelLombokAction",
            });
            return;
        } finally {
            disposables.forEach(d => d.dispose());
        }
        annotationsAfter = selectedItems.map(item => {
            return item.label.split('@')[1];
        });
    }
    const lombokParams: LombokRequestParams = {
        context: params,
        annotationsBefore: annotationResponse.annotations,
        annotationsAfter
    };

    const lombok: string[] = [];
    const delombok: string[] = [];
    for (const annotation of lombokParams.annotationsBefore) {
        if (!lombokParams.annotationsAfter.includes(annotation)) {
            delombok.push(annotation);
        }
    }
    for (const annotation of lombokParams.annotationsAfter) {
        if (!lombokParams.annotationsBefore.includes(annotation)) {
            lombok.push(annotation);
        }
    }
    if (!lombok.length && !delombok.length) {
        sendInfo("", {
            operationName: "cancelLombokAction",
        });
        return;
    }
    const startAt = Date.now();
    let workspaceEdit;
    try {
        workspaceEdit = await executeJavaLanguageServerCommand(Commands.JAVA_CODEACTION_LOMBOK, JSON.stringify(lombokParams)) as WorkspaceEdit;
    } finally {
        sendInfo("", {
            operationName: "applyLombokAction",
            lombok: JSON.stringify(lombok),
            delombok: JSON.stringify(delombok),
            duration: Date.now() - startAt,
        });
    }
    await applyWorkspaceEdit(workspaceEdit);
    await revealWorkspaceEdit(workspaceEdit);
    // organize imports silently to fix missing annotation imports
    await commands.executeCommand(Commands.ORGANIZE_IMPORTS_SILENTLY, params.textDocument.uri.toString());
}

function getSelectedAnnotations(text: string): string[] {
    return supportedLombokAnnotations.filter((item) => text.includes(`@${item}`));
}

export class LombokCodeActionProvider implements CodeActionProvider {
    provideCodeActions(document: TextDocument, range: Range | Selection, context: CodeActionContext, _token: CancellationToken): ProviderResult<(CodeAction | Command)[]> {
        const params: CodeActionParams = {
            textDocument: codeConverter.asTextDocumentIdentifier(document),
            range: codeConverter.asRange(range),
            context: codeConverter.asCodeActionContext(context)
        };
        const selectText = document.getText(range);
        let codeActionTitle = "Lombok...";
        let selectedAnnotations: string[] = [];
        if (selectText !== "") {
            selectedAnnotations = getSelectedAnnotations(selectText);
            if (selectedAnnotations.length === 1) {
                codeActionTitle = `Delombok '${selectedAnnotations[0]}'`;
            } else if (selectedAnnotations.length > 1) {
                codeActionTitle = `Delombok ${selectedAnnotations.length} annotations`;
            }
        }

        return [
            {
                title: codeActionTitle,
                kind: CodeActionKind.Refactor,
                command: {
                    title: codeActionTitle,
                    command: Commands.CODEACTION_LOMBOK,
                    arguments: [params, selectedAnnotations]
                },
            }
        ];
    }
}
