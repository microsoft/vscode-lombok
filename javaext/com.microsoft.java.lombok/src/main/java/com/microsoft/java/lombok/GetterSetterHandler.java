package com.microsoft.java.lombok;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.manipulation.CoreASTProvider;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.corext.codemanipulation.GetterSetterUtil;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility2Core;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;
import org.eclipse.jdt.ls.core.internal.codemanipulation.GenerateGetterSetterOperation;
import org.eclipse.jdt.ls.core.internal.codemanipulation.GenerateGetterSetterOperation.AccessorField;
import org.eclipse.jdt.ls.core.internal.codemanipulation.GenerateGetterSetterOperation.AccessorKind;
import org.eclipse.jdt.ls.core.internal.handlers.CodeGenerationUtils;
import org.eclipse.jdt.ls.core.internal.text.correction.SourceAssistProcessor;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.text.edits.TextEdit;

public class GetterSetterHandler {
    private final static int generateVisibility = Modifier.PUBLIC;

    public static TextEdit generateMethods(CodeActionParams params, AccessorKind accessorKind, IProgressMonitor monitor) {
        try {
            IType type = SourceAssistProcessor.getSelectionType(params);
            AccessorField[] accessors = getAccessors(type, accessorKind);

            if (accessors == null || accessors.length == 0) {
                return null;
            }

            final ICompilationUnit unit = type.getCompilationUnit();
            CompilationUnit astRoot = CoreASTProvider.getInstance().getAST(unit, CoreASTProvider.WAIT_YES, monitor);
            final ASTRewrite astRewrite = ASTRewrite.create(astRoot.getAST());
            ListRewrite listRewriter = null;
            if (type.isAnonymous()) {
                final ClassInstanceCreation creation = ASTNodes
                        .getParent(NodeFinder.perform(astRoot, type.getNameRange()), ClassInstanceCreation.class);
                if (creation != null) {
                    final AnonymousClassDeclaration declaration = creation.getAnonymousClassDeclaration();
                    if (declaration != null) {
                        listRewriter = astRewrite.getListRewrite(declaration,
                                AnonymousClassDeclaration.BODY_DECLARATIONS_PROPERTY);
                    }
                }
            } else {
                final AbstractTypeDeclaration declaration = ASTNodes
                        .getParent(NodeFinder.perform(astRoot, type.getNameRange()), AbstractTypeDeclaration.class);
                if (declaration != null) {
                    listRewriter = astRewrite.getListRewrite(declaration, declaration.getBodyDeclarationsProperty());
                }
            }

            if (listRewriter == null) {
                return null;
            }

            ASTNode insertion = StubUtility2Core.getNodeToInsertBefore(listRewriter,
                    CodeGenerationUtils.findInsertElement(type, null));
            for (AccessorField accessor : accessors) {
                generateGetterSetterMethods(type, listRewriter, accessor, insertion);
            }

            return astRewrite.rewriteAST();
        } catch (OperationCanceledException | CoreException e) {
            return null;
        }
    }

    private static void generateGetterSetterMethods(IType type, ListRewrite listRewriter, AccessorField accessor,
            ASTNode insertion) throws OperationCanceledException, CoreException {
        IField field = type.getField(accessor.fieldName);
        if (field == null) {
            return;
        }

        if (accessor.generateGetter) {
            insertMethod(field, listRewriter, AccessorKind.GETTER, insertion);
        }

        if (accessor.generateSetter) {
            insertMethod(field, listRewriter, AccessorKind.SETTER, insertion);
        }
    }

    // See:
    // org.eclipse.jdt.ls.core.internal.codemanipulation.GenerateGetterSetterOperation.insertMethod
    private static void insertMethod(IField field, ListRewrite rewrite, AccessorKind kind, ASTNode insertion)
            throws CoreException {
        IType type = field.getDeclaringType();
        String delimiter = StubUtility.getLineDelimiterUsed(type);
        int flags = generateVisibility | (field.getFlags() & Flags.AccStatic);
        String stub;
        if (kind == AccessorKind.GETTER) {
            String name = GetterSetterUtil.getGetterName(field, null);
            stub = GetterSetterUtil.getGetterStub(field, name, false, flags);
        } else {
            String name = GetterSetterUtil.getSetterName(field, null);
            stub = GetterSetterUtil.getSetterStub(field, name, false, flags);
        }

        Map<String, String> options = type.getCompilationUnit() != null ? type.getCompilationUnit().getOptions(true)
                : type.getJavaProject().getOptions(true);
        String formattedStub = CodeFormatterUtil.format(CodeFormatter.K_CLASS_BODY_DECLARATIONS, stub, 0, delimiter,
                options);
        MethodDeclaration declaration = (MethodDeclaration) rewrite.getASTRewrite()
                .createStringPlaceholder(formattedStub, ASTNode.METHOD_DECLARATION);
        if (insertion != null) {
            rewrite.insertBefore(declaration, insertion, null);
        } else {
            rewrite.insertLast(declaration, null);
        }
    }

