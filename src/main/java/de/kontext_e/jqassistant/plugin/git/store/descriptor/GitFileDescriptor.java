package de.kontext_e.jqassistant.plugin.git.store.descriptor;

import com.buschmais.xo.neo4j.api.annotation.Indexed;
import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Property;
import com.buschmais.xo.neo4j.api.annotation.Relation;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.relation.GitAddRelation;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.relation.GitDeleteRelation;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.relation.GitUpdateRelation;

@Label("File")
public interface GitFileDescriptor extends GitDescriptor {

    @Indexed
    @Property("relativePath")
    String getRelativePath();
    void setRelativePath(String relativePath);

    @Property("createdAt")
    String getCreatedAt();
    void setCreatedAt(String relativePath);

    @Property("deletedAt")
    String getDeletedAt();
    void setDeletedAt(String deletedAt);

    @Property("lastModificationAt")
    String getLastModificationAt();
    void setLastModificationAt(String lastModificationAt);

    @Property("createdAtEpoch")
    Long getCreatedAtEpoch();
    void setCreatedAtEpoch(Long relativePathEpoch);

    @Property("deletedAtEpoch")
    Long getDeletedAtEpoch();
    void setDeletedAtEpoch(Long deletedAtEpoch);

    @Property("lastModificationAtEpoch")
    Long getLastModificationAtEpoch();
    void setLastModificationAtEpoch(Long lastModificationAtEpoch);

    @Relation("HAS_NEW_NAME")
    GitFileDescriptor getHasNewName();
    void setHasNewName(GitFileDescriptor gitFileDescriptor);

    @Relation("COPY_OF")
    GitFileDescriptor getCopyOf();
    void setCopyOf(GitFileDescriptor oldFile);

    @Relation.Incoming
    GitAddRelation getAdds();
    void setAdds(GitAddRelation adds);

    @Relation.Incoming
    GitDeleteRelation getDeletes();
    void setDeletes(GitDeleteRelation deletes);

    @Relation.Incoming
    GitUpdateRelation getUpdates();
    void setUpdates(GitUpdateRelation updates);
}
