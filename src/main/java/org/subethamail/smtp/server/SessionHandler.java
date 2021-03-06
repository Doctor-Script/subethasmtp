package org.subethamail.smtp.server;

import org.subethamail.smtp.internal.server.AcceptAllSessionHandler;
import org.subethamail.smtp.internal.server.ConcurrentSessionsBySourceLimiter;

import com.github.davidmoten.guavamini.Preconditions;

/**
 * Handler of session lifecycle events.
 *
 * @author Diego Salvi
 */
public interface SessionHandler {

    /**
     * This method is invoked on a session creation, before sending the SMTP greeting and can react rejecting
     * the session.
     * <p>
     * Rejected session will be closed and no method {@link #onSessionEnd(Session)} will be invoked.
     * </p>
     *
     * @param session newly created session (should never be null)
     * @return starting session result event, can allow or reject the newly created session
     */
    SessionAcceptance accept(Session session) ;

    /**
     * This method is invoked on session close.
     *
     * @param session closing session
     */
    void onSessionEnd(Session session);
    
    
    public static SessionHandler limitConcurrentSessionsBySource(int maxConnectionsPerSource) {
        return new ConcurrentSessionsBySourceLimiter(maxConnectionsPerSource);
    }
    
    public static SessionHandler acceptAll() {
        return AcceptAllSessionHandler.INSTANCE;
    }

    /**
     * Result object for {@link SessionHandler#accept(Session)}
     *
     * @author Diego Salvi
     */
    public static final class SessionAcceptance {

        // Singleton success result
        private static final SessionAcceptance SUCCESS = new SessionAcceptance(true, 0, null);

        /**
         * Returns a success {@link SessionHandler#accept(Session)} result.
         *
         * @return session start success
         */
        public static SessionAcceptance success() {
            return SUCCESS;
        }

        /**
         * Returns a failed {@link SessionHandler#accept(Session)} result.
         *
         * @param code SMTP failure result code
         * @param message SMTP failure result message
         * @return session start failure
         */
        public static SessionAcceptance failure(int code, String message) {
            /* Check that code is a failure response! */
            return new SessionAcceptance(false, code, message);
        }

        private final boolean accepted;
        
        // only used if not accepted
        private final int errorCode;
        private final String errorMessage;

        SessionAcceptance(boolean accepted, int errorCode, String errorMessage) {
            Preconditions.checkArgument(accepted || (errorCode > 199 && errorCode < 600),
                    "Invalid SMTP response code " + errorCode);
            Preconditions.checkArgument(!accepted || (errorCode == 0 && errorMessage == null));
            this.accepted = accepted;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }
        
        public boolean accepted() {
            return accepted;
        }

        /**
         * If session was accepted then returns 0 else returns the SMTP response code
         * representing the reason for the session not being accepted.
         * 
         * @return error code for a session that was not accepted
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * If session was accepted then returns null else returns the message
         * representing the reason for the session not being accepted.
         * 
         * @return error message for a session that was not accepted
         */
        public String errorMessage() {
            return errorMessage;
        }
    }

}
