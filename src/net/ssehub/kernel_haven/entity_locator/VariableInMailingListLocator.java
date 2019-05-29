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

import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.entity_locator.VariableInMailingListLocator.VariableMailLocation;
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
    
    /**
     * Creates this component.
     * 
     * @param config The pipeline configuration.
     */
    public VariableInMailingListLocator(@NonNull Configuration config) {
        super(config);
    }

    @Override
    protected void execute() {
        // TODO: implement
    }

    @Override
    public @NonNull String getResultName() {
        return "Variables in Mails";
    }

}
