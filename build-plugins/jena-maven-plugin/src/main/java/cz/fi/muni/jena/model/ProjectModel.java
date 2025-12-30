package cz.fi.muni.jena.model;

/**
 * Model class to represent project model. <br>
 * Can be used as input configuration.
 */
public class ProjectModel {

    private String artifactId;
    private String groupId;

    public ProjectModel() {
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
}
