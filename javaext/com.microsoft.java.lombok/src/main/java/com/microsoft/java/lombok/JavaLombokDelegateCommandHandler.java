package com.microsoft.java.lombok;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.ls.core.internal.IDelegateCommandHandler;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.jdt.ls.core.internal.JSONUtility;

import com.microsoft.java.lombok.JavaCodeActionHandler.LombokRequestParams;

public class JavaLombokDelegateCommandHandler implements IDelegateCommandHandler {
    public static final String JAVA_CODEACTION_LOMBOK_ANNOTATIONS = "java.codeAction.lombok.getAnnotations";
    public static final String JAVA_CODEACTION_LOMBOK = "java.codeAction.lombok";
    IProgressMonitor monitor;

    @Override
    public Object executeCommand(String commandId, List<Object> arguments, IProgressMonitor progress) throws Exception {
        switch (commandId) {
            case JAVA_CODEACTION_LOMBOK_ANNOTATIONS:
                CodeActionParams getAnnotationsParams = JSONUtility.toModel(arguments.get(0), CodeActionParams.class);
                return AnnotationHandler.findLombokAnnotation(getAnnotationsParams, monitor);
            case JAVA_CODEACTION_LOMBOK:
                LombokRequestParams lombokRequestParams = JSONUtility.toModel(arguments.get(0),
                        LombokRequestParams.class);
                return JavaCodeActionHandler.lombokAction(lombokRequestParams, monitor);
            default:
                break;
        }
        throw new UnsupportedOperationException(
                String.format("Java lombok plugin doesn't support the command '%s'.", commandId));
    }
}
