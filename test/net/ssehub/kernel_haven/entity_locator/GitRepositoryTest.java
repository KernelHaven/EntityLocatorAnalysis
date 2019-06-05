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
package net.ssehub.kernel_haven.entity_locator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import net.ssehub.kernel_haven.entity_locator.util.GitException;
import net.ssehub.kernel_haven.entity_locator.util.GitRepository;
import net.ssehub.kernel_haven.util.Util;
import net.ssehub.kernel_haven.util.ZipArchive;

/**
 * Tests the {@link GitRepository}.
 *
 * @author Adam
 */
@SuppressWarnings("null")
public class GitRepositoryTest {

    private static final File TESTDATA = new File("testdata");
    
    private static final File TEST_REPO = new File(TESTDATA, "testRepo");
    
    /**
     * Extracts the test repository in testRepo.zip.
     * 
     * @throws IOException If extraction fails.
     */
    @BeforeClass
    public static void extractTestRepo() throws IOException {
        try (ZipArchive archive = new ZipArchive(new File(TESTDATA, "testRepo.zip"))) {
            for (File f : archive.listFiles()) {
                File target = new File(TESTDATA, f.getPath());
                target.getParentFile().mkdirs();
                archive.extract(f, new File(TESTDATA, f.getPath()));
            }
        }
    }
    
    /**
     * Deletes the test repository.
     * 
     * @throws IOException If deleting fails.
     */
    @AfterClass
    public static void cleanUpTestRepo() throws IOException {
        Util.deleteFolder(TEST_REPO);
    }
    
    
    /**
     * Tests the {@link GitRepository#createRemoteName(String)} method.
     */
    @Test
    public void testCreateRemoteName() {
        assertThat(GitRepository.createRemoteName("https://github.com/KernelHaven/KernelHaven.git"),
                is("github_com_KernelHaven_KernelHaven"));
        assertThat(GitRepository.createRemoteName("github.com/KernelHaven/KernelHaven"),
                is("github_com_KernelHaven_KernelHaven"));
        assertThat(GitRepository.createRemoteName("something//:github.com/KernelHaven/KernelHaven.notgit"),
                is("github_com_KernelHaven_KernelHaven_notgit"));
    }
    
    /**
     * Tests opening an existing git repository.
     * 
     * @throws GitException unwanted.
     */
    @Test
    public void testGetWorkingDir() throws GitException {
        GitRepository repo = new GitRepository(TEST_REPO);
        
        assertThat(repo.getWorkingDirectory(), is(TEST_REPO));
    }
    
    /**
     * Tests creating a new, empty git repository in a new folder.
     * 
     * @throws GitException unwanted.
     * @throws IOException unwanted.
     */
    @Test
    public void testIntializeNewGitRepo() throws GitException, IOException {
        File newRepo = new File(TESTDATA, "newRepo");
        assertThat(newRepo.exists(), is(false));
        
        try {
            GitRepository repo = new GitRepository(newRepo);
            
            assertThat(repo.getWorkingDirectory(), is(newRepo));
            assertThat(newRepo.isDirectory(), is(true));
            assertThat(newRepo.listFiles(), is(new File[] {new File(newRepo, ".git")}));
            
        } finally {
            if (newRepo.exists()) {
                Util.deleteFolder(newRepo);
            }
        }
    }
    
    /**
     * Tests that trying to create a repository that is already a file fails.
     * 
     * @throws GitException wanted.
     * @throws IOException unwanted.
     */
    @Test(expected = GitException.class)
    public void testRepoIsFile() throws GitException, IOException {
        File someFile = new File(TESTDATA, "someFile");
        someFile.createNewFile();
        assertThat(someFile.isFile(), is(true));
        
        try {
            new GitRepository(someFile);
            
        } finally {
            someFile.delete();
        }
    }
    
