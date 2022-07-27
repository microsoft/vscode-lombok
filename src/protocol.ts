
import {
    CodeActionParams,
} from 'vscode-languageclient';

export interface AnnotationResponse {
    annotations: string[];
}

export interface LombokRequestParams {
    context: CodeActionParams;
    lombokAnnotations: string[];
    delombokAnnotations: string[];
}