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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.config.ListSetting;
import net.ssehub.kernel_haven.config.Setting;
import net.ssehub.kernel_haven.config.Setting.Type;
import net.ssehub.kernel_haven.entity_locator.VariableInMailingListLocator.VariableMailLocation;
import net.ssehub.kernel_haven.entity_locator.util.GitException;
import net.ssehub.kernel_haven.entity_locator.util.GitRepository;
import net.ssehub.kernel_haven.util.ProgressLogger;
import net.ssehub.kernel_haven.util.Util;
import net.ssehub.kernel_haven.util.io.TableElement;
import net.ssehub.kernel_haven.util.io.TableRow;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * A component that locates variables in external mailings lists (like the Linux Kernel Mailing List).
 *
 * @author Adam
 */
public class VariableInMailingListLocator extends AnalysisComponent<VariableMailLocation> {

    /**
     * Represents a variable that was found in a mail from a mailing list.
     */
    @TableRow
    public static class VariableMailLocation {

        private String variable;
        
        private String mailIdentifier;
        
        private int numOccurrences;

        /**
         * Creates a variable-mail mapping.
         * 
         * @param variable The variable that was found.
         * @param mailIdentifier An identifier for the mail that was found (usually an URL to a web-interface).
         * @param numOccurrences The number of occurrences of this variable in the mail.
         */
        public VariableMailLocation(String variable, String mailIdentifier, int numOccurrences) {
            this.variable = variable;
            this.mailIdentifier = mailIdentifier;
            this.numOccurrences = numOccurrences;
        }
        
        /**
         * Returns the variable that was found.
         * 
         * @return The variable that was found.
         */
        @TableElement(index = 0, name = "Variable")
        public String getVariable() {
            return variable;
        }
        
        /**
         * Returns an identifier for the mail where the variable was fond. This is usually an URL to a web-interface
         * displaying the mail.
         * 
         * @return The identifier for the mail.
         */
        @TableElement(index = 1, name = "Mail")
        public String getMailIdentifier() {
            return mailIdentifier;
        }
        
        /**
         * Returns the number of occurrences of this variable in the mail.
         * 
         * @return The identifier for the mail.
         */
        @TableElement(index = 2, name = "Occurrences")
        public int getNumOccurrences() {
            return numOccurrences;
        }
        
    }
    
    public static final @NonNull ListSetting<@NonNull String> MAIL_SOURCES = new ListSetting<>(
        "analysis.mail_locator.mail_sources", Type.STRING, true, "List of Git repositories that contain "
                + "the mails to be searched. These may be remote URLs or local directories. In the first case, the "
                + "remote will be cloned into a temporary directory. In the second case, the master branch of the "
                + "existing checkout will be used directly.");
    
    public static final @NonNull Setting<@NonNull Pattern> VAR_REGEX = new Setting<>(
        "analysis.mail_locator.variable_regex", Type.REGEX, true, null, "Specifies the regular expression used to find "
                + "relevant variables.");
    
    public static final @NonNull Setting<@NonNull String> URL_PREFIX = new Setting<>(
            "analysis.mail_locator.url_prefix", Type.STRING, true, null, "Specifies an URL prefix for the mails. The "
                    + "message-id of the mail will be appended to this string (with slashes replaced by %2F) to "
                    + "create the identifier of the mail.");
    
    private @NonNull List<@NonNull String> mailSources;
    
    private @NonNull Pattern varRegex;
    
    private @NonNull String urlPrefix;
    
    /**
     * Creates this component.
     * 
     * @param config The pipeline configuration.
     * 
     * @throws SetUpException If setting up this component fails.
     */
    public VariableInMailingListLocator(@NonNull Configuration config) throws SetUpException {
        super(config);
        
        config.registerSetting(MAIL_SOURCES);
        this.mailSources = config.getValue(MAIL_SOURCES);
        if (mailSources.isEmpty()) {
            throw new SetUpException("No mail source given in " + MAIL_SOURCES.getKey());
        }
        
        config.registerSetting(VAR_REGEX);
        this.varRegex = config.getValue(VAR_REGEX);
        
        config.registerSetting(URL_PREFIX);
        this.urlPrefix = config.getValue(URL_PREFIX);
    }

