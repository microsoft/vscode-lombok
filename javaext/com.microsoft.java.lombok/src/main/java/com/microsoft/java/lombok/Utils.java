package com.microsoft.java.lombok;

import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.BindingKey;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.manipulation.CoreASTProvider;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;

public class Utils {
    public static void removeMethods(IType type, ListRewrite rewriter, Map<String, String> methodsToCheck,
            IProgressMonitor monitor) {
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
                if (methodsToCheck.keySet().contains(item.getName())) {
                    // check signature
                    if (methodsToCheck.get(item.getName()).equals(new BindingKey(item.getKey()).toSignature())) {
                        rewriter.remove(astRoot.findDeclaringNode(item), null);
                    }
                }
            }
        } catch (Exception e) {
            JavaLanguageServerPlugin.logException("Remove Lombok methods", e);
        }
        return;
    }
}