    /**
     * Tests cloning an existing repository.
     * 
     * @throws GitException unwanted.
     * @throws IOException unwanted.
     */
    @Test
    public void testClone() throws GitException, IOException {
        File clonedRepo = new File(TESTDATA, "clonedRepo");
        assertThat(clonedRepo.exists(), is(false));
        
        try {
            GitRepository repo = GitRepository.clone("file://" + TEST_REPO.getAbsolutePath(), clonedRepo);
            
            assertThat(repo.getWorkingDirectory(), is(clonedRepo));
            assertThat(clonedRepo.isDirectory(), is(true));
            assertThat(new File(clonedRepo, ".git").isDirectory(), is(true));
            assertThat(new File(clonedRepo, "m").isFile(), is(true));
            
        } finally {
            if (clonedRepo.exists()) {
                Util.deleteFolder(clonedRepo);
            }
        }
    }
    
    /**
     * Tests that cloning into an existing location throws an exception.
     * 
     * @throws GitException wanted.
     * @throws IOException unwanted.
     */
    @Test(expected = GitException.class)
    public void testCloneExistingTarget() throws GitException, IOException {
        File clonedRepo = new File(TESTDATA, "clonedRepo");
        clonedRepo.mkdir();
        assertThat(clonedRepo.exists(), is(true));
        
        try {
            GitRepository.clone("file://" + TEST_REPO.getAbsolutePath(), clonedRepo);
            
        } finally {
            if (clonedRepo.exists()) {
                Util.deleteFolder(clonedRepo);
            }
        }
    }
    
    /**
     * Tests that trying to clone a non-exiting repository fails.
     * 
     * @throws GitException unwanted.
     * @throws IOException unwanted.
     */
    @Test(expected = GitException.class)
    public void testCloneNotExistingSource() throws GitException, IOException {
        File clonedRepo = new File(TESTDATA, "clonedRepo");
        assertThat(clonedRepo.exists(), is(false));
        
        try {
            GitRepository.clone("file://doesnt_exist", clonedRepo);
            
        } catch (GitException e) {
            // expected
            assertThat(clonedRepo.exists(), is(false));
            
            throw e;
        }
    }
    
    /**
     * Tests the {@link GitRepository#listAllCommits()} method.
     * 
     * @throws GitException unwanted.
     */
    @Test
    public void listAllCommits() throws GitException {
        GitRepository repo = new GitRepository(TEST_REPO);
        
        assertThat(repo.listAllCommits(), is(Arrays.asList(
            "ac3ce2b8e1970cafedf445fc85e8f0e3b10fb678",
            "8761998b60bf12146be97ce4854ceddc7fd0bfc9",
            "da43e932a3bbed69d4a09426922a960652f591f6",
            "183dda81207043ba8d81e480c3a8da6a2502b895"
        )));
    }
    
    /**
     * Tests the {@link GitRepository#listAllCommits()} method when HEAD is currently on a specific commit.
     * 
     * @throws GitException unwanted.
     * @throws IOException unwanted.
     */
    @Test
    public void listAllCommitsOnCommit() throws GitException, IOException {
        // clone repo, so we don't modify the original
        File clonedRepo = new File(TESTDATA, "clonedRepo");
        assertThat(clonedRepo.exists(), is(false));
        
        try {
            GitRepository repo = GitRepository.clone("file://" + TEST_REPO.getAbsolutePath(), clonedRepo);
            assertThat(repo.getWorkingDirectory(), is(clonedRepo));
            assertThat(clonedRepo.isDirectory(), is(true));
            
            assertThat(repo.listAllCommits(), is(Arrays.asList(
                "ac3ce2b8e1970cafedf445fc85e8f0e3b10fb678",
                "8761998b60bf12146be97ce4854ceddc7fd0bfc9",
                "da43e932a3bbed69d4a09426922a960652f591f6",
                "183dda81207043ba8d81e480c3a8da6a2502b895"
            )));
            
            repo.checkout("8761998b60bf12146be97ce4854ceddc7fd0bfc9");
            
            assertThat(repo.listAllCommits(), is(Arrays.asList(
                "ac3ce2b8e1970cafedf445fc85e8f0e3b10fb678",
                "8761998b60bf12146be97ce4854ceddc7fd0bfc9"
            )));
            
        } finally {
            if (clonedRepo.exists()) {
                Util.deleteFolder(clonedRepo);
            }
        }
    }
    
