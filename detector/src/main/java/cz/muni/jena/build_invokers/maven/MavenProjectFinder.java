package cz.muni.jena.build_invokers.maven;

import cz.muni.jena.build_invokers.ProjectFinder;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

@Component
public class MavenProjectFinder implements ProjectFinder {

    private static final Logger LOGGER = LoggerFactory.getLogger(MavenProjectFinder.class);

    @Override
    public Set<Path> process(List<Path> paths) {
        List<ModelPathPair> models = parseToModels(paths);
        Set<Path> result = new HashSet<>();

        for (ModelPathPair pair : models) {
            if (result.contains(pair.path)) {
                continue;
            }

            Model model = pair.model;
            Parent parent = model.getParent();

            if (parent == null) {
                result.add(pair.path);
            } else {
                Optional<ModelPathPair> parentModel = findParentModel(parent, models);
                parentModel.map(ModelPathPair::path).ifPresentOrElse(result::add, () -> result.add(pair.path));
            }

        }

        return result;
    }

    private Optional<ModelPathPair> findParentModel(Parent parent, List<ModelPathPair> allModels) {
        return allModels.stream().filter(modelPathPair -> {
                    Model model = modelPathPair.model;

                    return model.getArtifactId().equals(parent.getArtifactId());
                })
                .findFirst()
                .flatMap(foundParent -> {
                    // If parent was found, try recursively check for nested project structure
                    // It means, that current parent can also have parent belonging to current project
                    Model model = foundParent.model;

                    if (model.getParent() == null) {
                        return Optional.of(foundParent);
                    }

                    // If the parent project belonging to current not found = current pom is top-level parent
                    return findParentModel(model.getParent(), allModels)
                            .or(() -> Optional.of(foundParent));
                });
    }


    @Override
    public String getFileSuffix() {
        return "pom.xml";
    }

    private List<ModelPathPair> parseToModels(List<Path> paths) {
        return paths.stream()
                .filter(path -> path.endsWith(getFileSuffix()))
                .map(this::parseToMavenModel)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    private Optional<ModelPathPair> parseToMavenModel(Path path) {
        try (FileReader fileReader = new FileReader(path.toFile())) {
            Model model = new MavenXpp3Reader().read(fileReader);
            return Optional.of(new ModelPathPair(model, path));
        } catch (IOException | XmlPullParserException e) {
            LOGGER.error("Failed to parse maven file at path {}!", path, e);
        }
        return Optional.empty();
    }

    private record ModelPathPair(Model model, Path path) {
    }
}
