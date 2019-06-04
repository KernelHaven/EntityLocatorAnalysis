/*
 * Copyright 2019 University of Hildesheim, Software Systems Engineering
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.ssehub.kernel_haven.entity_locator.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.ssehub.kernel_haven.util.Logger;
import net.ssehub.kernel_haven.util.Util;

/**
 * Represents a local git repository directory.
 * 
 * @author Adam
 */
public class GitRepository {

    private static final boolean DEBUG_LOGGING = false;
    
    private static final Logger LOGGER = Logger.get();
    
    private File workingDirectory;
    
    /**
     * Creates a {@link GitRepository} for the given folder.
     * 
     * @param workingDirectory The working directory. If it doesn't exist yet, it will be created.
     * 
     * @throws GitException If workingDirectory is not a git repository and it cannot be initialized as one.
     */
    public GitRepository(File workingDirectory) throws GitException {
        this.workingDirectory = workingDirectory;
        if (!workingDirectory.isDirectory()) {
            workingDirectory.mkdir();
        }
        
        if (!workingDirectory.isDirectory()) {
            throw new GitException(workingDirectory + " is not a directory");
        }
        
        if (!new File(workingDirectory, ".git").isDirectory()) {
            init();
        }
    }
    
    /**
     * Clones a given remote repository to a local destination.
     * 
     * @param remoteUrl The remote URL to clone.
     * @param destination The destination to clone to. This must not yet exist.
     * 
     * @return A {@link GitRepository} for the given cloned destination.
     * 
     * @throws GitException If cloning fails.
     */
    public static GitRepository clone(String remoteUrl, File destination) throws GitException {
        if (destination.exists()) {
            throw new GitException(destination + " already exists");
        }
        
        destination.mkdir();
        if (!destination.isDirectory()) {
            throw new GitException("Couldn't create destination directory: " + destination);
        }
        
        try {
            runGitCommand(destination, "git", "clone", remoteUrl, destination.getAbsolutePath());
            
            return new GitRepository(destination);
        } catch (GitException e) {
            // clean up failed clone destination
            try {
                Util.deleteFolder(destination);
            } catch (IOException e1) {
                // ignore
            }
            
            throw e;
        }
    }
    
    /**
     * Initializes this git repository. Calls <code>git init</code>.
     * 
     * @throws GitException If initializing this repository fails.
     */
    private void init() throws GitException {
        runGitCommand("git", "init");
    }
    
    /**
     * Adds a remote to this git repository.
     * 
     * @param name The name of the remote to add.
     * @param url the URL of the remote to add.
     * 
     * @throws GitException If adding the remote fails.
     */
    public void addRemote(String name, String url) throws GitException {
        runGitCommand("git", "remote", "add", name, url);
    }
    
    /**
     * Gets a set of all remote names that have been added to this git repository.
     * 
     * @return A set of all remote names.
     * 
     * @throws GitException 
     */
    public Set<String> getRemotes() throws GitException {
        String output = runGitCommand("git", "remote");
        
        Set<String> result = new HashSet<>();
        
        for (String line : output.split("\\n")) {
            result.add(line);
        }
        
        return result;
    }
    
    /**
     * Creates a remote name for the given URL.
     * 
     * @param url The URL to create a remote name for.
     * 
     * @return A remote name.
     */
    public static String createRemoteName(String url) {
        // drop leading protocol
        int colonIndex = url.indexOf(':');
        if (colonIndex != -1) {
            url = url.substring(colonIndex + 1);
            while (url.startsWith("/")) {
                url = url.substring(1);
            }
        }
        
        // drop trailing .git
        if (url.endsWith(".git")) {
            url = url.substring(0, url.length() - ".git".length());
        }
        
        // replace everything that isn't alphanumeric
        return url.replaceAll("[^A-Za-z0-9_\\-]", "_");
    }
    
    /**
     * Fetches the given remote.
     * 
     * @param remoteName The remote to fetch.
     * 
     * @throws GitException If fetching fails.
     */
    public void fetch(String remoteName) throws GitException {
        runGitCommand("git", "fetch", remoteName);
    }
    
    /**
     * Fetches the given remote to retrieve the given commit or branch. Some remote servers do not support this feature;
     * in this case, this method automatically falls back to {@link #fetch(String)} to fetch the complete remote.
     * 
     * @param remoteName The remote to fetch.
     * @param commitOrBranch The commit or branch that should be fetched. All history leading up to this is fetched.
     * 
     * @throws GitException If fetching fails.
     */
    public void fetch(String remoteName, String commitOrBranch) throws GitException {
        try {
            runGitCommand("git", "fetch", remoteName, commitOrBranch);
        } catch (GitException e) {
            if (e.getMessage().startsWith("error: Server does not allow request for unadvertised object")) {
                LOGGER.logWarning("Remote does not support fetching commits; falling back to fetching full remote");
                fetch(remoteName);
            } else {
                throw e;
            }
        }
    }
    
    /**
     * Checks out the given commit.
     * 
     * @param commitHash The commit to check out. May also be a branch or tag name.
     * 
     * @throws GitException If this command fails.
     */
    public void checkout(String commitHash) throws GitException {
        runGitCommand("git", "checkout", "--force", commitHash);
    }
    
