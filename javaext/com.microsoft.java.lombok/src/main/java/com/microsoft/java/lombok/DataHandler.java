package com.microsoft.java.lombok;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.ls.core.internal.codemanipulation.GenerateGetterSetterOperation.AccessorKind;
import org.eclipse.jdt.ls.core.internal.text.correction.SourceAssistProcessor;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;

import com.microsoft.java.lombok.ConstructorHandler.ConstructorKind;

public class DataHandler {

    public static TextEdit generateMethods(CodeActionParams params, List<String> existingAnnotations,
            IProgressMonitor monitor) {
        IType type = SourceAssistProcessor.getSelectionType(params, monitor);
        if (type == null || type.getCompilationUnit() == null) {
            return null;
        }

        TextEdit textEdit = new MultiTextEdit();
        if (!existingAnnotations.contains(AnnotationHandler.lombokAllArgsConstructorAnnotation)) {
            TextEdit allArgsConstructorTextEdit = ConstructorHandler.generateMethods(params, monitor,
                    ConstructorKind.ALL_ARGS);
            if (allArgsConstructorTextEdit != null) {
                textEdit.addChild(allArgsConstructorTextEdit);
            }
        }

        if (!existingAnnotations.contains(AnnotationHandler.lombokNoArgsConstructorAnnotation)) {
            TextEdit noArgConstructorTextEdit = ConstructorHandler.generateMethods(params, monitor,
                    ConstructorKind.NO_ARG);
            if (noArgConstructorTextEdit != null) {
                textEdit.addChild(noArgConstructorTextEdit);
            }
        }

        // check existing annotations and use reverse logics to get AccessorKind
        AccessorKind kind = null;
        boolean getterExists = existingAnnotations.contains(AnnotationHandler.lombokGetterAnnotation);
        boolean setterExists = existingAnnotations.contains(AnnotationHandler.lombokSetterAnnotation);
        if (getterExists && setterExists) {
            kind = null;
        } else if (getterExists) {
            kind = AccessorKind.SETTER;
        } else if (setterExists) {
            kind = AccessorKind.GETTER;
        } else {
            kind = AccessorKind.BOTH;
        }
        if (kind != null) {
            TextEdit accessorsTextEdit = GetterSetterHandler.generateMethods(params, kind, monitor);
            if (accessorsTextEdit != null) {
                textEdit.addChild(accessorsTextEdit);
            }
        }

        if (!existingAnnotations.contains(AnnotationHandler.lombokToStringAnnotation)) {
            TextEdit toStringTextEdit = ToStringHandler.generateMethods(params, monitor);
            if (toStringTextEdit != null) {
                textEdit.addChild(toStringTextEdit);
            }
        }

        if (!existingAnnotations.contains(AnnotationHandler.lombokEqualsAndHashCodeAnnotation)) {
            TextEdit hashCodeEqualsTextEdit = EqualsAndHashCodeHandler.generateMethods(params, monitor);
            if (hashCodeEqualsTextEdit != null) {
                textEdit.addChild(hashCodeEqualsTextEdit);
            }
        }

        return textEdit;
    }

    public static void removeMethods(IType type, ListRewrite rewriter, IProgressMonitor monitor) {
        // TODO: cache calculated binding
        ConstructorHandler.removeMethods(type, rewriter, ConstructorKind.NO_ARG, monitor);
        ConstructorHandler.removeMethods(type, rewriter, ConstructorKind.ALL_ARGS, monitor);
        GetterSetterHandler.removeMethods(type, rewriter, AccessorKind.BOTH, monitor);
        Utils.removeMethods(type, rewriter, ToStringHandler.toStringMethods, monitor);
        Utils.removeMethods(type, rewriter, EqualsAndHashCodeHandler.equalsAndHashCodeMethods, monitor);
    }
}
