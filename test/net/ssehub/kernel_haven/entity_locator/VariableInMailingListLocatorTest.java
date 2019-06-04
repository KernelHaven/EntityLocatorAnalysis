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
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.entity_locator.VariableInMailingListLocator.VariableMailLocation;
import net.ssehub.kernel_haven.test_utils.AnalysisComponentExecuter;
import net.ssehub.kernel_haven.test_utils.TestConfiguration;
import net.ssehub.kernel_haven.util.Util;
import net.ssehub.kernel_haven.util.ZipArchive;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * Tests the {@link VariableInMailingListLocator}.
 *
 * @author Adam
 */
@SuppressWarnings("null")
public class VariableInMailingListLocatorTest {
    
    private static final File TESTDATA = new File("testdata");
    
    private static final File MOCKED_REPO = new File(TESTDATA, "testRepo");
    
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
        Util.deleteFolder(MOCKED_REPO);
    }
    
    /**
     * Tests with a locally checked out, small test repository. The repository contains 4 mails.
     * 
     * @throws SetUpException unwanted.
     * @throws IOException unwanted.
     */
    @Test
    public void testLocalMockedRepo() throws SetUpException, IOException {
        TestConfiguration config = new TestConfiguration(new Properties());
        
        config.registerSetting(VariableInMailingListLocator.MAIL_SOURCES);
        config.setValue(VariableInMailingListLocator.MAIL_SOURCES, Arrays.asList(MOCKED_REPO.getAbsolutePath()));
        
        config.registerSetting(VariableInMailingListLocator.VAR_REGEX);
        config.setValue(VariableInMailingListLocator.VAR_REGEX, Pattern.compile("CONFIG_\\w+"));
        
        config.registerSetting(VariableInMailingListLocator.URL_PREFIX);
        config.setValue(VariableInMailingListLocator.URL_PREFIX, "https://lore.kernel.org/lkml/");
        
        List<@NonNull VariableMailLocation> result = AnalysisComponentExecuter.executeComponent(
                VariableInMailingListLocator.class, config);
        
        assertThat(result.size(), is(4));
        
        assertThat(result.get(0).getVariable(), is("CONFIG_ABC"));
        assertThat(result.get(0).getMailIdentifier(), is("https://lore.kernel.org/lkml/1215-4-7-1-5-4-7@test.org"));
        assertThat(result.get(0).getNumOccurrences(), is(1));
        
        assertThat(result.get(1).getVariable(), is("CONFIG_DEF"));
        assertThat(result.get(1).getMailIdentifier(), is("https://lore.kernel.org/lkml/121554-8-6-4-4-777@test.org"));
        assertThat(result.get(1).getNumOccurrences(), is(1));
        
        assertThat(result.get(2).getVariable(), is("CONFIG_ABC"));
        assertThat(result.get(2).getMailIdentifier(), is("https://lore.kernel.org/lkml/121554-8-6-4-4-777@test.org"));
        assertThat(result.get(2).getNumOccurrences(), is(2));
        
        assertThat(result.get(3).getVariable(), is("CONFIG_ABC"));
        assertThat(result.get(3).getMailIdentifier(), is("https://lore.kernel.org/lkml/123%2F456@test.org"));
        assertThat(result.get(3).getNumOccurrences(), is(1));
    }
    
    /**
     * Tests with a small test repository that needs to be cloned first. The repository contains 4 mails.
     * 
     * @throws SetUpException unwanted.
     * @throws IOException unwanted.
     */
    @Test
    public void testRemoteMockedRepo() throws SetUpException, IOException {
        TestConfiguration config = new TestConfiguration(new Properties());
        
        config.registerSetting(VariableInMailingListLocator.MAIL_SOURCES);
        config.setValue(VariableInMailingListLocator.MAIL_SOURCES,
                // use file:// prefix, so that the file is interpreted as an URL 
                Arrays.asList("file://" + MOCKED_REPO.getAbsolutePath()));
        
        config.registerSetting(VariableInMailingListLocator.VAR_REGEX);
        config.setValue(VariableInMailingListLocator.VAR_REGEX, Pattern.compile("CONFIG_\\w+"));
        
        config.registerSetting(VariableInMailingListLocator.URL_PREFIX);
        config.setValue(VariableInMailingListLocator.URL_PREFIX, "https://lore.kernel.org/lkml/");
        
        List<@NonNull VariableMailLocation> result = AnalysisComponentExecuter.executeComponent(
                VariableInMailingListLocator.class, config);
        
        assertThat(result.size(), is(4));
        
        assertThat(result.get(0).getVariable(), is("CONFIG_ABC"));
        assertThat(result.get(0).getMailIdentifier(), is("https://lore.kernel.org/lkml/1215-4-7-1-5-4-7@test.org"));
        assertThat(result.get(0).getNumOccurrences(), is(1));
        
        assertThat(result.get(1).getVariable(), is("CONFIG_DEF"));
        assertThat(result.get(1).getMailIdentifier(), is("https://lore.kernel.org/lkml/121554-8-6-4-4-777@test.org"));
        assertThat(result.get(1).getNumOccurrences(), is(1));
        
        assertThat(result.get(2).getVariable(), is("CONFIG_ABC"));
        assertThat(result.get(2).getMailIdentifier(), is("https://lore.kernel.org/lkml/121554-8-6-4-4-777@test.org"));
        assertThat(result.get(2).getNumOccurrences(), is(2));
        
        assertThat(result.get(3).getVariable(), is("CONFIG_ABC"));
        assertThat(result.get(3).getMailIdentifier(), is("https://lore.kernel.org/lkml/123%2F456@test.org"));
        assertThat(result.get(3).getNumOccurrences(), is(1));
    }
    
    /**
     * Tests that an exception is thrown if no mail sources are configured.
     * 
     * @throws SetUpException wanted.
     */
    @Test(expected = SetUpException.class)
    public void testNoMailSources() throws SetUpException {
        TestConfiguration config = new TestConfiguration(new Properties());

        config.registerSetting(VariableInMailingListLocator.MAIL_SOURCES);
        config.setValue(VariableInMailingListLocator.MAIL_SOURCES, Arrays.asList());
        
        config.registerSetting(VariableInMailingListLocator.VAR_REGEX);
        config.setValue(VariableInMailingListLocator.VAR_REGEX, Pattern.compile("CONFIG_\\w+"));
        
        config.registerSetting(VariableInMailingListLocator.URL_PREFIX);
        config.setValue(VariableInMailingListLocator.URL_PREFIX, "https://lore.kernel.org/lkml/");
        
        new VariableInMailingListLocator(config);
    }
    
}
