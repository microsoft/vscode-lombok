package com.microsoft.java.lombok;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.ls.core.internal.handlers.CodeGenerationUtils;
import org.eclipse.jdt.ls.core.internal.handlers.GenerateToStringHandler;
import org.eclipse.jdt.ls.core.internal.handlers.GenerateToStringHandler.CheckToStringResponse;
import org.eclipse.jdt.ls.core.internal.text.correction.SourceAssistProcessor;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.text.edits.TextEdit;

public class ToStringHandler {

    // <name, signature>
    public static final Map<String, String> toStringMethods = new HashMap<>();

    static {
        toStringMethods.put("toString", "()Ljava.lang.String;");
    }

    public static TextEdit generateMethods(CodeActionParams params, IProgressMonitor monitor) {
        CheckToStringResponse response = GenerateToStringHandler.checkToStringStatus(params);
        IType type = SourceAssistProcessor.getSelectionType(params);
        return GenerateToStringHandler.generateToString(type, response.fields,
                CodeGenerationUtils.findInsertElement(type, null), monitor);
    }

    public static void removeMethods(IType type, ListRewrite rewriter, IProgressMonitor monitor) {
        Utils.removeMethods(type, rewriter, toStringMethods, monitor);
    }
}
