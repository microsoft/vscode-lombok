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
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.manipulation.CoreASTProvider;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;
import org.eclipse.jdt.ls.core.internal.text.correction.SourceAssistProcessor;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

public class JavaCodeActionHandler {
    public static WorkspaceEdit lombokAction(LombokRequestParams params, IProgressMonitor monitor) {
        IType type = SourceAssistProcessor.getSelectionType(params.context, monitor);
        if (type == null || type.getCompilationUnit() == null) {
            return null;
        }

        List<String> annotationsToLombok = new ArrayList<>(Arrays.asList(params.annotationsToLombok));
        List<String> annotationsToRemoveMethods = new ArrayList<>(Arrays.asList(params.annotationsToLombok));
        List<String> annotationsToDelombok = new ArrayList<>(Arrays.asList(params.annotationsToDelombok));
        List<String> annotationsToAddMethods = new ArrayList<>(Arrays.asList(params.annotationsToDelombok));

        // delombok @Data will add methods which lombok following annotations will
        // remove. We don't remove these methods.
        if (annotationsToDelombok.contains(AnnotationHandler.lombokDataAnnotation)) {
            annotationsToRemoveMethods.remove(AnnotationHandler.lombokNoArgsConstructorAnnotation);
            annotationsToRemoveMethods.remove(AnnotationHandler.lombokAllArgsConstructorAnnotation);
            annotationsToRemoveMethods.remove(AnnotationHandler.lombokToStringAnnotation);
            annotationsToRemoveMethods.remove(AnnotationHandler.lombokEqualsAndHashCodeAnnotation);
        }

        // lombok @Data will remove methods which delombok following annotations will
        // add. We don't add these methods.
        if (annotationsToLombok.contains(AnnotationHandler.lombokDataAnnotation)) {
            annotationsToAddMethods.remove(AnnotationHandler.lombokNoArgsConstructorAnnotation);
            annotationsToAddMethods.remove(AnnotationHandler.lombokAllArgsConstructorAnnotation);
            annotationsToAddMethods.remove(AnnotationHandler.lombokToStringAnnotation);
            annotationsToAddMethods.remove(AnnotationHandler.lombokEqualsAndHashCodeAnnotation);
        }

        TextEdit textEdit = new MultiTextEdit();
        TextEdit removeAnnotationEdit = removeAnnotations(params.context, annotationsToDelombok, monitor);
        if (removeAnnotationEdit != null) {
            textEdit.addChild(removeAnnotationEdit);
        }

        TextEdit removeMethodEdit = removeMethods(params.context, annotationsToRemoveMethods, monitor);
        if (removeMethodEdit != null) {
            textEdit.addChild(removeMethodEdit);
        }

        TextEdit addAnnotationEdit = addAnnotations(params.context, annotationsToLombok, monitor);
        if (addAnnotationEdit != null) {
            textEdit.addChild(addAnnotationEdit);
        }

        TextEdit addMethodEdit = addMethods(params.context, annotationsToAddMethods, monitor);
        if (addMethodEdit != null) {
            textEdit.addChild(addMethodEdit);
        }

        return (textEdit == null) ? null
                : SourceAssistProcessor.convertToWorkspaceEdit(type.getCompilationUnit(), textEdit);
    }

    public static TextEdit addMethods(CodeActionParams params, List<String> annotations, IProgressMonitor monitor) {
        if (annotations.isEmpty()) {
            return null;
        }

        TextEdit textEdit = new MultiTextEdit();

        if (annotations.contains(AnnotationHandler.lombokDataAnnotation)) {
            TextEdit delombokDataEdit = DataHandler.generateDataTextEdit(params, monitor);
            if (delombokDataEdit != null) {
                textEdit.addChild(delombokDataEdit);
            }
            // @Data annotation include @NoArgsConstructor, @AllArgsConstructor, @ToString
            // and @EqualsAndHashCode, we don't need to process them.
            annotations.remove(AnnotationHandler.lombokNoArgsConstructorAnnotation);
            annotations.remove(AnnotationHandler.lombokAllArgsConstructorAnnotation);
            annotations.remove(AnnotationHandler.lombokToStringAnnotation);
            annotations.remove(AnnotationHandler.lombokEqualsAndHashCodeAnnotation);
        }

        if (annotations.contains(AnnotationHandler.lombokNoArgsConstructorAnnotation)) {
            TextEdit delombokNoArgsConstructorEdit = ConstructorHandler.generateConstructor(params, monitor,
                    ConstructorKind.NO_ARG);
            if (delombokNoArgsConstructorEdit != null) {
                textEdit.addChild(delombokNoArgsConstructorEdit);
            }
        }

        if (annotations.contains(AnnotationHandler.lombokAllArgsConstructorAnnotation)) {
            TextEdit delombokAllArgsConstructorEdit = ConstructorHandler.generateConstructor(params, monitor,
                    ConstructorKind.ALL_ARGS);
            if (delombokAllArgsConstructorEdit != null) {
                textEdit.addChild(delombokAllArgsConstructorEdit);
            }
        }

        if (annotations.contains(AnnotationHandler.lombokToStringAnnotation)) {
            TextEdit delombokToStringEdit = ToStringHandler.generateToString(params, monitor);
            if (delombokToStringEdit != null) {
                textEdit.addChild(delombokToStringEdit);
            }
        }

        if (annotations.contains(AnnotationHandler.lombokEqualsAndHashCodeAnnotation)) {
            TextEdit delombokHashCodeEqualsEdit = EqualsAndHashCodeHandler.generateHashCodeEquals(params, monitor);
            if (delombokHashCodeEqualsEdit != null) {
                textEdit.addChild(delombokHashCodeEqualsEdit);
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
                    ASTNode node = astRoot.findDeclaringNode(item);
                    fRewrite.getASTRewrite().remove(node, null);
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
        public String[] annotationsToLombok;
        public String[] annotationsToDelombok;
    }

}
