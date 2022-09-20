package com.microsoft.java.lombok;

import java.util.Set;
import java.util.Arrays;
import java.util.HashSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.manipulation.CoreASTProvider;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;
import org.eclipse.jdt.internal.corext.codemanipulation.GetterSetterUtil;
import org.eclipse.jdt.ls.core.internal.codemanipulation.GenerateGetterSetterOperation.AccessorField;
import org.eclipse.jdt.ls.core.internal.codemanipulation.GenerateGetterSetterOperation.AccessorKind;
import org.eclipse.jdt.ls.core.internal.text.correction.SourceAssistProcessor;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;

import com.microsoft.java.lombok.ConstructorHandler.ConstructorKind;

public class DataHandler {
    private static final String lombokCanEqualMethod = "canEqual";
    private static final String lombokEqualsMethod = "equals";
    private static final String lombokHashCodeMethod = "hashCode";
    private static final String lombokToStringMethod = "toString";

    public static TextEdit generateDataTextEdit(CodeActionParams params, IProgressMonitor monitor) {
        IType type = SourceAssistProcessor.getSelectionType(params, monitor);
        if (type == null || type.getCompilationUnit() == null) {
            return null;
        }

        TextEdit textEdit = new MultiTextEdit();
        TextEdit allArgsConstructorTextEdit = ConstructorHandler.generateConstructor(params, monitor,
                ConstructorKind.ALL_ARGS);
        if (allArgsConstructorTextEdit != null) {
            textEdit.addChild(allArgsConstructorTextEdit);
        }
        TextEdit noArgConstructorTextEdit = ConstructorHandler.generateConstructor(params, monitor,
                ConstructorKind.NO_ARG);
        if (noArgConstructorTextEdit != null) {
            textEdit.addChild(noArgConstructorTextEdit);
        }
        TextEdit accessorsTextEdit = GetterSetterHandler.generateGetterSetter(params, monitor);
        if (accessorsTextEdit != null) {
            textEdit.addChild(accessorsTextEdit);
        }
        TextEdit toStringTextEdit = ToStringHandler.generateToString(params, monitor);
        if (toStringTextEdit != null) {
            textEdit.addChild(toStringTextEdit);
        }
        TextEdit hashCodeEqualsTextEdit = EqualsAndHashCodeHandler.generateHashCodeEquals(params, monitor);
        if (hashCodeEqualsTextEdit != null) {
            textEdit.addChild(hashCodeEqualsTextEdit);
        }

        return textEdit;
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
            Set<String> dataMethods = new HashSet<String>(Arrays.asList(lombokCanEqualMethod, lombokEqualsMethod,
                    lombokHashCodeMethod, lombokToStringMethod));
            // Add constructors
            dataMethods.add(typeBinding.getName());
            // Add accessors
            for (AccessorField accessor : GetterSetterHandler.getImplementedAccessors(type, AccessorKind.BOTH)) {
                IField field = type.getField(accessor.fieldName);
                dataMethods.add(GetterSetterUtil.getGetterName(field, null));
                dataMethods.add(GetterSetterUtil.getSetterName(field, null));
            }
            IMethodBinding[] declaredMethods = typeBinding.getDeclaredMethods();
            for (IMethodBinding item : declaredMethods) {
                if (item.isDefaultConstructor()) {
                    continue;
                }
                if (dataMethods.contains(item.getName())) {
                    rewriter.remove(astRoot.findDeclaringNode(item), null);
                }
            }
        } catch (Exception e) {
            JavaLanguageServerPlugin.logException("Remove Lombok @Data methods", e);
        }
        return;
    }
}
