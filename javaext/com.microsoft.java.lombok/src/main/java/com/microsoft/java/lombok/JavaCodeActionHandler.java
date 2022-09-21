package com.microsoft.java.lombok;

import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.text.edits.TextEdit;

import com.microsoft.java.lombok.ConstructorHandler.ConstructorKind;

import org.eclipse.text.edits.MultiTextEdit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.manipulation.CoreASTProvider;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;
import org.eclipse.jdt.ls.core.internal.codemanipulation.GenerateGetterSetterOperation.AccessorKind;
import org.eclipse.jdt.ls.core.internal.text.correction.SourceAssistProcessor;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

public class JavaCodeActionHandler {
    public static WorkspaceEdit lombokAction(LombokRequestParams params, IProgressMonitor monitor) {
        IType type = SourceAssistProcessor.getSelectionType(params.context, monitor);
        if (type == null || type.getCompilationUnit() == null) {
            return null;
        }

        List<String> annotationsToLombok = new ArrayList<>();
        List<String> annotationsToDelombok = new ArrayList<>();

        List<String> annotationsBefore = Arrays.asList(params.annotationsBefore);
        List<String> annotationsAfter = Arrays.asList(params.annotationsAfter);
        for (String annotation : AnnotationHandler.lombokAnnotationSet) {
            if (annotationsBefore.contains(annotation) && !annotationsAfter.contains(annotation)) {
                annotationsToDelombok.add(annotation);
            } else if (!annotationsBefore.contains(annotation) && annotationsAfter.contains(annotation)) {
                annotationsToLombok.add(annotation);
            }
        }

        TextEdit textEdit = new MultiTextEdit();
        TextEdit removeAnnotationEdit = removeAnnotations(params.context, annotationsToDelombok, monitor);
        if (removeAnnotationEdit != null) {
            textEdit.addChild(removeAnnotationEdit);
        }

        TextEdit addAnnotationEdit = addAnnotations(params.context, annotationsToLombok, monitor);
        if (addAnnotationEdit != null) {
            textEdit.addChild(addAnnotationEdit);
        }

        TextEdit addMethodEdit = addMethods(params.context, annotationsToDelombok, annotationsAfter, monitor);
        if (addMethodEdit != null) {
            textEdit.addChild(addMethodEdit);
        }

        TextEdit removeMethodEdit = removeMethods(params.context, annotationsToLombok, monitor);
        if (removeMethodEdit != null) {
            textEdit.addChild(removeMethodEdit);
        }

        return (textEdit == null) ? null
                : SourceAssistProcessor.convertToWorkspaceEdit(type.getCompilationUnit(), textEdit);
    }