    /**
     * Searches the given mail for any relevant variables.
     * 
     * @param in The mail to read.
     * 
     * @throws IOException If reading the mail fails.
     */
    private void searchInMail(@NonNull BufferedReader in) throws IOException {
        String messageId = null;
        String line;
        while ((line = in.readLine()) != null) {
            String header = line.toLowerCase();
            if (header.startsWith("message-id:")) {
                messageId = line.substring("message-id:".length()).trim();
                if (!messageId.isEmpty()
                        && messageId.charAt(0) == '<' && messageId.charAt(messageId.length() - 1) == '>') {
                    messageId = messageId.substring(1, messageId.length() - 1);
                }
            }
            
            // only parse the header of the mail
            if (line.isEmpty()) {
                break;
            }
        }
        
        if (messageId == null) {
            // couldn't find any message id...
            return;
        }
        
        // read the rest of the mail and search for variables
        Map<String, Integer> foundVars = new HashMap<>();
        while ((line = in.readLine()) != null) {
            Matcher m = varRegex.matcher(line);
            while (m.find()) {
                foundVars.put(m.group(), foundVars.getOrDefault(m.group(), 0) + 1);
            }
        }
        
        if (!foundVars.isEmpty()) {
            String mailId = urlPrefix + URLEncoder.encode(messageId, "UTF-8");
            for (Map.Entry<String, Integer> entry : foundVars.entrySet()) {
                addResult(new VariableMailLocation(entry.getKey(), mailId, entry.getValue()));
            }
        }
    }
    
    /**
     * Executes this analysis on the given git repository.
     * 
     * @param gitRepo The git repository containing the already checked-out mail archive.
     */
    private void execute(@NonNull GitRepository gitRepo) {
        List<@NonNull String> commits;
        try {
            gitRepo.checkout("master");
            commits = gitRepo.listAllCommits();
        } catch (GitException e) {
            LOGGER.logException("Couldn't initialize git repository", e);
            return;
        }
        
        File m = new File("m");
        
        ProgressLogger progress = new ProgressLogger("VariableInMailingListLocator (parsing mails)", commits.size());
        for (String commit : commits) {
            try {
                // check out only the relevant file; this is slightly faster than a full checkout
                gitRepo.checkout(commit, m);
            } catch (GitException e) {
                LOGGER.logException("Couldn't check out commit: " + commit, e);
            }
            
            try (BufferedReader in = new BufferedReader(new FileReader(new File(gitRepo.getWorkingDirectory(), "m")))) {
                
                searchInMail(in);
                
            } catch (IOException e) {
                LOGGER.logException("Couldn't read mail", e);
            }
            
            progress.processedOne();
        }
        
        progress.close();

        // reset HEAD to master to leave the repository "clean"
        try {
            gitRepo.checkout("master");
        } catch (GitException e) {
            LOGGER.logExceptionWarning("Couldn't reset HEAD to master (after finishing crawling)", e);
        }
    }
    
    @Override
    protected void execute() {
        ProgressLogger progress = new ProgressLogger("VariableInMailingListLocator (crawling mail sources)",
                this.mailSources.size());
        
        for (String mailSource : this.mailSources) {
            File dir = new File(mailSource);
            if (dir.isDirectory()) {
                // mailSource is a locally checked-out git repository
                try {
                    execute(new GitRepository(dir));
                } catch (GitException e) {
                    LOGGER.logException(mailSource + " is not a valid git repository", e);
                }
            } else {
                // mailSource is a remote URL
                File dest = null;
                try {
                    dest = File.createTempFile("cloned_mail_source", ".git");
                    dest.delete();
                    
                    execute(GitRepository.clone(mailSource, dest));
                    
                } catch (IOException | GitException e) {
                    LOGGER.logException("Could not clone " + mailSource, e);
                } finally {
                    if (dest != null) {
                        try {
                            Util.deleteFolder(dest);
                        } catch (IOException e) {
                            LOGGER.logException("Couldn't clear temporary checkout", e);
                        }
                    }
                }
            }
            
            progress.processedOne();
        }
        
        progress.close();
    }

    @Override
    public @NonNull String getResultName() {
        return "Variables in Mails";
    }

}