    /**
     * Checks out the given branch on the given remote.
     * 
     * @param remoteName The name of the remote that has the branch.
     * @param branch The name of the branch in the remote to check out.
     * 
     * @throws GitException If this command fails.
     */
    public void checkout(String remoteName, String branch) throws GitException {
        runGitCommand("git", "checkout", "--force", remoteName + "/" + branch);
    }
    
    /**
     * Creates a list of all commit hashes in this repository. The result order is based on the author date,
     * sorted old to new.
     * 
     * @return The list of all commit hashes.
     * 
     * @throws GitException If this command fails.
     */
    public List<String> listAllCommits() throws GitException {
        String out = runGitCommand("git", "log", "--format=format:%H", "--author-date-order", "--reverse");
        return Arrays.asList(out.split("\n"));
    }
    
    /**
     * Returns the commit hash that is directly before <code>date</code> in the given <code>branch</code>.
     * 
     * @param remoteName The name of the remote that has the given branch.
     * @param branch The branch to get the commit hash for.
     * @param date The date of the commit. The commit closest to and before this date will be returned.
     * 
     * @return The commit hash.
     * 
     * @throws GitException If this command fails.
     */
    public String getCommitBefore(String remoteName, String branch, String date) throws GitException {
        String hash = runGitCommand("git", "rev-list", "-n", "1", "--before=" + date, remoteName + "/" + branch);
        
        return hash;
    }
    
    /**
     * Returns last commit hash in the given <code>branch</code>.
     * 
     * @param remoteName The name of the remote that has the given branch.
     * @param branch The branch to get the commit hash for.
     * 
     * @return The commit hash.
     * 
     * @throws GitException If this command fails.
     */
    public String getLastCommitOfBranch(String remoteName, String branch) throws GitException {
        String hash = runGitCommand("git", "rev-list", "-n", "1", remoteName + "/" + branch);
        
        return hash;
    }
    
    /**
     * Checks if the given remote branch is checked out in the local git repo.
     * 
     * @param remote The remote name.
     * @param branch The branch name of the remote
     * 
     * @return Whether the repository contains the given remote branch.
     * 
     * @throws GitException 
     */
    public boolean containsRemoteBranch(String remote, String branch) throws GitException {
        String output = runGitCommand("git", "branch", "-r");
        
        Set<String> branches = new HashSet<>();
        
        for (String line : output.split("\\n")) {
            branches.add(line.trim());
        }
        
        return branches.contains(remote + "/" + branch);
    }
    
    /**
     * Checks if the given commit is checked out in the local git repo.
     * 
     * @param commit The commit to check.
     * 
     * @return Whether the repository contains the given commit. Also <code>false</code> if something else goes wrong.
     */
    public boolean containsCommit(String commit) {
        boolean exists;
        
        try {
            runGitCommand("git", "cat-file", "-e", commit + "^{commit}");
            exists = true;
            
        } catch (GitException e) {
            exists = false;
        }
        
        return exists;
    }
    
    /**
     * The working directory of this git repository.
     * 
     * @return The working directory. This is an existing folder.
     */
    public File getWorkingDirectory() {
        return workingDirectory;
    }
    
    /**
     * Runs the given git command in this git repository.
     * 
     * @param command The command to run, with command line parameters.
     * 
     * @return The standard output stream content.
     * 
     * @throws GitException If the given command fails executing or returns non-success.
     */
    private String runGitCommand(String... command) throws GitException {
        return runGitCommand(workingDirectory, command);
    }
    
    /**
     * Runs the given git command.
     * 
     * @param workingDirectory The working directory to execute the command in.
     * @param command The command to run, with command line parameters.
     * 
     * @return The standard output stream content.
     * 
     * @throws GitException If the given command fails executing or returns non-success.
     */
    private static String runGitCommand(File workingDirectory, String... command) throws GitException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workingDirectory);
        
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        
        boolean success = false;
        
        if (DEBUG_LOGGING) {
            LOGGER.logDebug(Arrays.toString(command));
        }
        
        try {
            success = Util.executeProcess(builder, "Git", stdout, stderr, 0);
            
        } catch (IOException e) {
            throw new GitException(e);
            
        } finally {
            if (DEBUG_LOGGING) {
                logWithLimit("Stdout:", stdout.toString().trim(), 20);
                logWithLimit("Stderr:", stderr.toString().trim(), 1000);
            }
        }
        
        if (!success) {
            throw new GitException(stderr.toString().trim());
        }
        
        return stdout.toString().trim();
    }
    
    /**
     * Logs the given message to debug stream. If the message has too many lines, it is skipped.
     * 
     * @param header The "header" line for the message.
     * @param message The message to log.
     * @param limit The maximum number of lines.
     */
    private static void logWithLimit(String header, String message, int limit) {
        if (message.chars().filter((ch) -> ch == '\n').count() > limit) {
            LOGGER.logDebug(header, "<too long>");
        } else {
            LOGGER.logDebug(header, message);
            
        }
    }
    
}
