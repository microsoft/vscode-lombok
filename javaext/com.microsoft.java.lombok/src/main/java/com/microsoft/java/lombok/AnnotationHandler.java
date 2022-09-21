package com.microsoft.java.lombok;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.manipulation.CoreASTProvider;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;
import org.eclipse.jdt.ls.core.internal.text.correction.SourceAssistProcessor;
import org.eclipse.lsp4j.CodeActionParams;

public class AnnotationHandler {
    public static final String lombokDataAnnotation = "Data";
    public static final String lombokNoArgsConstructorAnnotation = "NoArgsConstructor";
    public static final String lombokAllArgsConstructorAnnotation = "AllArgsConstructor";
    public static final String lombokGetterAnnotation = "Getter";
    public static final String lombokSetterAnnotation = "Setter";
    public static final String lombokToStringAnnotation = "ToString";
    public static final String lombokEqualsAndHashCodeAnnotation = "EqualsAndHashCode";

    public static Set<String> lombokAnnotationSet = new HashSet<String>(Arrays.asList(
            lombokDataAnnotation, lombokNoArgsConstructorAnnotation, lombokAllArgsConstructorAnnotation,
            lombokGetterAnnotation, lombokSetterAnnotation, lombokToStringAnnotation,
            lombokEqualsAndHashCodeAnnotation));

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

    public static class AnnotationResponse {
        public String[] annotations;

        AnnotationResponse(String[] annotations) {
            this.annotations = annotations;
        }
    }

}
