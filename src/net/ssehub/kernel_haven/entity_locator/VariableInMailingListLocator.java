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

import static net.ssehub.kernel_haven.util.null_checks.NullHelpers.notNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.entity_locator.VariableInMailingListLocator.VariableMailLocation;
import net.ssehub.kernel_haven.entity_locator.util.GitException;
import net.ssehub.kernel_haven.entity_locator.util.GitRepository;
import net.ssehub.kernel_haven.util.ProgressLogger;
import net.ssehub.kernel_haven.util.null_checks.NonNull;;

/**
 * A component that locates variables in external mailings lists (like the Linux Kernel Mailing List).
 *
 * @author Adam
 */
public class VariableInMailingListLocator extends AnalysisComponent<VariableMailLocation> {

    /**
     * Represents a variable that was found in a mail from a mailing list.
     */
    public static class VariableMailLocation {

        private String variable;
        
        private String mailIdentifier;

        /**
         * Creates a variable-mail mapping.
         * 
         * @param variable The variable that was found.
         * @param mailIdentifier An identifier for the mail that was found (usually an URL to a web-interface).
         */
        public VariableMailLocation(String variable, String mailIdentifier) {
            this.variable = variable;
            this.mailIdentifier = mailIdentifier;
        }
        
        /**
         * Returns the variable that was found.
         * 
         * @return The variable that was found.
         */
        public String getVariable() {
            return variable;
        }
        
        /**
         * Returns an identifier for the mail where the variable was fond. This is usually an URL to a web-interface
         * displaying the mail.
         * 
         * @return The identifier for the mail.
         */
        public String getMailIdentifier() {
            return mailIdentifier;
        }
        
    }
    
    private static final @NonNull Pattern VAR_REGEX = notNull(Pattern.compile("CONFIG_\\w+")); // TODO
    
    private static final @NonNull String LORE_URL = "https://lore.kernel.org/lkml/"; // TODO
    
    private static final @NonNull File GIT_CHECKOUT = new File("E:\\tmp\\lkml\\test"); // TODO
    
    private @NonNull GitRepository gitRepo;
    
    /**
     * Creates this component.
     * 
     * @param config The pipeline configuration.
     * 
     * @throws SetUpException If setting up this component fails.
     */
    public VariableInMailingListLocator(@NonNull Configuration config) throws SetUpException {
        super(config);
        
        try {
            this.gitRepo = new GitRepository(GIT_CHECKOUT);
        } catch (GitException e) {
            throw new SetUpException("Couldn't open git repository", e);
        }
    }

    @Override
    protected void execute() {
        List<String> commits;
        try {
            gitRepo.checkout("master");
            commits = gitRepo.listAllCommits();
        } catch (GitException e) {
            LOGGER.logException("Couldn't initialize git repository", e);
            return;
        }
        
        ProgressLogger progress = new ProgressLogger("Parsing Mails", commits.size());
        for (String commit : commits) {
            try {
                gitRepo.checkout(commit);
            } catch (GitException e) {
                LOGGER.logException("Couldn't check out commit: " + commit, e);
            }

            try (BufferedReader in = new BufferedReader(new FileReader(new File(gitRepo.getWorkingDirectory(), "m")))) {
                
                String messageId = null;
                String line;
                while ((line = in.readLine()) != null) {
                    String header = line.toLowerCase();
                    if (header.startsWith("message-id:")) {
                        messageId = line.substring("message-id:".length()).trim();
                        if (messageId.charAt(0) == '<' && messageId.charAt(messageId.length() - 1) == '>') {
                            messageId = messageId.substring(1, messageId.length() - 1);
                        }
                        break;
                    }
                    
                    // only parse the header of the mail
                    if (line.isEmpty()) {
                        break;
                    }
                }
                
                if (messageId == null) {
                    System.err.println("Couldn't find message ID...");
                    progress.processedOne();
                    continue;
                }
                
                // read the rest of the mail and search for variables
                Set<String> foundVars = new HashSet<>();
                while ((line = in.readLine()) != null) {
                    Matcher m = VAR_REGEX.matcher(line);
                    if (m.find()) {
                        foundVars.add(m.group());
                    }
                }
                
                if (!foundVars.isEmpty()) {
                    String mailId = LORE_URL + messageId + "/";
                    for (String var : foundVars) {
                        addResult(new VariableMailLocation(var, mailId));
                    }
                }
                
            } catch (IOException e) {
                e.printStackTrace();
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
