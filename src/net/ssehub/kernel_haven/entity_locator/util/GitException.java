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

/**
 * An exception thrown by {@link GitRepository}.
 * 
 * @author Adam
 */
public class GitException extends Exception {

    private static final long serialVersionUID = -2230086275703944780L;
    
    /**
     * Creates an empty {@link GitException}.
     */
    public GitException() {
    }
    
    /**
     * Creates a {@link GitException} with the given message.
     * 
     * @param message The exception message.
     */
    public GitException(String message) {
        super(message);
    }

    /**
     * Creates a {@link GitException} with the given cause.
     * 
     * @param cause The exception cause.
     */
    public GitException(Throwable cause) {
        super(cause);
    }
    
    /**
     * Creates a {@link GitException} with the given message and cause.
     * 
     * @param message The exception message.
     * @param cause The exception cause.
     */
    public GitException(String message, Throwable cause) {
        super(message, cause);
    }

}
