
import { commands, ExtensionContext, window, workspace, CodeActionProvider, CancellationToken, CodeAction, CodeActionContext, Command, ProviderResult, Range, Selection, TextDocument, TextEditorRevealType, CodeActionProviderMetadata, CodeActionKind, ThemeIcon, Uri, env, Disposable, QuickPickItem } from "vscode";
import { Commands, executeJavaLanguageServerCommand } from './commands';
import * as ProtocolConverter from "vscode-languageclient/lib/common/protocolConverter";
import * as CodeConverter from "vscode-languageclient/lib/common/codeConverter";
import { CodeActionParams, WorkspaceEdit } from 'vscode-languageclient';
import { AnnotationResponse, LombokRequestParams } from './protocol';

const protoConverter: ProtocolConverter.Converter = ProtocolConverter.createConverter(undefined, undefined);
const codeConverter: CodeConverter.Converter = CodeConverter.createConverter();

const allLombokAnnotations = ["Data", "NoArgsConstructor", "AllArgsConstructor", "ToString", "EqualsAndHashCode"];
const annotationsDescriptions = [
    "Bundles the features of @ToString, @EqualsAndHashCode, @Getter, @Setter and @RequiredArgsConstructor together",
    "Generate a constructor with no parameters.",
    "Generates a constructor with 1 parameter for each field in your class.",
    "Generates a toString for you.",
    "Generates hashCode and equals implementations from the fields of your object."
];

const annotationLinks = [
    "https://projectlombok.org/features/Data",
    "https://projectlombok.org/features/constructor",
    "https://projectlombok.org/features/constructor",
    "https://projectlombok.org/features/ToString",
    "https://projectlombok.org/features/EqualsAndHashCode"
];

async function applyWorkspaceEdit(workspaceEdit: WorkspaceEdit): Promise<boolean> {
    const edit = protoConverter.asWorkspaceEdit(workspaceEdit);
    if (edit) {
        return workspace.applyEdit(edit);
    }
    else {
        return Promise.resolve(true);
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

export function registerCodaActionCommand(context: ExtensionContext) {
    context.subscriptions.push(commands.registerCommand(Commands.CODEACTION_LOMBOK, async (params: CodeActionParams) => {
        params.context.diagnostics = [];
        const selectAnnotationResponse = await executeJavaLanguageServerCommand(Commands.JAVA_CODEACTION_SELECTANNOTATION, JSON.stringify(params)) as AnnotationResponse;
        let selectAnnotations = [];
        let lombokAnnotations = [];
        let delombokAnnotations = [];
        if (selectAnnotationResponse.annotations.length > 0) {
            delombokAnnotations = selectAnnotationResponse.annotations;
        }
        else {
            const annotationResponse = await executeJavaLanguageServerCommand(Commands.JAVA_CODEACTION_LOMBOK_ANNOTATIONS, JSON.stringify(params)) as AnnotationResponse;
            if (!annotationResponse) {
                return;
            }
            const annotationItems = allLombokAnnotations.map(name => {
                return {
                    label: `@${name}`,
                    description: annotationsDescriptions[allLombokAnnotations.indexOf(name)],
                    buttons: [{
                        iconPath: new ThemeIcon("link-external"),
                        tooltip: "Reference"
                    }]
                };
            });
            const initSelectItems = annotationItems.filter((item) => {
                return annotationResponse.annotations.indexOf(item.label.split('@')[1]) >= 0;
            });

            const disposables: Disposable[] = [];
            const selectItems = await new Promise<readonly QuickPickItem[]>(async (resolve, reject) => {
                const pickBox = window.createQuickPick();
                pickBox.items = annotationItems;
                pickBox.canSelectMany = true;
                pickBox.ignoreFocusOut = true;
                pickBox.selectedItems = initSelectItems;
                pickBox.placeholder = 'Select the Lombok Annotation';

                disposables.push(
                    pickBox.onDidTriggerItemButton(e => {
                        const annotation = e.item.label.split('@')[1];
                        env.openExternal(Uri.parse(annotationLinks[allLombokAnnotations.indexOf(annotation)]));
                    }),
                    pickBox.onDidAccept(() => {
                        resolve(pickBox.selectedItems);
                    }),
                    pickBox.onDidHide(() => {
                        resolve(pickBox.selectedItems);
                    })
                );
                disposables.push(pickBox);
                pickBox.show();
            });
            disposables.forEach(d => d.dispose());

            selectAnnotations = selectItems.map(item => {
                return item.label.split('@')[1];
            });

            delombokAnnotations = annotationResponse.annotations.filter((item) => {
                return selectAnnotations.indexOf(item) < 0;
            });
            lombokAnnotations = selectAnnotations.filter((item) => {
                return annotationResponse.annotations.indexOf(item) < 0;
            });
        }
        if (delombokAnnotations.length === 0 && lombokAnnotations.length === 0) {
            return;
        }
        const delombokParams: LombokRequestParams = {
            context: params,
            lombokAnnotations,
            delombokAnnotations
        };
        const workspaceEdit = await executeJavaLanguageServerCommand(Commands.JAVA_CODEACTION_LOMBOK, JSON.stringify(delombokParams)) as WorkspaceEdit;
        await applyWorkspaceEdit(workspaceEdit);
        await revealWorkspaceEdit(workspaceEdit);
    }));
}


export class LombokCodeActionProvider implements CodeActionProvider {
    provideCodeActions(document: TextDocument, range: Range | Selection, context: CodeActionContext, token: CancellationToken): ProviderResult<(CodeAction | Command)[]> {
        const params: CodeActionParams = {
            textDocument: codeConverter.asTextDocumentIdentifier(document),
            range: codeConverter.asRange(range),
            context: codeConverter.asCodeActionContext(context)
        };
        return [
            {
                title: "Lombok",
                kind: CodeActionKind.Refactor,
                command: {
                    title: "Lombok",
                    command: Commands.CODEACTION_LOMBOK,
                    arguments: [params]
                },
            }
        ];
    }
}