    public static TextEdit addMethods(CodeActionParams params, List<String> annotations, List<String> annotationsAfter, IProgressMonitor monitor) {
        if (annotations.isEmpty()) {
            return null;
        }

        TextEdit textEdit = new MultiTextEdit();

        if (annotations.contains(AnnotationHandler.lombokDataAnnotation)) {
            TextEdit delombokDataEdit = DataHandler.generateMethods(params, annotationsAfter, monitor);
            if (delombokDataEdit != null) {
                textEdit.addChild(delombokDataEdit);
            }
            // @Data annotation include @NoArgsConstructor, @AllArgsConstructor, @ToString
            // and @EqualsAndHashCode, we don't need to process them.
            annotations.remove(AnnotationHandler.lombokNoArgsConstructorAnnotation);
            annotations.remove(AnnotationHandler.lombokAllArgsConstructorAnnotation);
            annotations.remove(AnnotationHandler.lombokGetterAnnotation);
            annotations.remove(AnnotationHandler.lombokSetterAnnotation);
            annotations.remove(AnnotationHandler.lombokToStringAnnotation);
            annotations.remove(AnnotationHandler.lombokEqualsAndHashCodeAnnotation);
        }

        if (!annotationsAfter.contains(AnnotationHandler.lombokDataAnnotation)) {
            // only add methods when @Data is not exist
            if (annotations.contains(AnnotationHandler.lombokNoArgsConstructorAnnotation)) {
                TextEdit delombokNoArgsConstructorEdit = ConstructorHandler.generateMethods(params, monitor,
                        ConstructorKind.NO_ARG);
                if (delombokNoArgsConstructorEdit != null) {
                    textEdit.addChild(delombokNoArgsConstructorEdit);
                }
            }

            if (annotations.contains(AnnotationHandler.lombokAllArgsConstructorAnnotation)) {
                TextEdit delombokAllArgsConstructorEdit = ConstructorHandler.generateMethods(params, monitor,
                        ConstructorKind.ALL_ARGS);
                if (delombokAllArgsConstructorEdit != null) {
                    textEdit.addChild(delombokAllArgsConstructorEdit);
                }
            }

            AccessorKind kind = Utils.getAccessorKindFromAnnotations(annotations);
            if (kind != null) {
                TextEdit delombokAccessorsEdit = GetterSetterHandler.generateMethods(params, kind, monitor);
                if (delombokAccessorsEdit != null) {
                    textEdit.addChild(delombokAccessorsEdit);
                }
            }

            if (annotations.contains(AnnotationHandler.lombokToStringAnnotation)) {
                TextEdit delombokToStringEdit = ToStringHandler.generateMethods(params, monitor);
                if (delombokToStringEdit != null) {
                    textEdit.addChild(delombokToStringEdit);
                }
            }

            if (annotations.contains(AnnotationHandler.lombokEqualsAndHashCodeAnnotation)) {
                TextEdit delombokHashCodeEqualsEdit = EqualsAndHashCodeHandler.generateMethods(params, monitor);
                if (delombokHashCodeEqualsEdit != null) {
                    textEdit.addChild(delombokHashCodeEqualsEdit);
                }
            }
        }
        return textEdit;
    }

    public static TextEdit removeAnnotations(CodeActionParams params, List<String> annotations,
            IProgressMonitor monitor) {
        if (annotations.isEmpty()) {
            return null;
        }

        IType type = SourceAssistProcessor.getSelectionType(params);
        try {
            CompilationUnit astRoot = CoreASTProvider.getInstance().getAST(type.getCompilationUnit(),
                    CoreASTProvider.WAIT_YES, monitor);
            if (astRoot == null) {
                return null;
            }
            ITypeBinding typeBinding = ASTNodes.getTypeBinding(astRoot, type);
            if (typeBinding == null) {
                return null;
            }
            CompilationUnitRewrite fRewrite = new CompilationUnitRewrite((ICompilationUnit) astRoot.getTypeRoot(),
                    astRoot);

            boolean hasAnnotation = false;
            for (IAnnotationBinding item : typeBinding.getAnnotations()) {
                if (annotations.contains(item.getName())) {
                    hasAnnotation = true;
                    fRewrite.getASTRewrite().remove(astRoot.findDeclaringNode(item), null);
                }
            }

            if (!hasAnnotation) {
                return null;
            }

            CompilationUnitChange change = fRewrite.createChange(true);
            if (change == null) {
                return null;
            }
            return change.getEdit();
        } catch (Exception e) {
            JavaLanguageServerPlugin.logException("Remove Lombok annotations", e);
        }
        return null;
    }

