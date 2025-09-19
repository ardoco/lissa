package edu.kit.kastel.sdq.lissa.ratlr.preprocessor;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import spoon.Launcher;
import spoon.SpoonAPI;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtAnnotationFieldAccess;
import spoon.reflect.code.CtArrayRead;
import spoon.reflect.code.CtArrayWrite;
import spoon.reflect.code.CtAssert;
import spoon.reflect.code.CtAssignment;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtBreak;
import spoon.reflect.code.CtCase;
import spoon.reflect.code.CtCasePattern;
import spoon.reflect.code.CtCatch;
import spoon.reflect.code.CtCatchVariable;
import spoon.reflect.code.CtCodeSnippetExpression;
import spoon.reflect.code.CtCodeSnippetStatement;
import spoon.reflect.code.CtComment;
import spoon.reflect.code.CtConditional;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtContinue;
import spoon.reflect.code.CtDo;
import spoon.reflect.code.CtExecutableReferenceExpression;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtFieldWrite;
import spoon.reflect.code.CtFor;
import spoon.reflect.code.CtForEach;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtJavaDoc;
import spoon.reflect.code.CtJavaDocTag;
import spoon.reflect.code.CtLambda;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtNewArray;
import spoon.reflect.code.CtNewClass;
import spoon.reflect.code.CtOperatorAssignment;
import spoon.reflect.code.CtRecordPattern;
import spoon.reflect.code.CtResource;
import spoon.reflect.code.CtReturn;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtStatementList;
import spoon.reflect.code.CtSuperAccess;
import spoon.reflect.code.CtSwitch;
import spoon.reflect.code.CtSwitchExpression;
import spoon.reflect.code.CtSynchronized;
import spoon.reflect.code.CtTextBlock;
import spoon.reflect.code.CtThisAccess;
import spoon.reflect.code.CtThrow;
import spoon.reflect.code.CtTry;
import spoon.reflect.code.CtTryWithResource;
import spoon.reflect.code.CtTypeAccess;
import spoon.reflect.code.CtTypePattern;
import spoon.reflect.code.CtUnaryOperator;
import spoon.reflect.code.CtUnnamedPattern;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.code.CtVariableWrite;
import spoon.reflect.code.CtWhile;
import spoon.reflect.code.CtYieldStatement;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtAnnotationMethod;
import spoon.reflect.declaration.CtAnnotationType;
import spoon.reflect.declaration.CtAnonymousExecutable;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtEnum;
import spoon.reflect.declaration.CtEnumValue;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtImport;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtModule;
import spoon.reflect.declaration.CtModuleRequirement;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtPackageDeclaration;
import spoon.reflect.declaration.CtPackageExport;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtProvidedService;
import spoon.reflect.declaration.CtReceiverParameter;
import spoon.reflect.declaration.CtRecord;
import spoon.reflect.declaration.CtRecordComponent;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeParameter;
import spoon.reflect.declaration.CtUsedService;
import spoon.reflect.reference.CtArrayTypeReference;
import spoon.reflect.reference.CtCatchVariableReference;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtIntersectionTypeReference;
import spoon.reflect.reference.CtLocalVariableReference;
import spoon.reflect.reference.CtModuleReference;
import spoon.reflect.reference.CtPackageReference;
import spoon.reflect.reference.CtParameterReference;
import spoon.reflect.reference.CtTypeMemberWildcardImportReference;
import spoon.reflect.reference.CtTypeParameterReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.reference.CtUnboundVariableReference;
import spoon.reflect.reference.CtVariableReference;
import spoon.reflect.reference.CtWildcardReference;
import spoon.reflect.visitor.CtVisitor;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CodeGraphProviderTest {

    @Test
    public void testMethodChain() {
        SpoonAPI launcher = new Launcher();
        launcher.addInputResource("datasets/doc2code/TeaStore/model_2022/code");
        CtModel model = launcher.buildModel();
        for (CtType<?> type : model.getAllTypes()) {
            if (type.getSimpleName().equals("CartServlet")) {
                for (CtExecutableReference<?> declaredExecutable : type.getDeclaredExecutables()) {
                    System.out.println("---" + declaredExecutable);
                    declaredExecutable.accept(new MethodChainProcessor(declaredExecutable));
                }
            }
        }
    }
    
    private static final class MethodChained {
        private final CtExecutable<?> executable;
        private final Collection<ChainedElement> chainedInvocations;

        private MethodChained(CtExecutable<?> executable) {
            this.executable = executable;
            this.chainedInvocations = new LinkedList<>();
        }
        
        private void extractChainedInvocations() {
            CtBlock<?> body = this.executable.getBody();
            if (body == null) {
                return;
            }

            for (CtStatement statement : body.getStatements()) {
                
            }
        }
    }
    
    private static class ChainedElement implements Iterable<ChainedElement> {
        private final CtInvocation<?> invocation;

        private ChainedElement(CtInvocation<?> invocation) {
            this.invocation = invocation;
        }

        public CtInvocation<?> getInvocation() {
            return this.invocation;
        }

        @NotNull
        @Override
        public Iterator<ChainedElement> iterator() {
            return null;
        }
    }
    
    private static final class ChainedComposite extends ChainedElement {

        private final Collection<ChainedElement> elements = new LinkedList<>();

        private ChainedComposite(CtInvocation<?> invocation) {
            super(invocation);
        }

        public void add(ChainedElement element) {
            this.elements.add(element);
        }
    }
    
    private static final class ChainedLeaf extends ChainedElement {
        private ChainedLeaf(CtInvocation<?> invocation) {
            super(invocation);
        }
    }

    private static final class MethodChainProcessor implements CtVisitor {

        
        private final CtExecutable<?> rootExecutable;
        private final MethodChained chained;
        private final Collection<ChainedElement> chainedInvocations;
        private ChainedComposite current;
        private boolean insideRoot = true;

        private MethodChainProcessor(CtExecutableReference<?> executableReference) {
            CtExecutable<?> declaration = executableReference.getDeclaration();
            if (declaration == null) {
                this.rootExecutable = null;
                this.chained = null;
                this.chainedInvocations = List.of();
            } else {
                this.rootExecutable = declaration;
                this.chained = new MethodChained(declaration);
                this.chainedInvocations = new LinkedList<>();
            }
        }

        public MethodChained getChained() {
            return this.chained;
        }

        @Override
        public <T> void visitCtInvocation(CtInvocation<T> invocation) {
            System.out.println(invocation);
            this.current = new ChainedComposite(invocation);
            ChainedComposite temp = this.current;
            for (CtExpression<?> argument : invocation.getArguments()) {
                this.current = temp;
                argument.accept(this);
            }
            CtExecutableReference<T> reference = invocation.getExecutable();
            if (reference != null) {
                CtExecutable<T> executable = reference.getDeclaration();
                if (executable != null) {
                    if (this.insideRoot) {
                        this.chainedInvocations.add(temp);
                        this.insideRoot = false;
                        executable.accept(this);
                        this.insideRoot = true;
                    }
                }
                reference.accept(this);
            } else {
                this.current.add(new ChainedLeaf(invocation));
            }
        }

        @Override
        public <T> void visitCtExecutableReference(CtExecutableReference<T> reference) {
            System.out.println(reference);            
            CtExecutable<T> declaration = reference.getDeclaration();
            if (declaration != null) {
                declaration.accept(this);
            }
        }

        @Override
        public <T> void visitCtClass(CtClass<T> ctClass) {
            System.out.println("---CLASS---" + ctClass.getSimpleName());
        }

        @Override
        public void visitCtTypeParameter(CtTypeParameter typeParameter) {
            // ignored
        }

        @Override
        public <T> void visitCtConditional(CtConditional<T> conditional) {
            CtExpression<Boolean> condition = conditional.getCondition();
            if (condition != null) {
                condition.accept(this);
            }
            CtExpression<T> thenExpression = conditional.getThenExpression();
            if (thenExpression != null) {
                thenExpression.accept(this);
            }
            CtExpression<T> elseExpression = conditional.getElseExpression();
            if (elseExpression != null) {
                elseExpression.accept(this);
            }
        }

        @Override
        public <T> void visitCtMethod(CtMethod<T> m) {
            CtBlock<T> body = m.getBody();
            if (body != null) {
                body.accept(this);
            }
        }

        @Override
        public <T> void visitCtAnnotationMethod(CtAnnotationMethod<T> annotationMethod) {
            CtBlock<T> body = annotationMethod.getBody();
            if (body != null) {
                body.accept(this);
            }
        }

        @Override
        public <T> void visitCtNewArray(CtNewArray<T> newArray) {
            ChainedComposite temp = this.current;
            for (CtExpression<?> element : newArray.getElements()) {
                this.current = temp;
                element.accept(this);
            }
        }

        @Override
        public <T> void visitCtConstructorCall(CtConstructorCall<T> ctConstructorCall) {
            CtExecutableReference<T> executable = ctConstructorCall.getExecutable();
            if (executable != null) {
                executable.accept(this);
            }
        }

        @Override
        public <T> void visitCtNewClass(CtNewClass<T> newClass) {
            System.out.println("---ANONYMOUS CLASS---" + newClass);
        }

        @Override
        public <T> void visitCtLambda(CtLambda<T> lambda) {
            CtMethod<Object> overriddenMethod = lambda.getOverriddenMethod();
            if (overriddenMethod != null) {
                overriddenMethod.accept(this);
            }
        }

        @Override
        public <T, E extends CtExpression<?>> void visitCtExecutableReferenceExpression(CtExecutableReferenceExpression<T, E> expression) {
            CtExecutableReference<T> executable = expression.getExecutable();
            if (executable != null) {
                executable.accept(this);
            }
        }

        @Override
        public <T, A extends T> void visitCtOperatorAssignment(CtOperatorAssignment<T, A> assignment) {
            CtExpression<A> expression = assignment.getAssignment();
            if (expression != null) {
                expression.accept(this);
            }
        }

        @Override
        public void visitCtPackage(CtPackage ctPackage) {
            throw new IllegalStateException("should not visit package");
        }

        @Override
        public void visitCtPackageReference(CtPackageReference reference) {
            CtPackage declaration = reference.getDeclaration();
            if (declaration != null) {
                declaration.accept(this);
            }
        }

        @Override
        public <T> void visitCtParameter(CtParameter<T> parameter) {
            // ignored
        }

        @Override
        public <T> void visitCtParameterReference(CtParameterReference<T> reference) {
            CtParameter<T> declaration = reference.getDeclaration();
            if (declaration != null) {
                declaration.accept(this);
            }
        }

        @Override
        public <R> void visitCtReturn(CtReturn<R> returnStatement) {
            CtExpression<R> returnedExpression = returnStatement.getReturnedExpression();
            if (returnedExpression != null) {
                returnedExpression.accept(this);
            }
        }

        @Override
        public <R> void visitCtStatementList(CtStatementList statements) {
            ChainedComposite temp = this.current;
            for (CtStatement statement : statements) {
                this.current = temp;
                statement.accept(this);
            }
        }

        @Override
        public <S> void visitCtSwitch(CtSwitch<S> switchStatement) {
            CtExpression<S> selector = switchStatement.getSelector();
            if (selector != null) {
                selector.accept(this);
            }
            ChainedComposite temp = this.current;
            for (CtCase<? super S> aCase : switchStatement.getCases()) {
                this.current = temp;
                aCase.accept(this);
            }
        }

        @Override
        public <T, S> void visitCtSwitchExpression(CtSwitchExpression<T, S> switchExpression) {
            CtExpression<S> selector = switchExpression.getSelector();
            if (selector != null) {
                selector.accept(this);
            }
            ChainedComposite temp = this.current;
            for (CtCase<? super S> aCase : switchExpression.getCases()) {
                this.current = temp;
                aCase.accept(this);
            }
        }

        @Override
        public void visitCtSynchronized(CtSynchronized synchro) {
            CtExpression<?> expression = synchro.getExpression();
            if (expression != null) {
                expression.accept(this);
            }
        }

        @Override
        public void visitCtThrow(CtThrow throwStatement) {
            CtExpression<? extends Throwable> thrownExpression = throwStatement.getThrownExpression();
            if (thrownExpression != null) {
                thrownExpression.accept(this);
            }
        }

        @Override
        public void visitCtTry(CtTry tryBlock) {
            CtBlock<?> body = tryBlock.getBody();
            if (body != null) {
                body.accept(this);
            }
            ChainedComposite temp = this.current;
            for (CtCatch catcher : tryBlock.getCatchers()) {
                this.current = temp;
                catcher.accept(this);
            }
            CtBlock<?> finalizer = tryBlock.getFinalizer();
            if (finalizer != null) {
                finalizer.accept(this);
            }
        }

        @Override
        public void visitCtTryWithResource(CtTryWithResource tryWithResource) {
            ChainedComposite temp = this.current;
            for (CtResource<?> resource : tryWithResource.getResources()) {
                this.current = temp;
                resource.accept(this);
            }
            visitCtTry(tryWithResource);
        }

        @Override
        public void visitCtTypeParameterReference(CtTypeParameterReference ref) {
            // ignored
        }

        @Override
        public void visitCtWildcardReference(CtWildcardReference wildcardReference) {
            // ignored
        }

        @Override
        public <T> void visitCtIntersectionTypeReference(CtIntersectionTypeReference<T> reference) {
            // ignored
        }

        @Override
        public <T> void visitCtTypeReference(CtTypeReference<T> reference) {
            CtType<T> typeDeclaration = reference.getTypeDeclaration();
            if (typeDeclaration != null) {
                typeDeclaration.accept(this);
            }
        }

        @Override
        public <T> void visitCtTypeAccess(CtTypeAccess<T> typeAccess) {
            // TODO
        }

        @Override
        public <T> void visitCtUnaryOperator(CtUnaryOperator<T> operator) {
            // ignored
        }

        @Override
        public <T> void visitCtVariableRead(CtVariableRead<T> variableRead) {
            // ignored
        }

        @Override
        public <T> void visitCtVariableWrite(CtVariableWrite<T> variableWrite) {
            CtVariableReference<T> variable = variableWrite.getVariable();
            if (variable != null) {
                variable.accept(this);
            }
        }

        @Override
        public void visitCtWhile(CtWhile whileLoop) {
            CtExpression<Boolean> loopingExpression = whileLoop.getLoopingExpression();
            if (loopingExpression != null) {
                loopingExpression.accept(this);
            }
            CtStatement body = whileLoop.getBody();
            if (body != null) {
                body.accept(this);
            }
        }

        @Override
        public <T> void visitCtAnnotationFieldAccess(CtAnnotationFieldAccess<T> annotationFieldAccess) {
            CtFieldReference<T> variable = annotationFieldAccess.getVariable();
            if (variable != null) {
                variable.accept(this);
            }
        }

        @Override
        public <T> void visitCtFieldRead(CtFieldRead<T> fieldRead) {
            // ignored
        }

        @Override
        public <T> void visitCtFieldWrite(CtFieldWrite<T> fieldWrite) {
            CtFieldReference<T> variable = fieldWrite.getVariable();
            if (variable != null) {
                variable.accept(this);
            }
            CtExpression<?> target = fieldWrite.getTarget();
            if (target != null) {
                target.accept(this);
            }
        }

        @Override
        public <T> void visitCtSuperAccess(CtSuperAccess<T> f) {
            CtExpression<?> target = f.getTarget();
            if (target != null) {
                target.accept(this);
            }
        }

        @Override
        public void visitCtComment(CtComment comment) {
            // ignored
        }

        @Override
        public void visitCtJavaDoc(CtJavaDoc comment) {
            // ignored
        }

        @Override
        public void visitCtJavaDocTag(CtJavaDocTag docTag) {
            // ignored
        }

        @Override
        public void visitCtImport(CtImport ctImport) {
            // ignored
        }

        @Override
        public void visitCtModule(CtModule module) {
            throw new IllegalStateException("should not visit module");
        }

        @Override
        public void visitCtModuleReference(CtModuleReference moduleReference) {
            CtModule declaration = moduleReference.getDeclaration();
            if (declaration != null) {
                declaration.accept(this);
            }
        }

        @Override
        public void visitCtPackageExport(CtPackageExport moduleExport) {
            throw new IllegalStateException();
        }

        @Override
        public void visitCtModuleRequirement(CtModuleRequirement moduleRequirement) {
            throw new IllegalStateException();
        }

        @Override
        public void visitCtProvidedService(CtProvidedService moduleProvidedService) {
            throw new IllegalStateException();
        }

        @Override
        public void visitCtUsedService(CtUsedService usedService) {
            throw new IllegalStateException();
        }

        @Override
        public void visitCtCompilationUnit(CtCompilationUnit compilationUnit) {
            // ignored
        }

        @Override
        public void visitCtPackageDeclaration(CtPackageDeclaration packageDeclaration) {
            throw new IllegalStateException();
        }

        @Override
        public void visitCtTypeMemberWildcardImportReference(CtTypeMemberWildcardImportReference wildcardReference) {
            // ignored
        }

        @Override
        public void visitCtYieldStatement(CtYieldStatement statement) {
            CtExpression<?> expression = statement.getExpression();
            if (expression != null) {
                expression.accept(this);
            }
        }

        @Override
        public void visitCtTypePattern(CtTypePattern pattern) {
            // ignored
        }

        @Override
        public void visitCtRecord(CtRecord recordType) {
            System.out.println("---RECORD---" + recordType.getSimpleName());
        }

        @Override
        public void visitCtRecordComponent(CtRecordComponent recordComponent) {
            visitCtMethod(recordComponent.toMethod());
        }

        @Override
        public void visitCtCasePattern(CtCasePattern casePattern) {
            // ignored
        }

        @Override
        public void visitCtRecordPattern(CtRecordPattern recordPattern) {
            // ignored
        }

        @Override
        public void visitCtReceiverParameter(CtReceiverParameter receiverParameter) {
            // ignored
        }

        @Override
        public void visitCtUnnamedPattern(CtUnnamedPattern unnamedPattern) {
            // ignored
        }

        @Override
        public <T> void visitCtConstructor(CtConstructor<T> c) {
            CtBlock<T> body = c.getBody();
            if (body != null) {
                body.accept(this);
            }
        }

        @Override
        public void visitCtContinue(CtContinue continueStatement) {
            // ignored
        }

        @Override
        public void visitCtDo(CtDo doLoop) {
            CtStatement body = doLoop.getBody();
            if (body != null) {
                body.accept(this);
            }
            CtExpression<Boolean> loopingExpression = doLoop.getLoopingExpression();
            if (loopingExpression != null) {
                loopingExpression.accept(this);
            }
        }

        @Override
        public <T extends Enum<?>> void visitCtEnum(CtEnum<T> ctEnum) {
            System.out.println("---ENUM---" + ctEnum.getSimpleName());
        }

        @Override
        public <A extends Annotation> void visitCtAnnotation(CtAnnotation<A> annotation) {
            CtTypeReference<A> annotationType = annotation.getAnnotationType();
            if (annotationType != null) {
                annotationType.accept(this);
            }
            Map<String, CtExpression> values = annotation.getValues();
            if (values != null) {
                ChainedComposite temp = this.current;
                for (Map.Entry<String, CtExpression> stringCtExpressionEntry : values.entrySet()) {
                    this.current = temp;
                    stringCtExpressionEntry.getValue().accept(this);
                }
            }
        }

        @Override
        public <T> void visitCtCodeSnippetExpression(CtCodeSnippetExpression<T> expression) {
            // ignored
        }

        @Override
        public void visitCtCodeSnippetStatement(CtCodeSnippetStatement statement) {
            // ignored
        }

        @Override
        public <A extends Annotation> void visitCtAnnotationType(CtAnnotationType<A> annotationType) {
            ChainedComposite temp = this.current;
            for (CtAnnotationMethod<?> annotationMethod : annotationType.getAnnotationMethods()) {
                this.current = temp;
                annotationMethod.accept(this);
            }
        }

        @Override
        public void visitCtAnonymousExecutable(CtAnonymousExecutable anonymousExec) {
            CtBlock<Void> body = anonymousExec.getBody();
            if (body != null) {
                body.accept(this);
            }
        }

        @Override
        public <T> void visitCtArrayRead(CtArrayRead<T> arrayRead) {
            CtExpression<Integer> indexExpression = arrayRead.getIndexExpression();
            if (indexExpression != null) {
                indexExpression.accept(this);
            }
        }

        @Override
        public <T> void visitCtArrayWrite(CtArrayWrite<T> arrayWrite) {
            CtExpression<Integer> indexExpression = arrayWrite.getIndexExpression();
            if (indexExpression != null) {
                indexExpression.accept(this);
            }
            CtExpression<?> target = arrayWrite.getTarget();
            if (target != null) {
                target.accept(this);
            }
        }

        @Override
        public <T> void visitCtArrayTypeReference(CtArrayTypeReference<T> reference) {
            // ignored
        }

        @Override
        public <T> void visitCtAssert(CtAssert<T> asserted) {
            CtExpression<Boolean> assertExpression = asserted.getAssertExpression();
            if (assertExpression != null) {
                assertExpression.accept(this);
            }
            CtExpression<T> expression = asserted.getExpression();
            if (expression != null) {
                expression.accept(this);
            }
        }

        @Override
        public <T, A extends T> void visitCtAssignment(CtAssignment<T, A> assignement) {
            CtExpression<T> assigned = assignement.getAssigned();
            if (assigned != null) {
                assigned.accept(this);
            }
            CtExpression<A> expression = assignement.getAssignment();
            if (expression != null) {
                expression.accept(this);
            }
        }

        @Override
        public <T> void visitCtBinaryOperator(CtBinaryOperator<T> operator) {
            CtExpression<?> leftHandOperand = operator.getLeftHandOperand();
            if (leftHandOperand != null) {
                leftHandOperand.accept(this);
            }
            CtExpression<?> rightHandOperand = operator.getRightHandOperand();
            if (rightHandOperand != null) {
                rightHandOperand.accept(this);
            }
        }

        @Override
        public <R> void visitCtBlock(CtBlock<R> block) {
            System.out.println("---BLOCK---");
            System.out.println(block);
            ChainedComposite temp = this.current;
            for (CtStatement statement : block.getStatements()) {
                this.current = temp;
                System.out.println("---STATEMENT " + statement.getClass() + "---");
                System.out.println(statement);
                statement.accept(this);
            }
        }

        @Override
        public void visitCtBreak(CtBreak breakStatement) {
            // ignored
        }

        @Override
        public <S> void visitCtCase(CtCase<S> caseStatement) {
            ChainedComposite temp = this.current;
            for (CtExpression<S> caseExpression : caseStatement.getCaseExpressions()) {
                this.current = temp;
                caseExpression.accept(this);
            }
        }

        @Override
        public void visitCtCatch(CtCatch catchBlock) {
            CtBlock<?> body = catchBlock.getBody();
            if (body != null) {
                body.accept(this);
            }
        }

        @Override
        public <T> void visitCtLiteral(CtLiteral<T> literal) {
            // ignored
        }

        @Override
        public void visitCtTextBlock(CtTextBlock ctTextBlock) {
            // ignored
        }

        @Override
        public <T> void visitCtLocalVariable(CtLocalVariable<T> localVariable) {
            CtExpression<T> assignment = localVariable.getAssignment();
            if (assignment != null) {
                assignment.accept(this);
            }
        }

        @Override
        public <T> void visitCtLocalVariableReference(CtLocalVariableReference<T> reference) {
            // ignored
        }

        @Override
        public <T> void visitCtCatchVariable(CtCatchVariable<T> catchVariable) {
            // ignored
        }

        @Override
        public <T> void visitCtCatchVariableReference(CtCatchVariableReference<T> reference) {
            // ignored
        }

        @Override
        public <T> void visitCtField(CtField<T> f) {
            CtExpression<T> assignment = f.getAssignment();
            if (assignment != null) {
                assignment.accept(this);
            }
        }

        @Override
        public <T> void visitCtEnumValue(CtEnumValue<T> enumValue) {
            CtExpression<T> assignment = enumValue.getAssignment();
            if (assignment != null) {
                assignment.accept(this);
            }
            CtExpression<T> defaultExpression = enumValue.getDefaultExpression();
            if (defaultExpression != null) {
                defaultExpression.accept(this);
            }
        }

        @Override
        public <T> void visitCtThisAccess(CtThisAccess<T> thisAccess) {
            CtExpression<?> target = thisAccess.getTarget();
            if (target != null) {
                target.accept(this);
            }
        }

        @Override
        public <T> void visitCtFieldReference(CtFieldReference<T> reference) {
            // ignored
        }

        @Override
        public <T> void visitCtUnboundVariableReference(CtUnboundVariableReference<T> reference) {
            // ignored
        }

        @Override
        public void visitCtFor(CtFor forLoop) {
            CtExpression<Boolean> expression = forLoop.getExpression();
            if (expression != null) {
                expression.accept(this);
            }
            ChainedComposite temp = this.current;
            for (CtStatement ctStatement : forLoop.getForInit()) {
                this.current = temp;
                ctStatement.accept(this);
            }
            for (CtStatement ctStatement : forLoop.getForUpdate()) {
                this.current = temp;
                ctStatement.accept(this);
            }
            CtStatement body = forLoop.getBody();
            if (body != null) {
                body.accept(this);
            }
        }

        @Override
        public void visitCtForEach(CtForEach foreach) {
            CtExpression<?> expression = foreach.getExpression();
            if (expression != null) {
                expression.accept(this);
            }
            CtStatement body = foreach.getBody();
            if (body != null) {
                body.accept(this);
            }
        }

        @Override
        public void visitCtIf(CtIf ifElement) {
            CtExpression<Boolean> condition = ifElement.getCondition();
            if (condition != null) {
                condition.accept(this);
            }
            CtStatement thenStatement = ifElement.getThenStatement();
            if (thenStatement != null) {
                thenStatement.accept(this);
            }
            CtStatement elseStatement = ifElement.getElseStatement();
            if (elseStatement != null) {
                elseStatement.accept(this);
            }
        }

        @Override
        public <T> void visitCtInterface(CtInterface<T> intrface) {
            System.out.println("---INTERFACE---" + intrface.getSimpleName());
        }
    }
}
