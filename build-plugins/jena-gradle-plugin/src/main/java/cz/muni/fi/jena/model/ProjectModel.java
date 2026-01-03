package cz.muni.fi.jena.model;

import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;

public abstract class ProjectModel {

    @Input
    public abstract Property<String> getGroupId();

    @Input
    public abstract Property<String> getArtifactId();
}