    /**
     * Tests the {@link GitRepository#containsCommit(String)} method.
     * 
     * @throws GitException unwanted.
     */
    @Test
    public void testContainsCommit() throws GitException {
        GitRepository repo = new GitRepository(TEST_REPO);
        
        assertThat(repo.containsCommit("ac3ce2b8e1970cafedf445fc85e8f0e3b10fb678"), is(true));
        assertThat(repo.containsCommit("8761998b60bf12146be97ce4854ceddc7fd0bfc9"), is(true));
        assertThat(repo.containsCommit("da43e932a3bbed69d4a09426922a960652f591f6"), is(true));
        assertThat(repo.containsCommit("183dda81207043ba8d81e480c3a8da6a2502b895"), is(true));
        
        assertThat(repo.containsCommit("master"), is(true));
        assertThat(repo.containsCommit("HEAD"), is(true));
        
        assertThat(repo.containsCommit("398c7500a1f5f74e207bd2edca1b1721b3cc1f1e"), is(false));
        assertThat(repo.containsCommit("origin"), is(false));
        assertThat(repo.containsCommit("origin/master"), is(false));
    }
    
    /**
     * Tests the {@link GitRepository#checkout(String)} method.
     * 
     * @throws GitException unwanted.
     * @throws IOException unwanted.
     */
    @Test
    public void testCheckout() throws GitException, IOException {
        // clone the testRepo, so that we don't modify the original
        File clonedRepo = new File(TESTDATA, "clonedRepo");
        assertThat(clonedRepo.exists(), is(false));
        
        try {
            File m = new File(clonedRepo, "m");
            
            GitRepository repo = GitRepository.clone("file://" + TEST_REPO.getAbsolutePath(), clonedRepo);
            assertThat(repo.getWorkingDirectory(), is(clonedRepo));
            assertThat(clonedRepo.isDirectory(), is(true));
            assertThat(new File(clonedRepo, ".git").isDirectory(), is(true));
            assertThat(m.isFile(), is(true));
            
            try (BufferedReader in = new BufferedReader(new FileReader(m))) {
                assertThat(in.readLine(), is("Received: (sender4@test.org) by some.server.org"));
            }
            
            repo.checkout("8761998b60bf12146be97ce4854ceddc7fd0bfc9");
            
            try (BufferedReader in = new BufferedReader(new FileReader(m))) {
                assertThat(in.readLine(), is("Received: (sender2@test.org) by some.server.org"));
            }
            
        } finally {
            if (clonedRepo.exists()) {
                Util.deleteFolder(clonedRepo);
            }
        }
    }
    
    /**
     * Tests the {@link GitRepository#checkout(String, File)} method.
     * 
     * @throws GitException unwanted.
     * @throws IOException unwanted.
     */
    @Test
    public void testCheckoutFile() throws GitException, IOException {
        // clone the testRepo, so that we don't modify the original
        File clonedRepo = new File(TESTDATA, "clonedRepo");
        assertThat(clonedRepo.exists(), is(false));
        
        try {
            File m = new File(clonedRepo, "m");
            
            GitRepository repo = GitRepository.clone("file://" + TEST_REPO.getAbsolutePath(), clonedRepo);
            assertThat(repo.getWorkingDirectory(), is(clonedRepo));
            assertThat(clonedRepo.isDirectory(), is(true));
            assertThat(new File(clonedRepo, ".git").isDirectory(), is(true));
            assertThat(m.isFile(), is(true));
            
            try (BufferedReader in = new BufferedReader(new FileReader(m))) {
                assertThat(in.readLine(), is("Received: (sender4@test.org) by some.server.org"));
            }
            
            repo.checkout("8761998b60bf12146be97ce4854ceddc7fd0bfc9", new File("m"));
            
            try (BufferedReader in = new BufferedReader(new FileReader(m))) {
                assertThat(in.readLine(), is("Received: (sender2@test.org) by some.server.org"));
            }
            
        } finally {
            if (clonedRepo.exists()) {
                Util.deleteFolder(clonedRepo);
            }
        }
    }
    
