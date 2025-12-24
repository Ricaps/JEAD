package cz.muni.jena.issue.language.elements;

import java.util.Optional;

public record ResolvedMethodDeclaration(ResolvedMethodDeclaration methodDeclaration) {

//    public Optional<String> methodSignature() {
//        try {
//            return methodDeclaration.getSignature().asString();
//        }
//    }
}
