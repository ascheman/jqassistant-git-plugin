package de.kontext_e.jqassistant.plugin.git.store.descriptor.change;

import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Relation;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.GitFileDescriptor;

@Label("Rename")
public interface GitRenameChangeDescriptor extends GitChangeDescriptor {

    @Relation("RENAMES")
    GitFileDescriptor getRenames();
    void setRenames(GitFileDescriptor gitFileDescriptor);

}
