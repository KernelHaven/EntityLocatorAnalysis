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
