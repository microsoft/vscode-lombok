package com.microsoft.java.lombok;

import java.util.Set;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.manipulation.CoreASTProvider;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.ls.core.internal.JDTUtils;
import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;
import org.eclipse.jdt.ls.core.internal.corrections.InnovationContext;
import org.eclipse.jdt.ls.core.internal.handlers.CodeActionHandler;
import org.eclipse.jdt.ls.core.internal.text.correction.QuickAssistProcessor;
import org.eclipse.jdt.ls.core.internal.text.correction.SourceAssistProcessor;
import org.eclipse.jdt.core.dom.IAnnotationBinding;

import org.eclipse.lsp4j.CodeActionParams;

public class AnnotationHandler {
    public static Set<String> lombokAnnotationSet = new HashSet<String>(Arrays.asList(
            "Data", "NoArgsConstructor", "AllArgsConstructor", "ToString", "EqualsAndHashCode"));
    public static final String lombokDataAnnotaion = "Data";
    public static final String lombokNoArgsConstructorAnnotaion = "NoArgsConstructor";
    public static final String lombokAllArgsConstructorAnnotaion = "AllArgsConstructor";
    public static final String lombokToStringAnnotation = "ToString";
    public static final String lombokEqualsAndHashCodeAnnotation = "EqualsAndHashCode";

    public static AnnotationResponse findLombokAnnotation(CodeActionParams params, IProgressMonitor monitor) {
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

            List<String> result = new ArrayList<String>();
            IAnnotationBinding[] annotations = typeBinding.getAnnotations();
            for (IAnnotationBinding item : annotations) {
                if (lombokAnnotationSet.contains(item.getName())) {
                    result.add(item.getName());
                }
            }
            AnnotationResponse response = new AnnotationResponse(result.toArray(new String[result.size()]));
            return response;
        } catch (Exception e) {
            JavaLanguageServerPlugin.logException("Find Lombok annotation", e);
        }
        return null;
    }

    public static List<String> getSelectAnnotations(CodeActionParams params, IProgressMonitor monitor) {
        List<String> result = new ArrayList<String>();
        try {
            IType type = SourceAssistProcessor.getSelectionType(params);
            final ICompilationUnit unit = JDTUtils.resolveCompilationUnit(params.getTextDocument().getUri());
            CompilationUnit astRoot = CodeActionHandler.getASTRoot(unit, monitor);
            InnovationContext context = CodeActionHandler.getContext(unit, astRoot, params.getRange());
            ITypeBinding typeBinding = ASTNodes.getTypeBinding(astRoot, type);
            ASTNode classNode = astRoot.findDeclaringNode(typeBinding);
            ArrayList<ASTNode> coveredNodes = QuickAssistProcessor.getFullyCoveredNodes(context, classNode);

            for (ASTNode node : coveredNodes) {
                if (node instanceof MarkerAnnotation) {
                    MarkerAnnotation annotation = (MarkerAnnotation) node;
                    String name = annotation.resolveAnnotationBinding().getName();
                    if (lombokAnnotationSet.contains(name)) {
                        result.add(name);
                    }
                }
            }
            return result;
        } catch (Exception e) {
            JavaLanguageServerPlugin.logException("Get select lombok annoations", e);
            return new ArrayList<String>();
        }
    }

    public static AnnotationResponse getSelectAnnotationResponse(CodeActionParams params, IProgressMonitor monitor) {
        List<String> annotations = getSelectAnnotations(params, monitor);
        AnnotationResponse response = new AnnotationResponse(annotations.toArray(new String[0]));
        return response;
    }

    public static class AnnotationResponse {
        public String[] annotations;

        AnnotationResponse(String[] annotations) {
            this.annotations = annotations;
        }
    }

}
