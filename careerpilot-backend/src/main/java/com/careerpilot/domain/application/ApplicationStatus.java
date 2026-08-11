package com.careerpilot.domain.application;

import java.util.EnumSet;
import java.util.Set;

/**
 * F5: Application status enum with allowed transition rules.
 *
 * Transition graph:
 *   SAVED      → APPLIED, REJECTED
 *   APPLIED    → SCREENING, REJECTED
 *   SCREENING  → INTERVIEW, REJECTED
 *   INTERVIEW  → OFFER, REJECTED
 *   OFFER      → REJECTED (withdrawal)
 *   REJECTED   → (terminal — no further transitions)
 *
 * REJECTED is reachable from any non-terminal state.
 * No backward movement is permitted.
 */
public enum ApplicationStatus {

    SAVED {
        @Override
        public Set<ApplicationStatus> allowedTransitions() {
            return EnumSet.of(APPLIED, REJECTED);
        }
    },
    APPLIED {
        @Override
        public Set<ApplicationStatus> allowedTransitions() {
            return EnumSet.of(SCREENING, REJECTED);
        }
    },
    SCREENING {
        @Override
        public Set<ApplicationStatus> allowedTransitions() {
            return EnumSet.of(INTERVIEW, REJECTED);
        }
    },
    INTERVIEW {
        @Override
        public Set<ApplicationStatus> allowedTransitions() {
            return EnumSet.of(OFFER, REJECTED);
        }
    },
    OFFER {
        @Override
        public Set<ApplicationStatus> allowedTransitions() {
            return EnumSet.of(REJECTED);
        }
    },
    REJECTED {
        @Override
        public Set<ApplicationStatus> allowedTransitions() {
            return EnumSet.noneOf(ApplicationStatus.class);
        }
    };

    /**
     * Returns the set of statuses this status can legally transition to.
     * An empty set means the status is terminal.
     */
    public abstract Set<ApplicationStatus> allowedTransitions();

    /**
     * Returns true if transitioning from this status to {@code next} is permitted.
     */
    public boolean canTransitionTo(ApplicationStatus next) {
        return allowedTransitions().contains(next);
    }
}
