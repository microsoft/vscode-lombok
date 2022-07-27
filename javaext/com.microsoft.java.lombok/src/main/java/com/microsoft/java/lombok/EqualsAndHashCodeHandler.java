package com.microsoft.java.lombok;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.manipulation.CoreASTProvider;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;
import org.eclipse.jdt.ls.core.internal.handlers.HashCodeEqualsHandler;
import org.eclipse.jdt.ls.core.internal.handlers.HashCodeEqualsHandler.CheckHashCodeEqualsResponse;
import org.eclipse.jdt.ls.core.internal.preferences.Preferences;
import org.eclipse.jdt.ls.core.internal.text.correction.SourceAssistProcessor;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.text.edits.TextEdit;

public class EqualsAndHashCodeHandler {
    private static final String lombokCanEqualMethod = "canEqual";
    private static final String lombokEqualsMethod = "equals";
    private static final String lombokHashCodeMethod = "hashCode";

    public static TextEdit generateHashCodeEquals(CodeActionParams params, IProgressMonitor monitor) {
        CheckHashCodeEqualsResponse response = HashCodeEqualsHandler.checkHashCodeEqualsStatus(params);
        IType type = SourceAssistProcessor.getSelectionType(params, monitor);
		Preferences preferences = JavaLanguageServerPlugin.getPreferencesManager().getPreferences();
		boolean useJava7Objects = preferences.isHashCodeEqualsTemplateUseJava7Objects();
		boolean useInstanceof = preferences.isHashCodeEqualsTemplateUseInstanceof();
		boolean useBlocks = preferences.isCodeGenerationTemplateUseBlocks();
		boolean generateComments = preferences.isCodeGenerationTemplateGenerateComments();
		return HashCodeEqualsHandler.generateHashCodeEqualsTextEdit(type, response.fields, true, useJava7Objects, useInstanceof, useBlocks, generateComments, params.getRange(), monitor);
    }
    
    public static void removeMethods(IType type, ListRewrite rewriter, IProgressMonitor monitor) {
        try{
            CompilationUnit astRoot = CoreASTProvider.getInstance().getAST(type.getCompilationUnit(), CoreASTProvider.WAIT_YES, monitor);
            if (astRoot == null) {
		    	return;
            }
            ITypeBinding typeBinding = ASTNodes.getTypeBinding(astRoot, type);
		    if (typeBinding == null) {
		    	return;
		    }
            
            Set<String> dataMethods = new HashSet<String>(Arrays.asList(lombokCanEqualMethod, lombokEqualsMethod, lombokHashCodeMethod));
            IMethodBinding[] declaredMethods = typeBinding.getDeclaredMethods();
            for(IMethodBinding item : declaredMethods){
                if(dataMethods.contains(item.getName())){
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