    // See:
    // org.eclipse.jdt.ls.core.internal.codemanipulation.GenerateGetterSetterOperation.getUnimplementedAccessors
    public static AccessorField[] getImplementedAccessors(IType type, AccessorKind kind) throws JavaModelException {
        if (!GenerateGetterSetterOperation.supportsGetterSetter(type)) {
            return new AccessorField[0];
        }
        List<AccessorField> implemented = new ArrayList<>();
        IField[] fields = type.isRecord() ? type.getRecordComponents() : type.getFields();
        for (IField field : fields) {
            int flags = field.getFlags();
            if (!Flags.isEnum(flags)) {
                boolean isStatic = Flags.isStatic(flags);
                boolean generateGetter = (GetterSetterUtil.getGetter(field) != null);
                boolean generateSetter = (GetterSetterUtil.getSetter(field) != null);
                switch (kind) {
                    case BOTH:
                        if (generateGetter || generateSetter) {
                            implemented.add(new AccessorField(field.getElementName(), isStatic, generateGetter,
                                    generateSetter, Signature.getSignatureSimpleName(field.getTypeSignature())));
                        }
                        break;
                    case GETTER:
                        if (generateGetter) {
                            implemented.add(new AccessorField(field.getElementName(), isStatic, generateGetter, false,
                                    Signature.getSignatureSimpleName(field.getTypeSignature())));
                        }
                        break;
                    case SETTER:
                        if (generateSetter) {
                            implemented.add(new AccessorField(field.getElementName(), isStatic, false, generateSetter,
                                    Signature.getSignatureSimpleName(field.getTypeSignature())));
                        }
                        break;
                    default:
                        break;
                }
            }
        }
        return implemented.toArray(new AccessorField[0]);
    }

    public static AccessorField[] getAccessors(IType type, AccessorKind kind) {
        try {
            if (type == null || type.isAnnotation() || type.isInterface() || type.getCompilationUnit() == null) {
                return new AccessorField[0];
            }
            List<AccessorField> accessors = new ArrayList<>();
            IField[] fields = type.isRecord() ? type.getRecordComponents() : type.getFields();
            for (IField field : fields) {
                int flags = field.getFlags();
                if (!Flags.isEnum(flags)) {
                    boolean isStatic = Flags.isStatic(flags);
                    switch (kind) {
                        case BOTH:
                            accessors.add(new AccessorField(field.getElementName(), isStatic, true, true,
                                    Signature.getSignatureSimpleName(field.getTypeSignature())));
                            break;
                        case GETTER:
                            accessors.add(new AccessorField(field.getElementName(), isStatic, true, false,
                                    Signature.getSignatureSimpleName(field.getTypeSignature())));
                            break;
                        case SETTER:
                            accessors.add(new AccessorField(field.getElementName(), isStatic, false, true,
                                    Signature.getSignatureSimpleName(field.getTypeSignature())));
                            break;
                        default:
                            break;
                    }
                }
            }
            return accessors.toArray(new AccessorField[0]);
        } catch (JavaModelException e) {
            JavaLanguageServerPlugin.logException("Failed to resolve the accessors.", e);
            return new AccessorField[0];
        }
    }

    public static void removeMethods(IType type, ListRewrite rewriter, AccessorKind kind, IProgressMonitor monitor) {
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
            Set<String> methods = new HashSet<>();
            // Add accessors
            for (AccessorField accessor : GetterSetterHandler.getImplementedAccessors(type, AccessorKind.BOTH)) {
                IField field = type.getField(accessor.fieldName);
                if (kind == AccessorKind.GETTER || kind == AccessorKind.BOTH) {
                    methods.add(GetterSetterUtil.getGetterName(field, null));
                }
                if (kind == AccessorKind.SETTER || kind == AccessorKind.BOTH) {
                    methods.add(GetterSetterUtil.getSetterName(field, null));
                }
            }
            IMethodBinding[] declaredMethods = typeBinding.getDeclaredMethods();
            for (IMethodBinding item : declaredMethods) {
                if (item.isDefaultConstructor()) {
                    continue;
                }
                if (methods.contains(item.getName())) {
                    rewriter.remove(astRoot.findDeclaringNode(item), null);
                }
            }
        } catch (Exception e) {
            JavaLanguageServerPlugin.logException("Remove Lombok @Data methods", e);
        }
    }
}
