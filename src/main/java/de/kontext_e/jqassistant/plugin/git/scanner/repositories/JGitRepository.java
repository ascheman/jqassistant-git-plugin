package de.kontext_e.jqassistant.plugin.git.scanner.repositories;

import de.kontext_e.jqassistant.plugin.git.scanner.model.GitBranch;
import de.kontext_e.jqassistant.plugin.git.scanner.model.GitChange;
import de.kontext_e.jqassistant.plugin.git.scanner.model.GitCommit;
import de.kontext_e.jqassistant.plugin.git.scanner.model.GitTag;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * A Scanner based on Eclipse JGit.
  * Check out JGit documentation
 * <ul>
 *     <li><a href="https://eclipse.org/jgit/">JGit project</a></li>
 *     <li><a href="https://github.com/centic9/jgit-cookbook">JGit cookbook</a></li>
 * </ul>
 *
 * @author Gerd Aschemann - gerd@aschemann.net - @GerdAschemann
 * @since 1.1.0
 */
public class JGitRepository {

    private static final Logger logger = LoggerFactory.getLogger(JGitRepository.class);

    private final String path;
    private final Repository repository;
    private final Map<String, GitCommit> commits = new HashMap<>();
    private final Git git;

    public JGitRepository(final String path) throws IOException {
        this.path = path;
        this.repository = getRepository();
        this.git = new Git(repository);
    }

    public JGitRepository(Git git) {
        this.git = git;
        this.repository = git.getRepository();
        this.path = repository.getDirectory().getAbsolutePath();
    }

    public LogCommand getLogWithOrWithOutRange(String range) throws IOException {
        LogCommand result = git.log();

        if (null == range) { return result.all(); }

        int firstDot = range.indexOf('.');
        if (firstDot <= 0) { throw new IllegalArgumentException ("Git range must start like '<rev specification>..'"); }

        int lastDot = range.lastIndexOf(".");
        if (lastDot - firstDot != 1) { throw new IllegalArgumentException ("Git range specials ('three dot notation' etc.) are not supported!"); }

        String sinceString = range.substring(0, firstDot);
        String untilString = lastDot + 1 < range.length() ? range.substring(lastDot + 1) : "HEAD";
        logger.debug ("Using range from '{}' to '{}'", sinceString, untilString);

        AnyObjectId since = git.getRepository().resolve(sinceString);
        if (null == since) { throw new IllegalArgumentException("Could not retrieve 'since' Range part '" + sinceString + "'"); }

        AnyObjectId until = git.getRepository().resolve(untilString);
        if (null == until) { throw new IllegalArgumentException("Could not retrieve 'until' Range part '" + untilString + "'"); }

        result = result.addRange(since, until);
        return result;
    }

    public List<GitCommit> findCommits(String range) throws IOException {
        List<GitCommit> result = new LinkedList<>();

        ObjectId head = repository.resolve("HEAD");
        logger.debug("Found head: {}", head);

        RevWalk rw = new RevWalk(repository);

        if (range != null && range.endsWith(".")) { range += "HEAD"; }

        try (git) {
            LogCommand logCommand = getLogWithOrWithOutRange(range);
            Iterable<RevCommit> commits = logCommand.call();

            DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
            df.setRepository(repository);
            df.setDiffComparator(RawTextComparator.DEFAULT);
            df.setDetectRenames(true);

            for (RevCommit commit : commits) {
                logger.debug("Commit-Message: '{}'", commit.getShortMessage());
                final Date date = new Date(1000 * (long) commit.getCommitTime());
                final GitCommit gitCommit = retrieveCommit(ObjectId.toString(commit.getId()));
                gitCommit.setAuthor(makeStringOfIdent(commit.getAuthorIdent()));
                gitCommit.setCommitter(makeStringOfIdent(commit.getCommitterIdent()));
                gitCommit.setDate(date);
                gitCommit.setMessage(commit.getFullMessage());
                gitCommit.setShortMessage(commit.getShortMessage());
                gitCommit.setEncoding(commit.getEncodingName());
                addCommitParents(rw, df, commit, gitCommit);

                result.add(gitCommit);
            }
        } catch (GitAPIException e) {
            throw new IllegalStateException("Could not read logs from Git repository '" + path + "'", e);
        } finally {
            rw.close();
            repository.close();
        }

        logger.debug("Found #{} commits", result.size());
        return result;
    }