    /**
     * Tests the {@link GitRepository#checkout(String, File)} method with a not existing file.
     * 
     * @throws GitException wanted.
     * @throws IOException unwanted.
     */
    @Test(expected = GitException.class)
    public void testCheckoutNonExistingFile() throws GitException, IOException {
        // clone the testRepo, so that we don't modify the original
        File clonedRepo = new File(TESTDATA, "clonedRepo");
        assertThat(clonedRepo.exists(), is(false));
        
        try {
            File m = new File(clonedRepo, "m");
            
            GitRepository repo = GitRepository.clone("file://" + TEST_REPO.getAbsolutePath(), clonedRepo);
            assertThat(repo.getWorkingDirectory(), is(clonedRepo));
            assertThat(clonedRepo.isDirectory(), is(true));
            assertThat(new File(clonedRepo, ".git").isDirectory(), is(true));
            assertThat(m.isFile(), is(true));
            
            repo.checkout("8761998b60bf12146be97ce4854ceddc7fd0bfc9", new File("doesnt_exist"));
            
        } finally {
            if (clonedRepo.exists()) {
                Util.deleteFolder(clonedRepo);
            }
        }
    }
    
    /**
     * Tests that the {@link GitRepository#getRemotes()} returns an empty set for a repository with no remotes.
     * 
     * @throws GitException unwanted.
     */
    @Test
    public void testGetRemotesEmpty() throws GitException {
        GitRepository repo = new GitRepository(TEST_REPO);
        
        assertThat(repo.getRemotes(), is(new HashSet<>()));
    }
    
    /**
     * Tests the {@link GitRepository#addRemote(String, String)} and {@link GitRepository#getRemotes()} methods.
     * 
     * @throws GitException unwanted.
     * @throws IOException unwanted.
     */
    @Test
    public void testRemoteHandling() throws GitException, IOException {
        File newRepo = new File(TESTDATA, "newRepo");
        assertThat(newRepo.exists(), is(false));
        
        try {
            GitRepository repo = new GitRepository(newRepo);
            assertThat(repo.getWorkingDirectory(), is(newRepo));
            
            assertThat(repo.getRemotes(), is(new HashSet<>()));
            assertThat(repo.containsRemoteBranch("remote1", "master"), is(false));
            
            repo.addRemote("remote1", "file://" + TEST_REPO.getAbsolutePath());
            
            assertThat(repo.getRemotes(), is(new HashSet<>(Arrays.asList("remote1"))));
            assertThat(repo.containsRemoteBranch("remote1", "master"), is(false)); // no fetch yet
            
        } finally {
            if (newRepo.exists()) {
                Util.deleteFolder(newRepo);
            }
        }
    }
    
    /**
     * Extension of {@link #testRemoteHandling()}; tests the {@link GitRepository#fetch(String)} and
     * {@link GitRepository#containsRemoteBranch(String, String)} methods.
     * 
     * @throws GitException unwanted.
     * @throws IOException unwanted.
     */
    @Test
    public void testRemoteFetching() throws GitException, IOException {
        File newRepo = new File(TESTDATA, "newRepo");
        assertThat(newRepo.exists(), is(false));
        
        try {
            GitRepository repo = new GitRepository(newRepo);
            assertThat(repo.getWorkingDirectory(), is(newRepo));
            
            assertThat(repo.getRemotes(), is(new HashSet<>()));
            assertThat(repo.containsRemoteBranch("remote1", "master"), is(false));
            
            repo.addRemote("remote1", "file://" + TEST_REPO.getAbsolutePath());
            
            assertThat(repo.getRemotes(), is(new HashSet<>(Arrays.asList("remote1"))));
            assertThat(repo.containsRemoteBranch("remote1", "master"), is(false)); // no fetch yet
            
            // actual test begins here
            
            repo.fetch("remote1");
            
            assertThat(repo.getRemotes(), is(new HashSet<>(Arrays.asList("remote1"))));
            assertThat(repo.containsRemoteBranch("remote1", "master"), is(true));
            
        } finally {
            if (newRepo.exists()) {
                Util.deleteFolder(newRepo);
            }
        }
    }
    
