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
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

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
    
}
