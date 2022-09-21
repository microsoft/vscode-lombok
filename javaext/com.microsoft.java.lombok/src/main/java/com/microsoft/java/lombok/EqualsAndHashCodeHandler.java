package com.microsoft.java.lombok;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;
import org.eclipse.jdt.ls.core.internal.handlers.HashCodeEqualsHandler;
import org.eclipse.jdt.ls.core.internal.handlers.HashCodeEqualsHandler.CheckHashCodeEqualsResponse;
import org.eclipse.jdt.ls.core.internal.preferences.Preferences;
import org.eclipse.jdt.ls.core.internal.text.correction.SourceAssistProcessor;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.text.edits.TextEdit;

public class EqualsAndHashCodeHandler {

    // <name, signature>
    public static final Map<String, String> equalsAndHashCodeMethods = new HashMap<>();

    static {
        equalsAndHashCodeMethods.put("canEqual", "(Ljava.lang.Object;)Z");
        equalsAndHashCodeMethods.put("equals", "(Ljava.lang.Object;)Z");
        equalsAndHashCodeMethods.put("hashCode", "()I");
    }

    public static TextEdit generateMethods(CodeActionParams params, IProgressMonitor monitor) {
        CheckHashCodeEqualsResponse response = HashCodeEqualsHandler.checkHashCodeEqualsStatus(params);
        IType type = SourceAssistProcessor.getSelectionType(params, monitor);
        Preferences preferences = JavaLanguageServerPlugin.getPreferencesManager().getPreferences();
        return HashCodeEqualsHandler.generateHashCodeEqualsTextEdit(type, response.fields, true,
                preferences.isHashCodeEqualsTemplateUseJava7Objects(),
                preferences.isHashCodeEqualsTemplateUseInstanceof(), preferences.isCodeGenerationTemplateUseBlocks(),
                preferences.isCodeGenerationTemplateGenerateComments(), params.getRange(), monitor);
    }

    public static void removeMethods(IType type, ListRewrite rewriter, IProgressMonitor monitor) {
        Utils.removeMethods(type, rewriter, equalsAndHashCodeMethods, monitor);
    }
}