    /**
     * Extension of {@link #testRemoteHandling()}; tests the {@link GitRepository#fetch(String, String)} and
     * {@link GitRepository#containsRemoteBranch(String, String)} methods.
     * 
     * @throws GitException unwanted.
     * @throws IOException unwanted.
     */
    @Test
    public void testRemoteBranchFetching() throws GitException, IOException {
        File newRepo = new File(TESTDATA, "newRepo");
        assertThat(newRepo.exists(), is(false));
        
        try {
            GitRepository repo = new GitRepository(newRepo);
            assertThat(repo.getWorkingDirectory(), is(newRepo));
            
            assertThat(repo.getRemotes(), is(new HashSet<>()));
            assertThat(repo.containsRemoteBranch("remote1", "master"), is(false));
            
            repo.addRemote("remote1", "file://" + TEST_REPO.getAbsolutePath());
            
            assertThat(repo.getRemotes(), is(new HashSet<>(Arrays.asList("remote1"))));
            assertThat(repo.containsRemoteBranch("remote1", "master"), is(false)); // no fetch yet
            
            // actual test begins here
            
            repo.fetch("remote1", "master");
            
            assertThat(repo.getRemotes(), is(new HashSet<>(Arrays.asList("remote1"))));
            assertThat(repo.containsRemoteBranch("remote1", "master"), is(true));
            
        } finally {
            if (newRepo.exists()) {
                Util.deleteFolder(newRepo);
            }
        }
    }
    
    /**
     * Extension of {@link #testRemoteHandling()}; tests the {@link GitRepository#fetch(String, String)} and
     * {@link GitRepository#containsRemoteBranch(String, String)} methods.
     * <p>
     * This test tries to fetch a single commit; however, this will not work and the full remote will be fetched
     * instead.
     * 
     * @throws GitException unwanted.
     * @throws IOException unwanted.
     */
    @Test
    public void testRemoteCommitFetching() throws GitException, IOException {
        File newRepo = new File(TESTDATA, "newRepo");
        assertThat(newRepo.exists(), is(false));
        
        try {
            GitRepository repo = new GitRepository(newRepo);
            assertThat(repo.getWorkingDirectory(), is(newRepo));
            
            assertThat(repo.getRemotes(), is(new HashSet<>()));
            assertThat(repo.containsRemoteBranch("remote1", "master"), is(false));
            
            repo.addRemote("remote1", "file://" + TEST_REPO.getAbsolutePath());
            
            assertThat(repo.getRemotes(), is(new HashSet<>(Arrays.asList("remote1"))));
            assertThat(repo.containsRemoteBranch("remote1", "master"), is(false)); // no fetch yet
            
            // actual test begins here
            
            repo.fetch("remote1", "8761998b60bf12146be97ce4854ceddc7fd0bfc9");
            
            assertThat(repo.getRemotes(), is(new HashSet<>(Arrays.asList("remote1"))));
            assertThat(repo.containsRemoteBranch("remote1", "master"), is(true));
            
        } finally {
            if (newRepo.exists()) {
                Util.deleteFolder(newRepo);
            }
        }
    }
    
