import java.nio.file.Path

final LOMBOK_ARTIFACT_ID = "lombok-1.18.42.jar"

File dependenciesDir = new File( basedir, "target/dependency" )

assert dependenciesDir.isDirectory()

String lombokDependencyPath = Path.of(dependenciesDir.getPath(), LOMBOK_ARTIFACT_ID)

File copiedDependency = new File(lombokDependencyPath)
assert copiedDependency.exists()