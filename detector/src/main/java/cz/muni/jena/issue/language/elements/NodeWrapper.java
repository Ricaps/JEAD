package cz.muni.jena.issue.language.elements;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import cz.muni.jena.issue.Issue;
import cz.muni.jena.issue.IssueType;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public record NodeWrapper<T extends Node>(T node)
{
    public long calculateComplexity()
    {
        return Stream.of(
                        node.findAll(IfStmt.class).stream().flatMap(ifStmt -> Stream.concat(
                                Stream.of(ifStmt),
                                ifStmt.getElseStmt().stream()
                        )),
                        node.findAll(ForStmt.class).stream(),
                        node.findAll(ForEachStmt.class).stream(),
                        node.findAll(WhileStmt.class).stream(),
                        node.findAll(DoStmt.class).stream(),
                        node.findAll(SwitchStmt.class).stream(),
                        node.findAll(ConditionalExpr.class).stream(),
                        node.findAll(CatchClause.class).stream(),
                        node.findAll(TryStmt.class).stream(),
                        node.findAll(BinaryExpr.class).stream()
                                .filter(
                                        binaryExpr -> binaryExpr.getOperator() == BinaryExpr.Operator.AND
                                                || binaryExpr.getOperator() == BinaryExpr.Operator.OR
                                )
                )
                .flatMap(i -> i)
                .count() + 1L;
    }

    public Stream<Issue> findMethodCallsWithAnyOfTheseExceptionInSignature(
            Set<String> exceptions,
            ClassOrInterfaceDeclaration classOrInterfaceDeclaration,
            IssueType issueType
    )
    {
        return Stream.of(node)
                .flatMap(block -> block.findAll(MethodCallExpr.class).stream())
                .filter(
                        methodCallExpr -> Stream.of(methodCallExpr)
                                .flatMap(ResolvableNode::resolve)
                                .map(CallableDec::getSpecifiedException)
                                .flatMap(List::stream)
                                .filter(ResolvedType::isReferenceType)
                                .map(ResolvedType::asReferenceType)
                                .map(ResolvedReferenceType::getTypeDeclaration)
                                .flatMap(Optional::stream)
                                .filter(ResolvedTypeDeclaration::isClass)
                                .map(ResolvedTypeDeclaration::asClass)
                                .map(ResolvedClassDec::new)
                                .anyMatch(
                                        resolvedClassDec -> resolvedClassDec
                                                .isAnyOfTheseClassesInHierarchy(exceptions)
                                )
                )
                .map(unmockableStatement -> Issue.fromNodeWithRange(
                        unmockableStatement,
                        issueType,
                        classOrInterfaceDeclaration
                ));
    }

    public Stream<StringLiteralExpr> findStringsMatchingRegexInNode(Pattern pattern)
    {
        List<StringLiteralExpr> stringLiteralExprStream = Stream.of(
                        node.findAll(StringLiteralExpr.class).stream(),
                        node.findAll(NameExpr.class)
                                .stream()
                                .flatMap(ResolvableNode::resolve)
                                .filter(ResolvedValueDeclaration::isField)
                                .map(ResolvedValueDeclaration::asField)
                                .map(field -> field.toAst(FieldDeclaration.class))
                                .flatMap(Optional::stream)
                                .flatMap(
                                        fieldDeclaration -> fieldDeclaration
                                                .findAll(StringLiteralExpr.class)
                                                .stream()
                                ),
                        node.findAll(NameExpr.class).stream()
                                .flatMap(ResolvableNode::resolve)
                                .filter(ResolvedValueDeclaration::isVariable)
                                .map(field -> field.toAst(VariableDeclarationExpr.class))
                                .flatMap(Optional::stream)
                                .flatMap(
                                        variableDeclaration -> variableDeclaration
                                                .findAll(StringLiteralExpr.class)
                                                .stream()
                                )
                )
                .flatMap(i -> i).toList();
        return stringLiteralExprStream.stream()
                .filter(
                        stringLiteralExpr -> pattern
                                .matcher(stringLiteralExpr.getValue().toLowerCase(Locale.ROOT))
                                .find()
                );
    }
}
