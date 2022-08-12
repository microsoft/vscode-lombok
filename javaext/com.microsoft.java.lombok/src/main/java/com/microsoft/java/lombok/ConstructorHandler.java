package com.microsoft.java.lombok;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.manipulation.CoreASTProvider;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;
import org.eclipse.jdt.ls.core.internal.preferences.Preferences;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.ls.core.internal.handlers.GenerateConstructorsHandler;
import org.eclipse.jdt.ls.core.internal.handlers.GenerateConstructorsHandler.CheckConstructorsResponse;
import org.eclipse.jdt.ls.core.internal.handlers.JdtDomModels.LspMethodBinding;
import org.eclipse.jdt.ls.core.internal.handlers.JdtDomModels.LspVariableBinding;
import org.eclipse.jdt.ls.core.internal.text.correction.SourceAssistProcessor;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.text.edits.TextEdit;

import com.microsoft.java.lombok.ConstructorHandler;

public class ConstructorHandler {
    public static void removeMethods(IType type, ListRewrite rewriter, ConstructorKind kind, IProgressMonitor monitor) {
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

            String className = typeBinding.getName();
            int fieldsNum = typeBinding.getDeclaredFields().length;
            IMethodBinding[] declaredMethods = typeBinding.getDeclaredMethods();
            for (IMethodBinding item : declaredMethods) {
                if (item.isDefaultConstructor()) {
                    continue;
                }
                ITypeBinding[] parameterTypes = item.getParameterTypes();
                int parametersNum = parameterTypes.length;
                if (item.getName().equals(className)) {
                    if (kind == ConstructorKind.NOARGCONSTRUCTOR && parametersNum == 0) {
                        ASTNode node = astRoot.findDeclaringNode(item);
                        rewriter.replace(node, null, null);
                        break;
                    }
                    if (kind == ConstructorKind.ALLARGSCONSTRUCTOR && parametersNum == fieldsNum) {
                        ASTNode node = astRoot.findDeclaringNode(item);
                        rewriter.replace(node, null, null);
                        break;
                    }
                }
            }

        } catch (Exception e) {
            JavaLanguageServerPlugin.logException("Remove Lombok method", e);
        }
        return;
    }

    public static TextEdit generateConstructor(CodeActionParams params, IProgressMonitor monitor,
            ConstructorKind kind) {
        IType type = SourceAssistProcessor.getSelectionType(params, monitor);
        CheckConstructorsResponse response = GenerateConstructorsHandler.checkConstructorStatus(type, params.getRange(),
                monitor);
        if (response.constructors.length == 0 || response.constructors == null) {
            return null;
        }
        LspMethodBinding[] constructors = { response.constructors[0] };
        LspVariableBinding[] fields = new LspVariableBinding[] {};
        switch (kind) {
            case NOARGCONSTRUCTOR:
                break;
            case ALLARGSCONSTRUCTOR:
                fields = response.fields;
                break;
        }
        Preferences preferences = JavaLanguageServerPlugin.getPreferencesManager().getPreferences();
        CodeGenerationSettings settings = new CodeGenerationSettings();
        settings.createComments = preferences.isCodeGenerationTemplateGenerateComments();
        return GenerateConstructorsHandler.generateConstructors(type, constructors, fields, settings, null, monitor);
    }

    public enum ConstructorKind {
        NOARGCONSTRUCTOR(0), ALLARGSCONSTRUCTOR(1);

        private final int value;

        private ConstructorKind(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private static final String[] annotationKind = { "NoArgsConstructor", "AllArgsConstructor" };
}
