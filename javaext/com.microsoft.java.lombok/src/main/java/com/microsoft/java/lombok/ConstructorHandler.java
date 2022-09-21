package com.microsoft.java.lombok;

import java.util.Arrays;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.manipulation.CoreASTProvider;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;
import org.eclipse.jdt.ls.core.internal.preferences.Preferences;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.ls.core.internal.handlers.GenerateConstructorsHandler;
import org.eclipse.jdt.ls.core.internal.handlers.GenerateConstructorsHandler.CheckConstructorsResponse;
import org.eclipse.jdt.ls.core.internal.handlers.JdtDomModels.LspVariableBinding;
import org.eclipse.jdt.ls.core.internal.text.correction.SourceAssistProcessor;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.text.edits.TextEdit;

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
            
            long nonStaticFieldLength = Arrays.stream(typeBinding.getDeclaredFields()).filter(field -> !Modifier.isStatic(field.getModifiers())).count();

            for (IMethodBinding item : typeBinding.getDeclaredMethods()) {
                if (item.isDefaultConstructor()) {
                    continue;
                }
                int parametersNum = item.getParameterTypes().length;
                if (item.getName().equals(typeBinding.getName())) {
                    if (kind == ConstructorKind.NO_ARG && parametersNum == 0) {
                        ASTNode node = astRoot.findDeclaringNode(item);
                        rewriter.replace(node, null, null);
                        break;
                    } else if (kind == ConstructorKind.ALL_ARGS && parametersNum == nonStaticFieldLength) {
                        ASTNode node = astRoot.findDeclaringNode(item);
                        rewriter.replace(node, null, null);
                        break;
                    }
                }
            }

        } catch (Exception e) {
            JavaLanguageServerPlugin.logException("Remove Lombok method", e);
        }
    }

    public static TextEdit generateMethods(CodeActionParams params, IProgressMonitor monitor,
            ConstructorKind kind) {
        IType type = SourceAssistProcessor.getSelectionType(params, monitor);
        CheckConstructorsResponse response = GenerateConstructorsHandler.checkConstructorStatus(type, params.getRange(),
                monitor);
        if (response.constructors.length == 0) {
            return null;
        }
        LspVariableBinding[] fields = new LspVariableBinding[] {};
        if (kind == ConstructorKind.ALL_ARGS) {
            fields = response.fields;
        }
        Preferences preferences = JavaLanguageServerPlugin.getPreferencesManager().getPreferences();
        CodeGenerationSettings settings = new CodeGenerationSettings();
        settings.createComments = preferences.isCodeGenerationTemplateGenerateComments();
        return GenerateConstructorsHandler.generateConstructors(type, response.constructors, fields, settings, null, monitor);
    }

    public enum ConstructorKind {
        NO_ARG(0), ALL_ARGS(1);

        private final int value;

        private ConstructorKind(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
}