    /**
     * Tests that {@link GitRepository#fetch(String, String)} fails if the remote doesn't exist.
     * 
     * @throws GitException wanted.
     * @throws IOException unwanted.
     */
    @Test(expected = GitException.class)
    public void testRemoteFetchRemoteDoesntExist() throws GitException, IOException {
        File newRepo = new File(TESTDATA, "newRepo");
        assertThat(newRepo.exists(), is(false));
        
        try {
            GitRepository repo = new GitRepository(newRepo);
            assertThat(repo.getWorkingDirectory(), is(newRepo));
            
            assertThat(repo.getRemotes(), is(new HashSet<>()));
            assertThat(repo.containsRemoteBranch("remote1", "master"), is(false));
            
            repo.fetch("remote1", "master");
            
        } finally {
            if (newRepo.exists()) {
                Util.deleteFolder(newRepo);
            }
        }
    }
    
    /**
     * Extension of {@link #testRemoteFetching()}; tests the {@link GitRepository#checkout(String, String)} method.
     * 
     * @throws GitException unwanted.
     * @throws IOException unwanted.
     */
    @Test
    public void testRemoteCheckout() throws GitException, IOException {
        File newRepo = new File(TESTDATA, "newRepo");
        assertThat(newRepo.exists(), is(false));
        
        try {
            GitRepository repo = new GitRepository(newRepo);
            assertThat(repo.getWorkingDirectory(), is(newRepo));
            
            assertThat(repo.getRemotes(), is(new HashSet<>()));
            assertThat(repo.containsRemoteBranch("remote1", "master"), is(false));
            
            repo.addRemote("remote1", "file://" + TEST_REPO.getAbsolutePath());
            
            assertThat(repo.getRemotes(), is(new HashSet<>(Arrays.asList("remote1"))));
            assertThat(repo.containsRemoteBranch("remote1", "master"), is(false)); // no fetch yet
            
            repo.fetch("remote1");
            
            assertThat(repo.getRemotes(), is(new HashSet<>(Arrays.asList("remote1"))));
            assertThat(repo.containsRemoteBranch("remote1", "master"), is(true));
            
            // actual test begins here
            
            File m = new File(newRepo, "m");
            assertThat(m.exists(), is(false));
            
            repo.checkout("remote1", "master");
            
            assertThat(m.exists(), is(true));
            
        } finally {
            if (newRepo.exists()) {
                Util.deleteFolder(newRepo);
            }
        }
    }
    
    /**
     * Extension of {@link #testRemoteFetching()}; tests the
     * {@link GitRepository#getCommitBefore(String, String, String)} method.
     * 
     * @throws GitException unwanted.
     * @throws IOException unwanted.
     */
    @Test
    public void testGetCommitBefore() throws GitException, IOException {
        File newRepo = new File(TESTDATA, "newRepo");
        assertThat(newRepo.exists(), is(false));
        
        try {
            GitRepository repo = new GitRepository(newRepo);
            assertThat(repo.getWorkingDirectory(), is(newRepo));
            
            assertThat(repo.getRemotes(), is(new HashSet<>()));
            assertThat(repo.containsRemoteBranch("remote1", "master"), is(false));
            
            repo.addRemote("remote1", "file://" + TEST_REPO.getAbsolutePath());
            
            assertThat(repo.getRemotes(), is(new HashSet<>(Arrays.asList("remote1"))));
            assertThat(repo.containsRemoteBranch("remote1", "master"), is(false)); // no fetch yet
            
            repo.fetch("remote1");
            
            assertThat(repo.getRemotes(), is(new HashSet<>(Arrays.asList("remote1"))));
            assertThat(repo.containsRemoteBranch("remote1", "master"), is(true));
            
            // actual test begins here
            
            // there are 4 commits in the repo:
            // $ git log --pretty=format:"%H %cI"
            // 183dda81207043ba8d81e480c3a8da6a2502b895 2019-06-04T13:10:02+02:00
            // da43e932a3bbed69d4a09426922a960652f591f6 2019-06-04T13:09:38+02:00
            // 8761998b60bf12146be97ce4854ceddc7fd0bfc9 2019-06-04T13:08:11+02:00
            // ac3ce2b8e1970cafedf445fc85e8f0e3b10fb678 2019-06-04T12:18:42+02:00

            assertThat(repo.getCommitBefore("remote1", "master", "2019-06-04T12:15:00+02:00"), is(""));
            
            assertThat(repo.getCommitBefore("remote1", "master", "2019-06-04T12:20:00+02:00"),
                    is("ac3ce2b8e1970cafedf445fc85e8f0e3b10fb678"));
            
            assertThat(repo.getCommitBefore("remote1", "master", "2019-06-04T13:09:00+02:00"),
                    is("8761998b60bf12146be97ce4854ceddc7fd0bfc9"));
            
            assertThat(repo.getCommitBefore("remote1", "master", "2019-06-04T13:10:00+02:00"),
                    is("da43e932a3bbed69d4a09426922a960652f591f6"));
            
            assertThat(repo.getCommitBefore("remote1", "master", "2019-06-05T00:00:00+02:00"),
                    is("183dda81207043ba8d81e480c3a8da6a2502b895"));
            
            assertThat(repo.getCommitBefore("remote1", "master", "2019-06-04T13:10:01+02:00"),
                    is("da43e932a3bbed69d4a09426922a960652f591f6"));
            assertThat(repo.getCommitBefore("remote1", "master", "2019-06-04T13:10:02+02:00"),
                    is("183dda81207043ba8d81e480c3a8da6a2502b895"));
            assertThat(repo.getCommitBefore("remote1", "master", "2019-06-04T13:10:03+02:00"),
                    is("183dda81207043ba8d81e480c3a8da6a2502b895"));
            
        } finally {
            if (newRepo.exists()) {
                Util.deleteFolder(newRepo);
            }
        }
    }
    