    private String makeStringOfIdent(PersonIdent authorIdent) {
        return authorIdent.getName() + " <" +
               authorIdent.getEmailAddress() + ">";
    }

    private GitCommit retrieveCommit (String sha) {
        if (!commits.containsKey(sha)) {
            commits.put(sha, new GitCommit (sha));
        }
        return commits.get(sha);
    }

    private void addCommitParents(final RevWalk rw, final DiffFormatter df, final RevCommit revCommit, final GitCommit gitCommit) throws IOException {
        for (int i = 0; i < revCommit.getParentCount(); i++) {
            ObjectId parentId = revCommit.getParent(i).getId();
            RevCommit parent = rw.parseCommit(parentId);

            List<DiffEntry> diffs = df.scan(parent.getTree(), revCommit.getTree());
            for (DiffEntry diff : diffs) {
                final GitChange gitChange = new GitChange(
                        diff.getChangeType().name(),
                        diff.getOldPath(),
                        diff.getNewPath()
                );
                logger.debug(gitChange.toString());
                gitCommit.getGitChanges().add(gitChange);
            }

            String parentSha = ObjectId.toString(parentId);
            final GitCommit parentCommit = retrieveCommit(parentSha);
            gitCommit.getParents().add(parentCommit);
        }
    }

    private Repository getRepository() throws IOException {
        logger.debug("Opening repository for git directory '{}'", path);
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repository = builder
                .setGitDir(new File(path))
                .readEnvironment() // scan environment GIT_* variables
                .build();
        logger.debug("Using Git repository in '{}'", repository.getDirectory());
        return repository;
    }

    public GitBranch findHead() throws IOException {
        ObjectId head = repository.resolve(Constants.HEAD);
        return new GitBranch (Constants.HEAD, ObjectId.toString(head));
    }

    public String getCurrentlyCheckedOutBranch() throws IOException {
        return repository.getFullBranch();
    }

    public List<GitBranch> findBranches() {
        List<GitBranch> result = new LinkedList<>();
        try (git) {
            List<Ref> jGitBranches = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call();
            for (Ref jBranchRef : jGitBranches) {
                GitBranch newBranch = new GitBranch (jBranchRef.getName(), ObjectId.toString(jBranchRef.getObjectId()));
                result.add (newBranch);
            }
        } catch (GitAPIException e) {
            throw new IllegalStateException("Could not read branches from Git repository '" + path + "'", e);
        }

        return result;
    }

    private RevCommit resolveFirstCommitForTag (Git git, Ref tagRef) throws IOException, GitAPIException {
        LogCommand log = git.log();
        Ref peeledRef = git.getRepository().getRefDatabase().peel(tagRef);
        if(peeledRef.getPeeledObjectId() != null) {
            log.add(peeledRef.getPeeledObjectId());
        } else {
            log.add(tagRef.getObjectId());
        }

        Iterable<RevCommit> logs = log.call();

        return logs.iterator().next();
    }

    public List<GitTag> findTags() throws IOException {
        List<GitTag> result = new LinkedList<>();

        try (git) {
            List<Ref> tags = git.tagList().call();
            for (Ref tagRef : tags) {
                String label = tagRef.getName();
                RevCommit firstCommit = resolveFirstCommitForTag(git, tagRef);
                String objectId = ObjectId.toString(firstCommit);
                logger.debug ("Found Tag '{}' (name = '{}', sha = '{}')", tagRef, label, objectId);
                GitTag newTag = new GitTag (label, objectId);
                result.add (newTag);
            }
        } catch (GitAPIException e) {
            throw new IllegalStateException("Could not read tags from Git repository '" + path + "'", e);
        }

        return result;
    }
}