    public static TextEdit removeMethods(CodeActionParams params, List<String> annotations, IProgressMonitor monitor) {
        if (annotations.isEmpty()) {
            return null;
        }
        try {
            IType type = SourceAssistProcessor.getSelectionType(params);
            CompilationUnit astRoot = CoreASTProvider.getInstance().getAST(type.getCompilationUnit(),
                    CoreASTProvider.WAIT_YES, monitor);
            if (astRoot == null) {
                return null;
            }
            ITypeBinding typeBinding = ASTNodes.getTypeBinding(astRoot, type);
            if (typeBinding == null) {
                return null;
            }
            CompilationUnitRewrite fRewrite = new CompilationUnitRewrite((ICompilationUnit) astRoot.getTypeRoot(),
                    astRoot);
            AbstractTypeDeclaration declaration = (AbstractTypeDeclaration) astRoot.findDeclaringNode(typeBinding);
            ListRewrite rewriter = fRewrite.getASTRewrite().getListRewrite(declaration,
                    declaration.getBodyDeclarationsProperty());
            // remove exists methods
            if (annotations.contains(AnnotationHandler.lombokDataAnnotation)) {
                DataHandler.removeMethods(type, rewriter, monitor);
            }
            if (annotations.contains(AnnotationHandler.lombokNoArgsConstructorAnnotation)) {
                ConstructorHandler.removeMethods(type, rewriter, ConstructorKind.NO_ARG, monitor);
            }
            if (annotations.contains(AnnotationHandler.lombokAllArgsConstructorAnnotation)) {
                ConstructorHandler.removeMethods(type, rewriter, ConstructorKind.ALL_ARGS, monitor);
            }
            if (annotations.contains(AnnotationHandler.lombokGetterAnnotation)) {
                GetterSetterHandler.removeMethods(type, rewriter, AccessorKind.GETTER, monitor);
            }
            if (annotations.contains(AnnotationHandler.lombokSetterAnnotation)) {
                GetterSetterHandler.removeMethods(type, rewriter, AccessorKind.SETTER, monitor);
            }
            if (annotations.contains(AnnotationHandler.lombokToStringAnnotation)) {
                ToStringHandler.removeMethods(type, rewriter, monitor);
            }
            if (annotations.contains(AnnotationHandler.lombokEqualsAndHashCodeAnnotation)) {
                EqualsAndHashCodeHandler.removeMethods(type, rewriter, monitor);
            }

            CompilationUnitChange change = fRewrite.createChange(true);
            if (change == null) {
                return null;
            }
            return change.getEdit();
        } catch (Exception e) {
            JavaLanguageServerPlugin.logException("Remove Lombok methods", e);
        }
        return null;
    }

    public static TextEdit addAnnotations(CodeActionParams params, List<String> annotations, IProgressMonitor monitor) {
        if (annotations.isEmpty()) {
            return null;
        }

        IType type = SourceAssistProcessor.getSelectionType(params);
        try {
            CompilationUnit astRoot = CoreASTProvider.getInstance().getAST(type.getCompilationUnit(),
                    CoreASTProvider.WAIT_YES, monitor);
            if (astRoot == null) {
                return null;
            }
            ITypeBinding typeBinding = ASTNodes.getTypeBinding(astRoot, type);
            if (typeBinding == null) {
                return null;
            }
            CompilationUnitRewrite fRewrite = new CompilationUnitRewrite((ICompilationUnit) astRoot.getTypeRoot(),
                    astRoot);
            AbstractTypeDeclaration declaration = (AbstractTypeDeclaration) astRoot.findDeclaringNode(typeBinding);
            ListRewrite rewriter = fRewrite.getASTRewrite().getListRewrite(declaration,
                    declaration.getModifiersProperty());
            AST ast = fRewrite.getASTRewrite().getAST();

            for (String annotaion : annotations) {
                Annotation marker = ast.newMarkerAnnotation();
                marker.setTypeName(ast.newName(annotaion)); // $NON-NLS-1$
                rewriter.insertFirst(marker, null);
            }

            CompilationUnitChange change = fRewrite.createChange(true);
            if (change == null) {
                return null;
            }
            return change.getEdit();

        } catch (Exception e) {
            JavaLanguageServerPlugin.logException("Add Lombok annotations", e);
        }
        return null;
    }

    public static class LombokRequestParams {
        public CodeActionParams context;
        public String[] annotations;
        public String[] annotationsBefore;
        public String[] annotationsAfter;
    }

}
