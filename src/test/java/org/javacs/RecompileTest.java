package org.javacs;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import org.junit.Test;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class RecompileTest {
    @Test
    public void compileTwice() {
        URI file = FindResource.uri("/org/javacs/example/CompileTwice.java");
        JavacHolder compiler = newCompiler();
        List<String> visits = new ArrayList<>();
        GetClass getClass = new GetClass(compiler.context, visits);
        CompilationResult compile = compiler.compile(Collections.singletonMap(file, Optional.empty()));

        compile.trees.forEach(tree -> tree.accept(getClass));

        assertThat(compile.errors.getDiagnostics(), empty());
        assertThat(visits, hasItems("CompileTwice", "NestedStaticClass", "NestedClass"));

        // Compile again
        compile = compiler.compile(Collections.singletonMap(file, Optional.empty()));

        compile.trees.forEach(tree -> tree.accept(getClass));

        assertThat(compile.errors.getDiagnostics(), empty());
        assertThat(visits, hasItems("CompileTwice", "NestedStaticClass", "NestedClass",
                                    "CompileTwice", "NestedStaticClass", "NestedClass"));
    }

    @Test
    public void fixParseError() {
        URI bad = FindResource.uri("/org/javacs/example/FixParseErrorBefore.java");
        URI good = FindResource.uri("/org/javacs/example/FixParseErrorAfter.java");
        JavacHolder compiler = newCompiler();
        DiagnosticCollector<JavaFileObject> badErrors = compiler.compile(Collections.singletonMap(bad, Optional.empty())).errors;

        assertThat(badErrors.getDiagnostics(), not(empty()));

        // Parse again
        CompilationResult goodCompile = compiler.compile(Collections.singletonMap(good, Optional.empty()));
        DiagnosticCollector<JavaFileObject> goodErrors = goodCompile.errors;
        List<String> parsedClassNames = new ArrayList<>();
        GetClass getClass = new GetClass(compiler.context, parsedClassNames);

        goodCompile.trees.forEach(tree -> tree.accept(getClass));

        assertThat(goodErrors.getDiagnostics(), empty());
        assertThat(parsedClassNames, contains("FixParseErrorAfter"));
    }

    @Test
    public void fixTypeError() {
        URI bad = FindResource.uri("/org/javacs/example/FixTypeErrorBefore.java");
        URI good = FindResource.uri("/org/javacs/example/FixTypeErrorAfter.java");
        JavacHolder compiler = newCompiler();
        DiagnosticCollector<JavaFileObject> badErrors = compiler.compile(Collections.singletonMap(bad, Optional.empty())).errors;

        assertThat(badErrors.getDiagnostics(), not(empty()));

        // Parse again
        CompilationResult goodCompile = compiler.compile(Collections.singletonMap(good, Optional.empty()));
        DiagnosticCollector<JavaFileObject> goodErrors = goodCompile.errors;
        List<String> parsedClassNames = new ArrayList<>();
        GetClass getClass = new GetClass(compiler.context, parsedClassNames);

        goodCompile.trees.forEach(tree -> tree.accept(getClass));

        assertThat(goodErrors.getDiagnostics(), empty());
        assertThat(parsedClassNames, contains("FixTypeErrorAfter"));
    }

    private static JavacHolder newCompiler() {
        return new JavacHolder(
                Collections.emptySet(),
                Collections.singleton(Paths.get("src/test/resources")),
                Paths.get("out"),
                false
        );
    }

    @Test
    public void keepTypeError() throws IOException {
        URI file = FindResource.uri("/org/javacs/example/UndefinedSymbol.java");
        JavacHolder compiler = newCompiler();

        // Compile once
        DiagnosticCollector<JavaFileObject> errors = compiler.compile(Collections.singletonMap(file, Optional.empty())).errors;
        assertThat(errors.getDiagnostics(), not(empty()));

        // Compile twice
        errors = compiler.compile(Collections.singletonMap(file, Optional.empty())).errors;

        assertThat(errors.getDiagnostics(), not(empty()));
    }

    private static class GetClass extends BaseScanner {
        private final List<String> visits;

        public GetClass(Context context, List<String> visits) {
            super(context);

            this.visits = visits;
        }

        @Override
        public void visitClassDef(JCTree.JCClassDecl tree) {
            super.visitClassDef(tree);

            visits.add(tree.getSimpleName().toString());
        }
    }
}
