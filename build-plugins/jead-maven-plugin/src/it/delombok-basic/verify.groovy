import java.nio.file.Path

File delombokFile = new File(basedir, "src-delombok")

assert delombokFile.isDirectory()

Path testClassPath = Path.of(delombokFile.getPath(), "main", "java", "Test.java")
File testClassFile = testClassPath.toFile()
String content = testClassFile.text

assert content.contains("public Test(final String testField)")
assert content.contains("public String getTestField()")
assert !content.contains("@RequiredArgsConstructor")
assert !content.contains("@Data")