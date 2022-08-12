package com.microsoft.java.lombok;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.manipulation.CoreASTProvider;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;
import org.eclipse.jdt.ls.core.internal.handlers.CodeGenerationUtils;
import org.eclipse.jdt.ls.core.internal.handlers.GenerateToStringHandler;
import org.eclipse.jdt.ls.core.internal.handlers.GenerateToStringHandler.CheckToStringResponse;
import org.eclipse.jdt.ls.core.internal.text.correction.SourceAssistProcessor;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.text.edits.TextEdit;

public class ToStringHandler {
    private static final String lombokToStringMethod = "toString";

    public static TextEdit generateToString(CodeActionParams params, IProgressMonitor monitor) {
        CheckToStringResponse response = GenerateToStringHandler.checkToStringStatus(params);
        IType type = SourceAssistProcessor.getSelectionType(params);
        IJavaElement insertPosition = CodeGenerationUtils.findInsertElement(type, null);
        return GenerateToStringHandler.generateToString(type, response.fields, insertPosition, monitor);
    }

    public static void removeMethods(IType type, ListRewrite rewriter, IProgressMonitor monitor) {
        try {
            CompilationUnit astRoot = CoreASTProvider.getInstance().getAST(type.getCompilationUnit(),
                    CoreASTProvider.WAIT_YES, monitor);
            if (astRoot == null) {
                return;
            }
            ITypeBinding typeBinding = ASTNodes.getTypeBinding(astRoot, type);
            if (typeBinding == null) {
                return;
            }

            IMethodBinding[] declaredMethods = typeBinding.getDeclaredMethods();
            for (IMethodBinding item : declaredMethods) {
                if (item.getName().equals(lombokToStringMethod)) {
                    item.getName();
                    ASTNode node = astRoot.findDeclaringNode(item);
                    rewriter.replace(node, null, null);
                }
            }
        } catch (Exception e) {
            JavaLanguageServerPlugin.logException("Remove Lombok methods", e);
        }
        return;
    }
}
