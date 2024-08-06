package de.kontext_e.jqassistant.plugin.git.scanner;

import java.io.IOException;
import java.util.List;

import de.kontext_e.jqassistant.plugin.git.scanner.model.GitCommit;
import de.kontext_e.jqassistant.plugin.git.scanner.model.GitTag;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.junit.Ignore;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A Scanner based on Eclipse JGit.
 *
 * @author Gerd Aschemann - gerd@aschemann.net - @GerdAschemann
 *
 * @since 1.1.0
 */
// TODO This is not a real Unit test! Make gradle run it in some kind of integration test phase!!!
public class JGitRepositoryTest {
    @Ignore("No unit test, depends on state of git repo")
    @Test
    public void testFindCommits () throws IOException {
        JGitRepository jGitRepository = new JGitRepository("../.git", null);
        List<GitCommit> commits = jGitRepository.findCommits ();

        assertThat(commits.size()).isGreaterThan(0);
    }

    @Ignore("No unit test, depends on state of git repo")
    @Test
    public void testFindTags () throws IOException {
        JGitRepository jGitRepository = new JGitRepository("../.git", null);
        List<GitTag> tags = jGitRepository.findTags ();

        assertThat(tags.size()).isGreaterThan(0);
    }

    @Test
    public void testRange () throws IOException {
        Git git = mock(Git.class);
        Repository repository = mock(Repository.class);
        LogCommand logCommand = mock(LogCommand.class);
        ObjectId a1 = mock(ObjectId.class);
        ObjectId a2 = mock(ObjectId.class);

        when(git.getRepository()).thenReturn(repository);
        when(git.log()).thenReturn(logCommand);
        when(repository.resolve("HEAD^^")).thenReturn(a1);
        when(repository.resolve("4a877e")).thenReturn(a2);

        JGitRepository.getLogWithOrWithOutRange(git, "HEAD^^..4a877e");
        verify(logCommand).addRange(a1, a2);
    }


    @Test
    public void testRangeNoUntil () throws IOException {
        Git git = mock(Git.class);
        Repository repository = mock(Repository.class);
        LogCommand logCommand = mock(LogCommand.class);
        ObjectId a1 = mock(ObjectId.class);
        ObjectId a2 = mock(ObjectId.class);

        when(git.getRepository()).thenReturn(repository);
        when(git.log()).thenReturn(logCommand);
        when(repository.resolve("HEAD^^")).thenReturn(a1);
        when(repository.resolve("HEAD")).thenReturn(a2);

        JGitRepository.getLogWithOrWithOutRange(git, "HEAD^^..");
        verify(logCommand).addRange(a1, a2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRangeThreeDots () throws IOException {
        Git git = mock(Git.class);
        Repository repository = mock(Repository.class);
        LogCommand logCommand = mock(LogCommand.class);

        when(git.getRepository()).thenReturn(repository);
        when(git.log()).thenReturn(logCommand);

        JGitRepository.getLogWithOrWithOutRange(git, "HEAD^^...master");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRangeSingleDot () throws IOException {
        Git git = mock(Git.class);
        Repository repository = mock(Repository.class);
        LogCommand logCommand = mock(LogCommand.class);

        when(git.getRepository()).thenReturn(repository);
        when(git.log()).thenReturn(logCommand);

        JGitRepository.getLogWithOrWithOutRange(git, "HEAD^^.master");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRangeSinceDoesNotExist () throws IOException {
        Git git = mock(Git.class);
        Repository repository = mock(Repository.class);
        LogCommand logCommand = mock(LogCommand.class);

        when(git.getRepository()).thenReturn(repository);
        when(git.log()).thenReturn(logCommand);
        when(repository.resolve("NonExistingRev")).thenReturn(null);

        JGitRepository.getLogWithOrWithOutRange(git, "NonExistingRev..master");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRangeUntilDoesNotExist () throws IOException {
        Git git = mock(Git.class);
        Repository repository = mock(Repository.class);
        LogCommand logCommand = mock(LogCommand.class);
        ObjectId a1 = mock(ObjectId.class);

        when(git.getRepository()).thenReturn(repository);
        when(git.log()).thenReturn(logCommand);
        when(repository.resolve("HEAD")).thenReturn(a1);
        when(repository.resolve("NonExistingRev")).thenReturn(null);

        JGitRepository.getLogWithOrWithOutRange(git, "HEAD..NonExistingRev");
    }

}
