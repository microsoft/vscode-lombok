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
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;
import org.eclipse.jdt.core.manipulation.CoreASTProvider;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;
import org.eclipse.jdt.ls.core.internal.text.correction.SourceAssistProcessor;
import org.eclipse.jdt.internal.corext.codemanipulation.ContextSensitiveImportRewriteContext;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

public class JavaCodeActionHandler {
    public static WorkspaceEdit lombokAction(LombokRequestParams params, IProgressMonitor monitor) {
        IType type = SourceAssistProcessor.getSelectionType(params.context, monitor);
        if (type == null || type.getCompilationUnit() == null) {
            return null;
        }

        List<String> lombokAnnotations = new ArrayList<>(Arrays.asList(params.lombokAnnotations));
        List<String> removeMethodsAnnotations = new ArrayList<>(Arrays.asList(params.lombokAnnotations));
        List<String> delombokAnnotations = new ArrayList<>(Arrays.asList(params.delombokAnnotations));
        List<String> addMethodskAnnotations = new ArrayList<>(Arrays.asList(params.delombokAnnotations));

        // delombok @Data will add methods which lombok following annotations will
        // remove. We don't remove these methods.
        if (delombokAnnotations.contains(AnnotationHandler.lombokDataAnnotaion)) {
            removeMethodsAnnotations.remove(AnnotationHandler.lombokNoArgsConstructorAnnotaion);
            removeMethodsAnnotations.remove(AnnotationHandler.lombokAllArgsConstructorAnnotaion);
            removeMethodsAnnotations.remove(AnnotationHandler.lombokToStringAnnotation);
            removeMethodsAnnotations.remove(AnnotationHandler.lombokEqualsAndHashCodeAnnotation);
        }

        // lombok @Data will remove methods which delombok following annotations will
        // add. We don't add these methods.
        if (lombokAnnotations.contains(AnnotationHandler.lombokDataAnnotaion)) {
            addMethodskAnnotations.remove(AnnotationHandler.lombokNoArgsConstructorAnnotaion);
            addMethodskAnnotations.remove(AnnotationHandler.lombokAllArgsConstructorAnnotaion);
            addMethodskAnnotations.remove(AnnotationHandler.lombokToStringAnnotation);
            addMethodskAnnotations.remove(AnnotationHandler.lombokEqualsAndHashCodeAnnotation);
        }

        TextEdit textEdit = new MultiTextEdit();
        TextEdit removeAnnotationEdit = removeAnnotations(params.context, delombokAnnotations, monitor);
        if (removeAnnotationEdit != null) {
            textEdit.addChild(removeAnnotationEdit);
        }

        TextEdit removeMethodEdit = removeMethods(params.context, removeMethodsAnnotations, monitor);
        if (removeMethodEdit != null) {
            textEdit.addChild(removeMethodEdit);
        }

        TextEdit addAnnotationEdit = addAnnotations(params.context, lombokAnnotations, monitor);
        if (addAnnotationEdit != null) {
            textEdit.addChild(addAnnotationEdit);
        }

        TextEdit addMethodEdit = addMethods(params.context, addMethodskAnnotations, monitor);
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

        if (annotations.contains(AnnotationHandler.lombokDataAnnotaion)) {
            TextEdit delombokDataEdit = DataHandler.generateDataTextEdit(params, monitor);
            if (delombokDataEdit != null) {
                textEdit.addChild(delombokDataEdit);
            }
            // @Data annotation include @NoArgsConstructor, @AllArgsConstructor, @ToString
            // and @EqualsAndHashCode, we don't need to delombok them.
            annotations.remove(AnnotationHandler.lombokNoArgsConstructorAnnotaion);
            annotations.remove(AnnotationHandler.lombokAllArgsConstructorAnnotaion);
            annotations.remove(AnnotationHandler.lombokToStringAnnotation);
            annotations.remove(AnnotationHandler.lombokEqualsAndHashCodeAnnotation);
        }

        if (annotations.contains(AnnotationHandler.lombokNoArgsConstructorAnnotaion)) {
            TextEdit delombokNoArgsConstructorEdit = ConstructorHandler.generateConstructor(params, monitor,
                    ConstructorKind.NOARGCONSTRUCTOR);
            if (delombokNoArgsConstructorEdit != null) {
                textEdit.addChild(delombokNoArgsConstructorEdit);
            }
        }

        if (annotations.contains(AnnotationHandler.lombokAllArgsConstructorAnnotaion)) {
            TextEdit delombokAllArgsConstructorEdit = ConstructorHandler.generateConstructor(params, monitor,
                    ConstructorKind.ALLARGSCONSTRUCTOR);
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
            AbstractTypeDeclaration declaration = (AbstractTypeDeclaration) astRoot.findDeclaringNode(typeBinding);
            ListRewrite rewriter = fRewrite.getASTRewrite().getListRewrite(declaration,
                    declaration.getBodyDeclarationsProperty());

            boolean hasAnnotation = false;
            IAnnotationBinding[] allAnnotations = typeBinding.getAnnotations();
            for (IAnnotationBinding item : allAnnotations) {
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
            if (annotations.contains(AnnotationHandler.lombokDataAnnotaion)) {
                DataHandler.removeMethods(type, rewriter, monitor);
            }
            if (annotations.contains(AnnotationHandler.lombokNoArgsConstructorAnnotaion)) {
                ConstructorHandler.removeMethods(type, rewriter, ConstructorKind.NOARGCONSTRUCTOR, monitor);
            }
            if (annotations.contains(AnnotationHandler.lombokAllArgsConstructorAnnotaion)) {
                ConstructorHandler.removeMethods(type, rewriter, ConstructorKind.ALLARGSCONSTRUCTOR, monitor);
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
            ImportRewrite imports = fRewrite.getImportRewrite();
            AST ast = fRewrite.getASTRewrite().getAST();
            ASTNode root = declaration.getRoot();
            ImportRewriteContext context = null;
            if (root instanceof CompilationUnit) {
                context = new ContextSensitiveImportRewriteContext((CompilationUnit) root,
                        declaration.getStartPosition(), imports);
            }

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
        public String[] lombokAnnotations;
        public String[] delombokAnnotations;
    }

}