    /**
     * Extension of {@link #testRemoteFetching()}; tests the
     * {@link GitRepository#getLastCommitOfBranch(String, String)} method.
     * 
     * @throws GitException unwanted.
     * @throws IOException unwanted.
     */
    @Test
    public void testGetLastCommitOfBranch() throws GitException, IOException {
        File newRepo = new File(TESTDATA, "newRepo");
        assertThat(newRepo.exists(), is(false));
        
        try {
            GitRepository repo = new GitRepository(newRepo);
            assertThat(repo.getWorkingDirectory(), is(newRepo));
            
            assertThat(repo.getRemotes(), is(new HashSet<>()));
            assertThat(repo.containsRemoteBranch("remote1", "master"), is(false));
            
            repo.addRemote("remote1", "file://" + TEST_REPO.getAbsolutePath());
            
            assertThat(repo.getRemotes(), is(new HashSet<>(Arrays.asList("remote1"))));
            assertThat(repo.containsRemoteBranch("remote1", "master"), is(false)); // no fetch yet
            
            repo.fetch("remote1");
            
            assertThat(repo.getRemotes(), is(new HashSet<>(Arrays.asList("remote1"))));
            assertThat(repo.containsRemoteBranch("remote1", "master"), is(true));
            
            // actual test begins here
            
            assertThat(repo.getLastCommitOfBranch("remote1", "master"), is("183dda81207043ba8d81e480c3a8da6a2502b895"));
            
        } finally {
            if (newRepo.exists()) {
                Util.deleteFolder(newRepo);
            }
        }
    }
    
    /**
     * Calls all {@link GitException} constructors for full test coverage.
     */
    @Test
    public void testGitException() {
        GitException e = new GitException();
        assertThat(e.getMessage(), nullValue());
        assertThat(e.getCause(), nullValue());
        
        e = new GitException("abc");
        assertThat(e.getMessage(), is("abc"));
        assertThat(e.getCause(), nullValue());
        
        e = new GitException(new Exception("a"));
        assertThat(e.getMessage(), is("java.lang.Exception: a"));
        assertThat(e.getCause(), notNullValue());
        
        e = new GitException("abc", new Exception("a"));
        assertThat(e.getMessage(), is("abc"));
        assertThat(e.getCause(), notNullValue());
    }
    
}
